package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.service.IMobilePositionService;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMobilePositionMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class MobilePositionServiceImpl implements IMobilePositionService {

    @Autowired
    private DeviceChannelMapper channelMapper;

    @Autowired
    private DeviceMobilePositionMapper mobilePositionMapper;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<String, MobilePosition> redisTemplate;

    private final static Logger logger = LoggerFactory.getLogger(MobilePositionServiceImpl.class);

    private final String REDIS_MOBILE_POSITION_LIST = "redis_mobile_position_list";

    @Override
    public void add(MobilePosition mobilePosition) {
        List<MobilePosition> list = new ArrayList<>();
        list.add(mobilePosition);
        add(list);
    }

    @Override
    public void add(List<MobilePosition> mobilePositionList) {
        redisTemplate.opsForList().leftPushAll(REDIS_MOBILE_POSITION_LIST, mobilePositionList);
    }

    private List<MobilePosition> get(int length) {
        Long size = redisTemplate.opsForList().size(REDIS_MOBILE_POSITION_LIST);
        if (size == null || size == 0) {
            return new ArrayList<>();
        }
        List<MobilePosition> mobilePositions;
        if (size > length) {
            mobilePositions = redisTemplate.opsForList().rightPop(REDIS_MOBILE_POSITION_LIST, length);
        }else {
            mobilePositions = redisTemplate.opsForList().rightPop(REDIS_MOBILE_POSITION_LIST, size);
        }
        return  mobilePositions;
    }



    @Scheduled(fixedRate = 1000)
    @Transactional
    public void executeTaskQueue() {
        int countLimit = 3000;
        List<MobilePosition> mobilePositions = get(countLimit);
        if (mobilePositions == null || mobilePositions.isEmpty()) {
            return;
        }
        if (userSetting.getSavePositionHistory()) {
            mobilePositionMapper.batchadd(mobilePositions);
        }
        logger.info("[移动位置订阅]更新通道位置： {}", mobilePositions.size());
        Map<String, DeviceChannel> updateChannelMap = new HashMap<>();
        for (MobilePosition mobilePosition : mobilePositions) {
            DeviceChannel deviceChannel = new DeviceChannel();
            deviceChannel.setDeviceId(mobilePosition.getDeviceId());
            deviceChannel.setChannelId(mobilePosition.getChannelId());
            deviceChannel.setLongitude(mobilePosition.getLongitude());
            deviceChannel.setLatitude(mobilePosition.getLatitude());
            deviceChannel.setUpdateTime(DateUtil.getNow());
            deviceChannel.setGpsTime(mobilePosition.getTime());
            deviceChannel.setGpsSpeed(mobilePosition.getSpeed());
            deviceChannel.setGpsDirection(mobilePosition.getDirection() + "");
            deviceChannel.setGpsAltitude(mobilePosition.getAltitude() + "");
            updateChannelMap.put(mobilePosition.getDeviceId() + mobilePosition.getChannelId(), deviceChannel);
        }
        List<DeviceChannel> channels = new ArrayList<>(updateChannelMap.values());
        channelMapper.batchUpdatePosition(channels);
    }

}
