package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Gb28181CodeType;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.service.IGroupService;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.storager.dao.GroupMapper;
import com.genersoft.iot.vmp.storager.dao.CommonGbChannelMapper;
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

import java.util.List;

@Service
public class GroupServiceImpl implements IGroupService {

    private final static Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Autowired
    private CommonGbChannelMapper commonGbChannelDao;

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
        return commonGbChannelDao.getChannels(group.getCommonGroupDeviceId());
    }

    @Override
    public List<CommonGbChannel> getChannels(String deviceId) {
        Group group = groupMapper.queryByDeviceId(deviceId);
        if (group == null) {
            return null;
        }
        return commonGbChannelDao.getChannels(group.getCommonGroupDeviceId());
    }

    @Override
    public boolean add(Group group) {
        assert group.getCommonGroupDeviceId() != null;
        assert group.getCommonGroupDeviceId() != null;
        group.setCommonGroupCreateTime(DateUtil.getNow());
        group.setCommonGroupUpdateTime(DateUtil.getNow());
        return groupMapper.add(group) > 0;
    }

    @Override
    public boolean remove(int id) {
        return groupMapper.remove(id) > 0;
    }

    @Override
    public boolean remove(String deviceId) {
        return groupMapper.removeByDeviceId(deviceId) > 0;
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
            commonGbChannelDao.updateChanelGroup(groupInDb.getCommonGroupDeviceId(), group.getCommonGroupDeviceId());
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
        int limit = 50;
        if (channels.size() <= limit) {
            if (commonGbChannelDao.updateChanelForGroup(channels) <= 0) {
                logger.info("[添加通道到分组] 失败");
                return false;
            }
        } else {
            TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            for (int i = 0; i < channels.size(); i += limit) {
                int toIndex = i + limit;
                if (i + limit > channels.size()) {
                    toIndex = channels.size();
                }
                List<CommonGbChannel> channelsSub = channels.subList(i, toIndex);
                if (commonGbChannelDao.updateChanelForGroup(channelsSub) <= 0) {
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
        int limit = 50;
        if (channels.size() <= limit) {
            if (commonGbChannelDao.removeChannelsForGroup(channels) <= 0) {
                logger.info("[从分组移除通道] 失败");
                return false;
            }
        } else {
            TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            for (int i = 0; i < channels.size(); i += limit) {
                int toIndex = i + limit;
                if (i + limit > channels.size()) {
                    toIndex = channels.size();
                }
                List<CommonGbChannel> channelsSub = channels.subList(i, toIndex);
                if (commonGbChannelDao.removeChannelsForGroup(channelsSub) <= 0) {
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
        Gb28181CodeType channelIdType = SipUtils.getChannelIdType(groupParentId);
        List<Group> groupList;
        if (groupParentId == null || channelIdType == Gb28181CodeType.BUSINESS_GROUP) {
            groupList = groupMapper.queryVirtualGroupList(groupParentId);
        }else {
            groupList = groupMapper.queryChildGroupList(groupParentId);
        }

        return new PageInfo<>(groupList);
    }
}
