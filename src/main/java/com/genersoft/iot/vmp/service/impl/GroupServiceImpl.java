package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.service.IGroupService;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.storager.dao.GroupMapper;
import com.genersoft.iot.vmp.storager.dao.CommonGbChannelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

@Service
public class GroupServiceImpl implements IGroupService {

    private final static Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Autowired
    private CommonGbChannelMapper commonGbChannelDao;

    @Autowired
    private GroupMapper businessGroupDao;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;


    @Override
    public List<Group> getNodes(String parentId) {
        return businessGroupDao.getNodes(parentId);
    }

    @Override
    public List<CommonGbChannel> getChannels(int id) {
        Group businessGroup = businessGroupDao.query(id);
        if (businessGroup == null) {
            return null;
        }
        return commonGbChannelDao.getChannels(businessGroup.getCommonBusinessGroupPath());
    }

    @Override
    public List<CommonGbChannel> getChannels(String deviceId) {
        Group businessGroup = businessGroupDao.queryByDeviceId(deviceId);
        if (businessGroup == null) {
            return null;
        }
        return commonGbChannelDao.getChannels(businessGroup.getCommonBusinessGroupPath());
    }

    @Override
    public boolean add(Group businessGroup) {
        return businessGroupDao.add(businessGroup) > 0;
    }

    @Override
    public boolean remove(int id) {
        return businessGroupDao.remove(id) > 0;
    }

    @Override
    public boolean remove(String deviceId) {
        return businessGroupDao.removeByDeviceId(deviceId) > 0;
    }

    @Override
    public boolean update(Group businessGroup) {
        if (businessGroup.getCommonBusinessGroupId() == 0) {
            return false;
        }
        Group businessGroupInDb = businessGroupDao.query(businessGroup.getCommonBusinessGroupId());
        if (businessGroupInDb == null) {
            return false;
        }
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        boolean result = false;
        if (!businessGroupInDb.getCommonBusinessGroupPath().equals(businessGroup.getCommonBusinessGroupPath())) {
            // 需要更新通道信息
            int updateCount = commonGbChannelDao.updateBusinessGroupPath(businessGroupInDb.getCommonBusinessGroupPath(), businessGroup.getCommonBusinessGroupPath());
            if (updateCount > 0) {
                dataSourceTransactionManager.rollback(transactionStatus);
                return false;
            } else {
                result = businessGroupDao.update(businessGroup) > 0;
            }
        } else {
            result = businessGroupDao.update(businessGroup) > 0;
        }
        dataSourceTransactionManager.commit(transactionStatus);
        return result;
    }

    @Override
    public boolean updateChannelsToBusinessGroup(int id, List<CommonGbChannel> channels) {
        if (channels.isEmpty()) {
            return false;
        }
        Group businessGroup = businessGroupDao.query(id);
        if (businessGroup == null) {
            return false;
        }
        for (CommonGbChannel channel : channels) {
            channel.setCommonGbBusinessGroupID(businessGroup.getCommonBusinessGroupPath());
        }
        // TODO 增加对数量的判断，分批处理
        return commonGbChannelDao.updateChanelForBusinessGroup(channels) > 1;
    }

    @Override
    public boolean updateChannelsToBusinessGroup(String deviceId, List<CommonGbChannel> channels) {
        if (channels.isEmpty()) {
            return false;
        }
        Group businessGroup = businessGroupDao.queryByDeviceId(deviceId);
        if (businessGroup == null) {
            return false;
        }
        for (CommonGbChannel channel : channels) {
            channel.setCommonGbBusinessGroupID(businessGroup.getCommonBusinessGroupPath());
        }
        // TODO 增加对数量的判断，分批处理
        return commonGbChannelDao.updateChanelForBusinessGroup(channels) > 1;
    }

    @Override
    public boolean removeChannelsFromBusinessGroup(List<CommonGbChannel> channels) {
        // TODO 增加对数量的判断，分批处理
        return commonGbChannelDao.removeChannelsForBusinessGroup(channels) > 1;
    }

}
