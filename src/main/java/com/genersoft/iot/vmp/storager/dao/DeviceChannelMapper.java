package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.common.PageResult;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeviceChannelMapper {
    int update(DeviceChannel channel);

    List<DeviceChannel> queryChannelsByDeviceId(String deviceId);

    List<DeviceChannel> queryChannelsByDeviceId(String deviceId, String parentChannelId);

    DeviceChannel queryChannel(String deviceId, String channelId);

    int cleanChannelsByDeviceId(String deviceId);
}
