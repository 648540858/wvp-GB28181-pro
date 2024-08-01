package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    @Override
    public void add(Group group) {
        Assert.notNull(group, "参数不可为NULL");
        Assert.notNull(group.getDeviceId(), "设备编号不可为NULL");
        Assert.isTrue(group.getDeviceId().trim().length() == 20, "设备编号必须为20位");
        Assert.isTrue(group.getParentDeviceId().trim().length() == 20, "父级编号错误");
        Assert.notNull(group.getName(), "设备编号不可为NULL");

        GbCode decode = GbCode.decode(group.getDeviceId());
        Assert.notNull(decode, "设备编号不满足国标定义");
        // 根据字段判断此处应使用什么规则校验
        if (ObjectUtils.isEmpty(group.getParentDeviceId())) {
            if (ObjectUtils.isEmpty(group.getBusinessGroup())) {
                // 如果是建立业务分组，那么编号必须20位，且10-13必须为215,
                Assert.isTrue("215".equals(decode.getTypeCode()), "创建业务分组时设备编号11-13位应使用215");
                group.setBusinessGroup(group.getDeviceId());
            }else {
                // 建立第一个虚拟组织
                Assert.isTrue("216".equals(decode.getTypeCode()), "创建虚拟组织时设备编号11-13位应使用216");
            }
        }else {
            // 建立第一个虚拟组织
            Assert.isTrue("216".equals(decode.getTypeCode()), "创建虚拟组织时设备编号11-13位应使用216");
        }
        if (!ObjectUtils.isEmpty(group.getBusinessGroup())) {
            // 校验业务分组是否存在
            Group businessGroup = groupManager.queryBusinessGroup(group.getBusinessGroup());
            Assert.notNull(businessGroup, "所属的业务分组分组不存在");
        }
        if (!ObjectUtils.isEmpty(group.getParentDeviceId())) {
            Group groupInDb = groupManager.queryOneByDeviceId(group.getParentDeviceId(), group.getBusinessGroup());
            Assert.notNull(groupInDb, "所属的上级分组分组不存在");
        }
        group.setCreateTime(DateUtil.getNow());
        group.setUpdateTime(DateUtil.getNow());
        groupManager.add(group);
    }

    @Override
    public boolean deleteByDeviceId(String deviceId, String groupId) {
        Assert.notNull(deviceId, "设备编号不可为NULL");
        Assert.notNull(groupId, "业务分组不可为NULL");
        GbCode gbCode = GbCode.decode(deviceId);

        Group businessGroup = groupManager.queryBusinessGroup(groupId);
        Assert.notNull(businessGroup, "业务分组不存在");
        // 待删除的分组
        List<Group> groupList;
        // 是否需要清理业务分组字段
        if (gbCode.getTypeCode().equals("215")) {
            // 删除业务分组
            // 获取所有的虚拟组织
            groupList = groupManager.queryByBusinessGroup(deviceId);
            if (groupList.isEmpty()) {
                return false;
            }
        }else {
            // 删除虚拟组织
            Group group = groupManager.queryOneByDeviceId(deviceId, groupId);
            Assert.notNull(group, "分组不存在");
            // 获取所有子分组
            groupList = queryAllChildren(deviceId, groupId);
            if (groupList.isEmpty()) {
                return false;
            }

        }
        List<CommonGBChannelWitchGroupChannelId> channels = commonGBChannelMapper.queryByGroupList(groupList);
        if (channels.isEmpty()) {
            return false;
        }
        commonGBChannelMapper.batchDeleteGroup(channels);

        // TODO 待定 是否需要发送catalog事件，还是等分配的时候发送UPDATE事件
        groupManager.batchDelete(groupList);
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

    }

    @Override
    public List<Group> getAllChild(String parent) {
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
