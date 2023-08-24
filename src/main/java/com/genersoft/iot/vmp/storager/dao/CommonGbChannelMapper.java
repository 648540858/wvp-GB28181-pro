package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface CommonGbChannelMapper {
    List<CommonGbChannel> getChannels(String commonBusinessGroupPath);

    int updateChanelForBusinessGroup(List<CommonGbChannel> channels);

    int removeChannelsForBusinessGroup(List<CommonGbChannel> channels);

    int updateBusinessGroupPath(String commonBusinessGroupPath);

    CommonGbChannel queryByDeviceID(String channelId);

    int add(CommonGbChannel channel);

    int deleteByDeviceID(String channelId);

    int update(CommonGbChannel channel);

    boolean checkChannelInPlatform(String channelId, String platformServerId);
}
