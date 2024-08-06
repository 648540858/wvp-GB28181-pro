package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.GbCode;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.GroupTree;
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

import java.util.ArrayList;
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
        Assert.notNull(group.getName(), "设备编号不可为NULL");

        GbCode gbCode = GbCode.decode(group.getDeviceId());
        Assert.notNull(gbCode, "设备编号不满足国标定义");
        if ("215".equals(gbCode.getTypeCode())){
            // 添加业务分组
            addBusinessGroup(group);
        }else {
            Assert.isTrue("216".equals(gbCode.getTypeCode()), "创建虚拟组织时设备编号11-13位应使用216");
            // 添加虚拟组织
            addGroup(group);
        }
    }

    private void addGroup(Group group) {
        // 建立虚拟组织
        Assert.notNull(group.getBusinessGroup(), "所属的业务分组分组不存在");
        Group businessGroup = groupManager.queryBusinessGroup(group.getBusinessGroup());
        Assert.notNull(businessGroup, "所属的业务分组分组不存在");
        if (!ObjectUtils.isEmpty(group.getParentDeviceId())) {
            Group parentGroup = groupManager.queryOneByDeviceId(group.getParentDeviceId(), group.getBusinessGroup());
            Assert.notNull(parentGroup, "所属的上级分组分组不存在");
        }else {
            group.setParentDeviceId(null);
        }
        group.setCreateTime(DateUtil.getNow());
        group.setUpdateTime(DateUtil.getNow());
        groupManager.add(group);
        if (group.getPlatformId() != null) {
            CommonGBChannel channel = CommonGBChannel.build(group);
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(group.getPlatformId(), channel, CatalogEvent.ADD);
            }catch (Exception e) {
                log.warn("[添加虚拟组织] 发送失败， {}-{}", channel.getGbName(), channel.getGbDeviceDbId(), e);
            }
        }
    }

    private void addBusinessGroup(Group group) {
        group.setBusinessGroup(group.getDeviceId());
        group.setCreateTime(DateUtil.getNow());
        group.setUpdateTime(DateUtil.getNow());
        groupManager.addBusinessGroup(group);
        if (group.getPlatformId() != null) {
            CommonGBChannel channel = CommonGBChannel.build(group);
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(group.getPlatformId(), channel, CatalogEvent.ADD);
            }catch (Exception e) {
                log.warn("[添加虚拟组织] 发送失败， {}-{}", channel.getGbName(), channel.getGbDeviceDbId(), e);
            }
        }
    }

    private List<Group> queryAllChildren(String deviceId, Integer platformId) {
        List<Group> children = groupManager.getChildren(deviceId, platformId);
        if (ObjectUtils.isEmpty(children)) {
            return children;
        }
        for (int i = 0; i < children.size(); i++) {
            children.addAll(queryAllChildren(children.get(i).getDeviceId(), platformId));
        }
        return children;
    }

    @Override
    @Transactional
    public void update(Group group) {
        Assert.isTrue(group.getId()> 0, "更新必须携带分组ID");
        Assert.notNull(group.getDeviceId(), "编号不可为NULL");
        Assert.notNull(group.getBusinessGroup(), "业务分组不可为NULL");
        Group groupInDb = groupManager.queryOne(group.getId());
        Assert.notNull(groupInDb, "分组不存在");

        group.setName(group.getName());
        group.setUpdateTime(DateUtil.getNow());
        groupManager.update(group);
        // 修改他的子节点
        if (!group.getDeviceId().equals(groupInDb.getDeviceId())
                || !group.getBusinessGroup().equals(groupInDb.getBusinessGroup())) {
            List<Group> groupList = queryAllChildren(groupInDb.getDeviceId(), groupInDb.getPlatformId());
            if (!groupList.isEmpty()) {
               int result =  groupManager.updateChild(groupInDb.getDeviceId(), group);
               if (result > 0) {
                   for (Group chjildGroup : groupList) {
                       chjildGroup.setParentDeviceId(group.getDeviceId());
                       chjildGroup.setBusinessGroup(group.getBusinessGroup());
                       // 将变化信息发送通知
                       CommonGBChannel channel = CommonGBChannel.build(chjildGroup);
                       try {
                           // 发送catalog
                           eventPublisher.catalogEventPublish(null, channel, CatalogEvent.UPDATE);
                       }catch (Exception e) {
                           log.warn("[业务分组/虚拟组织变化] 发送失败，{}", group.getDeviceId(), e);
                       }
                   }
               }
            }
        }
        // 将变化信息发送通知
        CommonGBChannel channel = CommonGBChannel.build(group);
        try {
            // 发送catalog
            eventPublisher.catalogEventPublish(null, channel, CatalogEvent.UPDATE);
        }catch (Exception e) {
            log.warn("[业务分组/虚拟组织变化] 发送失败，{}", group.getDeviceId(), e);
        }

        // 由于编号变化，会需要处理太多内容以及可能发送大量消息，所以目前更新只只支持重命名
        GbCode decode = GbCode.decode(group.getDeviceId());
        if (!groupInDb.getDeviceId().equals(group.getDeviceId())) {
            if (decode.getTypeCode().equals("215")) {
                // 业务分组变化。需要将其下的所有业务分组修改
                gbChannelService.updateBusinessGroup(groupInDb.getDeviceId(), group.getDeviceId());
            }else {
                // 虚拟组织修改，需要把其下的子节点修改父节点ID
                gbChannelService.updateParentIdGroup(groupInDb.getDeviceId(), group.getDeviceId());
            }
        }
    }

    @Override
    public Group queryGroupByDeviceId(String regionDeviceId) {
        return null;
    }

    @Override
    public List<GroupTree> queryForTree(String query, String parent, Integer platformId) {
        if (parent == null) {
            // 查询所有业务分组
            return groupManager.queryBusinessGroupForTree(query, platformId);
        }else {
            GbCode gbCode = GbCode.decode(parent);
            if (gbCode.getTypeCode().equals("215")) {
                return groupManager.queryForTreeByBusinessGroup(query, parent, platformId);
            }else {
                return groupManager.queryForTree(query, parent, platformId);
            }

        }
    }

    @Override
    public void syncFromChannel() {

    }

    @Override
    @Transactional
    public boolean delete(int id) {
        Group group = groupManager.queryOne(id);
        Assert.notNull(group, "分组不存在");
        List<Group> groupListForDelete = new ArrayList<>();
        GbCode gbCode = GbCode.decode(group.getDeviceId());
        if (gbCode.getTypeCode().equals("215")) {
            List<Group> groupList = groupManager.queryByBusinessGroup(group.getDeviceId());
            if (!groupList.isEmpty()) {
                groupListForDelete.addAll(groupList);
            }
            // 业务分组
            gbChannelService.removeParentIdByBusinessGroup(group.getDeviceId());
        }else {
            List<Group> groupList = queryAllChildren(group.getDeviceId(), group.getPlatformId());
            if (!groupList.isEmpty()) {
                groupListForDelete.addAll(groupList);
            }
            groupListForDelete.add(group);
            gbChannelService.removeParentIdByGroupList(groupListForDelete);
        }
        groupManager.batchDelete(groupListForDelete);
        for (Group groupForDelete : groupListForDelete) {
            // 将变化信息发送通知
            CommonGBChannel channel = CommonGBChannel.build(groupForDelete);
            try {
                // 发送catalog
                eventPublisher.catalogEventPublish(null, channel, CatalogEvent.DEL);
            }catch (Exception e) {
                log.warn("[业务分组/虚拟组织删除] 发送失败，{}", groupForDelete.getDeviceId(), e);
            }
        }
        return true;
    }
}
