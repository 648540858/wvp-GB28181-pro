package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;

/**
 * 区域管理类
 */
@Service
@Slf4j
public class GroupServiceImpl implements IGroupService {

    @Autowired
    private GroupMapper groupManager;

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;

    @Autowired
    private IGbChannelService gbChannelService;

    @Autowired
    private EventPublisher eventPublisher;

    @Override
    public void add(Group group) {
        Assert.notNull(group, "参数不可为NULL");
        Assert.notNull(group.getDeviceId(), "设备编号不可为NULL");
        Assert.isTrue(group.getDeviceId().trim().length() == 20, "设备编号必须为20位");
        Assert.isTrue(group.getParentDeviceId().trim().length() == 20, "父级编号错误");
        Assert.notNull(group.getName(), "设备编号不可为NULL");

        GbCode gbCode = GbCode.decode(group.getDeviceId());
        Assert.notNull(gbCode, "设备编号不满足国标定义");
        // 根据字段判断此处应使用什么规则校验
        if (ObjectUtils.isEmpty(group.getParentDeviceId())) {
            if (ObjectUtils.isEmpty(group.getBusinessGroup())) {
                // 如果是建立业务分组，那么编号必须20位，且10-13必须为215,
                Assert.isTrue("215".equals(gbCode.getTypeCode()), "创建业务分组时设备编号11-13位应使用215");
                group.setBusinessGroup(group.getDeviceId());
            }else {
                // 建立第一个虚拟组织
                Assert.isTrue("216".equals(gbCode.getTypeCode()), "创建虚拟组织时设备编号11-13位应使用216");
            }
        }else {
            // 建立第一个虚拟组织
            Assert.isTrue("216".equals(gbCode.getTypeCode()), "创建虚拟组织时设备编号11-13位应使用216");
        }
        if (!ObjectUtils.isEmpty(group.getBusinessGroup())) {
            // 校验业务分组是否存在
            Group businessGroup = groupManager.queryBusinessGroup(group.getBusinessGroup());
            Assert.notNull(businessGroup, "所属的业务分组分组不存在");
        }
        if (!ObjectUtils.isEmpty(group.getParentDeviceId())) {
            Group parentGroup = groupManager.queryOneByDeviceId(group.getParentDeviceId(), group.getBusinessGroup());
            Assert.notNull(parentGroup, "所属的上级分组分组不存在");
        }
        group.setCreateTime(DateUtil.getNow());
        group.setUpdateTime(DateUtil.getNow());
        groupManager.add(group);
        // 添加新的虚拟组织需要发起同志
        if (gbCode.getTypeCode().equals("216")) {
            CommonGBChannel channel = CommonGBChannel.build(group);
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(null, channel, CatalogEvent.ADD);
            }catch (Exception e) {
                log.warn("[添加虚拟组织] 发送失败， {}-{}", channel.getGbName(), channel.getGbDeviceDbId(), e);
            }
        }
    }

    @Override
    @Transactional
    public boolean deleteByDeviceId(String deviceId, String groupId) {
        Assert.notNull(deviceId, "编号不可为NULL");
        Assert.notNull(groupId, "业务分组不可为NULL");
        GbCode gbCode = GbCode.decode(deviceId);

        Group businessGroup = groupManager.queryBusinessGroup(groupId);
        Assert.notNull(businessGroup, "业务分组不存在");
        // 是否需要清理业务分组字段
        if (gbCode.getTypeCode().equals("215")) {
            // 删除业务分组
            // 获取所有的虚拟组织
            int result = groupManager.deleteByBusinessGroup(deviceId);
            Assert.isTrue(result > 0, "分组不存在");
            gbChannelService.removeParentIdByBusinessGroup(deviceId);
        }else {
            // 删除虚拟组织
            Group group = groupManager.queryOneByDeviceId(deviceId, groupId);
            Assert.notNull(group, "分组不存在");
            // 获取所有子分组
            List<Group> groupList = queryAllChildren(deviceId, groupId);
            groupList.add(group);
            int result = groupManager.batchDelete(groupList);
            Assert.isTrue(result> 0, "删除分组失败");
            gbChannelService.removeParentIdByGroupList(groupList);
        }
        return true;
    }

    private List<Group> queryAllChildren(String deviceId, String groupId) {
        List<Group> children = groupManager.getChildren(deviceId, groupId);
        if (ObjectUtils.isEmpty(children)) {
            return children;
        }
        for (int i = 0; i < children.size(); i++) {
            children.addAll(queryAllChildren(children.get(i).getDeviceId(), groupId));
        }
        return children;
    }

    @Override
    public void update(Group group) {
        Assert.isTrue(group.getId()> 0, "更新必须携带分组ID");
        Assert.notNull(group.getDeviceId(), "编号不可为NULL");
        Assert.notNull(group.getBusinessGroup(), "业务分组不可为NULL");
        Group groupInDb = groupManager.queryOne(group.getId());
        Assert.notNull(groupInDb, "分组不存在");
        // 由于编号变化，会需要处理太多内容以及可能发送大量消息，所以目前更新只只支持重命名
        groupInDb.setName(group.getName());
        groupInDb.setUpdateTime(DateUtil.getNow());
        groupManager.update(groupInDb);


        // 名称变化-- 直接通知上级分组本身变化
        // 编号变化-- 通知：1.分组删除， 2.分组新增， 3.所有的所属通道parentId变化
        // 本身是业务分组，如果编号变化，相当于重建，需要做大量通知
        //


        GbCode decode = GbCode.decode(group.getDeviceId());
        if (decode.getTypeCode().equals("215")) {
            // 业务分组变化。需要将其下的所有业务分组修改
        }else {
            // 虚拟组织修改，需要把其下的子节点修改父节点ID
        }

        int update = groupManager.update(group);
        if (update == 1) {
            // TODO 查看此业务分组是否关联了国标设备，发送更新消息

        }

    }

    @Override
    public List<Group> getAllChild(String parentDeviceId) {
        return Collections.emptyList();
    }

    @Override
    public Group queryGroupByDeviceId(String regionDeviceId) {
        return null;
    }

    @Override
    public List<GroupTree> queryForTree(String query, String parent) {
        return Collections.emptyList();
    }

    @Override
    public void syncFromChannel() {

    }

    @Override
    public boolean delete(int id) {
        return false;
    }
}
