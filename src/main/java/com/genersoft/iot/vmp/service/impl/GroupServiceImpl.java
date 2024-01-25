package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.BatchLimit;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Gb28181CodeType;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.service.IGroupService;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.storager.dao.GroupMapper;
import com.genersoft.iot.vmp.storager.dao.CommonChannelMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.PageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GroupServiceImpl implements IGroupService {

    private final static Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Autowired
    private CommonChannelMapper commonGbChannelMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;


    @Override
    public List<Group> getNodes(String parentId) {
        return groupMapper.getNodes(parentId);
    }

    @Override
    public List<CommonGbChannel> getChannels(int id) {
        Group group = groupMapper.queryOne(id);
        if (group == null) {
            return null;
        }
        return commonGbChannelMapper.getChannels(group.getCommonGroupDeviceId());
    }

    @Override
    public List<CommonGbChannel> getChannels(String deviceId) {
        Group group = groupMapper.queryByDeviceId(deviceId);
        if (group == null) {
            return null;
        }
        return commonGbChannelMapper.getChannels(group.getCommonGroupDeviceId());
    }

    @Override
    public boolean add(Group group) {
        assert group.getCommonGroupDeviceId() != null;
        assert group.getCommonGroupDeviceId() != null;
        group.setCommonGroupCreateTime(DateUtil.getNow());
        group.setCommonGroupUpdateTime(DateUtil.getNow());
        Gb28181CodeType channelIdType = SipUtils.getChannelIdType(group.getCommonGroupDeviceId());
        if (ObjectUtils.isEmpty(group.getCommonGroupParentId().trim()) || channelIdType.equals(Gb28181CodeType.BUSINESS_GROUP)) {
            group.setCommonGroupParentId(null);
        }
        if (ObjectUtils.isEmpty(group.getCommonGroupTopId().trim()) && channelIdType.equals(Gb28181CodeType.BUSINESS_GROUP)) {
            group.setCommonGroupTopId(group.getCommonGroupDeviceId());
        }
        return groupMapper.add(group) > 0;
    }

    @Override
    public boolean remove(int id) {
        return groupMapper.remove(id) > 0;
    }

    @Override
    @Transactional
    public boolean remove(String deviceId) {
        assert deviceId != null;
        Gb28181CodeType channelIdType = SipUtils.getChannelIdType(deviceId);
        // 查询所有从属的分组
        List<Group> groupList;
        if (channelIdType == Gb28181CodeType.BUSINESS_GROUP) {
            // 如果要删除的是一个业务分组，那么直接查询commonGroupTopId是这个节点的即可，这里也包括要删除的节点本身
            groupList = groupMapper.queryGroupListByTopId(deviceId);
        }else {
            // 如果要删除的是一个虚拟组织，那么就不只能不断的递归拿到所有的子节点了
            Group group = groupMapper.queryByDeviceId(deviceId);
            assert  group != null;
            List<Group> groupParentList = new ArrayList<>();
            groupParentList.add(group);
            groupList = queryAllChildGroup(groupParentList, group.getCommonGroupTopId(), groupParentList);
        }

        if (!groupList.isEmpty()) {
            if (groupList.size() > BatchLimit.count) {
                for (int i = 0; i < groupList.size(); i += BatchLimit.count) {
                    int toIndex = i + BatchLimit.count;
                    if (i + BatchLimit.count > groupList.size()) {
                        toIndex = groupList.size();
                    }
                    List<Group> subList = groupList.subList(i, toIndex);
                    // 移除所有管理当前节点和字节点的通道
                    commonGbChannelMapper.removeGroupInfo(subList);
                    // 移除所有子节点
                    groupMapper.removeGroupByList(subList);
                }
            }else {
                // 移除所有管理当前节点和字节点的通道
                commonGbChannelMapper.removeGroupInfo(groupList);
                // 移除所有子节点
                groupMapper.removeGroupByList(groupList);
            }
        }
        return true;
    }

    private List<Group> queryAllChildGroup(List<Group> parentGroups, String commonGroupTopId, List<Group> resultList) {
        if (parentGroups.isEmpty()) {
            return resultList;
        }
        List<Group> childGroupList = groupMapper.queryChildGroupListInParentGroup(parentGroups, commonGroupTopId);
        if (childGroupList.isEmpty()) {
            return resultList;
        }else {
            resultList.addAll(childGroupList);
            return queryAllChildGroup(childGroupList, commonGroupTopId, resultList);
        }
    }

    @Override
    @Transactional
    public boolean update(Group group) {
        assert group.getCommonGroupId() >= 0;
        Group groupInDb = groupMapper.queryOne(group.getCommonGroupId());
        if (groupInDb == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未查询到待更新的分组");
        }
        if (!groupInDb.getCommonGroupDeviceId().equals(group.getCommonGroupDeviceId())) {
            // 分组编号变化
            // 修改所有子分组的父节点编号
            groupMapper.updateParentDeviceId(groupInDb.getCommonGroupDeviceId(), group.getCommonGroupDeviceId());
            // 修改所有通用通道中分组编号
            commonGbChannelMapper.updateChanelGroup(groupInDb.getCommonGroupDeviceId(), group.getCommonGroupDeviceId());
        }else if (
                ((groupInDb.getCommonGroupParentId() == null && group.getCommonGroupParentId() == null)
                        || groupInDb.getCommonGroupParentId().equals(group.getCommonGroupParentId()))
                && groupInDb.getCommonGroupName().equals(group.getCommonGroupName())) {
            // 数据无变化
            return false;
        }

        return groupMapper.update(group) > 0;
    }

    @Override
    public boolean updateChannelsToGroup(int id, List<CommonGbChannel> channels) {
        if (channels.isEmpty()) {
            return false;
        }
        Group group = groupMapper.queryOne(id);
        if (group == null) {
            return false;
        }
        return updateChannelsToGroup(group, channels);
    }

    @Override
    public boolean updateChannelsToGroup(String deviceId, List<CommonGbChannel> channels) {
        if (channels.isEmpty()) {
            return false;
        }
        Group group = groupMapper.queryByDeviceId(deviceId);
        if (group == null) {
            return false;
        }
        return updateChannelsToGroup(group, channels);
    }

    private boolean updateChannelsToGroup(Group group, List<CommonGbChannel> channels) {
        for (CommonGbChannel channel : channels) {
            channel.setCommonGbBusinessGroupID(group.getCommonGroupTopId());
            channel.setCommonGbParentID(group.getCommonGroupDeviceId());
        }
        if (channels.size() <= com.genersoft.iot.vmp.common.BatchLimit.count) {
            if (commonGbChannelMapper.updateChanelForGroup(channels) <= 0) {
                logger.info("[添加通道到分组] 失败");
                return false;
            }
        } else {
            TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            for (int i = 0; i < channels.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > channels.size()) {
                    toIndex = channels.size();
                }
                List<CommonGbChannel> channelsSub = channels.subList(i, toIndex);
                if (commonGbChannelMapper.updateChanelForGroup(channelsSub) <= 0) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    logger.info("[添加通道到分组] 失败");
                    return false;
                }
            }
            dataSourceTransactionManager.commit(transactionStatus);
        }
        return true;
    }

    @Override
    public boolean removeChannelsFromGroup(List<CommonGbChannel> channels) {
        if (channels.size() <= BatchLimit.count) {
            if (commonGbChannelMapper.removeChannelsForGroup(channels) <= 0) {
                logger.info("[从分组移除通道] 失败");
                return false;
            }
        } else {
            TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            for (int i = 0; i < channels.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > channels.size()) {
                    toIndex = channels.size();
                }
                List<CommonGbChannel> channelsSub = channels.subList(i, toIndex);
                if (commonGbChannelMapper.removeChannelsForGroup(channelsSub) <= 0) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    logger.info("[从分组移除通道] 失败");
                    return false;
                }
            }
            dataSourceTransactionManager.commit(transactionStatus);
        }
        return true;
    }

    @Override
    public PageInfo<Group> queryGroup(String query, int page, int count) {
        PageHelper.startPage(page, count);
        List<Group> groupList = groupMapper.query(query);
        return new PageInfo<>(groupList);
    }

    @Override
    public PageInfo<Group> queryChildGroupList(String groupParentId, int page, int count) {
        PageHelper.startPage(page, count);
        List<Group> groupList;
        if (groupParentId == null) {
            groupList = groupMapper.queryVirtualGroupList(groupParentId);
        }else {
            groupList = groupMapper.queryChildGroupList(groupParentId);
        }

        return new PageInfo<>(groupList);
    }

    @Override
    public Map<String, Group> getAllGroupMap() {
        return groupMapper.queryAllForMap();
    }
}
