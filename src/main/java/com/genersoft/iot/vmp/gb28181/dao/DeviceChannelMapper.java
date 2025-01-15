package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelReduce;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.web.gb28181.dto.DeviceChannelExtend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储设备通道信息
 */
@Mapper
@Repository
public interface DeviceChannelMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(DeviceChannel channel);

    int update(DeviceChannel channel);

    List<DeviceChannel> queryChannels(@Param("dataDeviceId") int dataDeviceId, @Param("civilCode") String civilCode,
                                      @Param("businessGroupId") String businessGroupId, @Param("parentChannelId") String parentChannelId,
                                      @Param("query") String query, @Param("hasSubChannel") Boolean hasSubChannel,
                                      @Param("online") Boolean online, @Param("channelIds") List<String> channelIds);

    List<DeviceChannel> queryChannelsByDeviceDbId(@Param("dataDeviceId") int dataDeviceId);

    List<Integer> queryChaneIdListByDeviceDbIds(List<Integer> deviceDbIds);

    int cleanChannelsByDeviceId(@Param("dataDeviceId") int dataDeviceId);

    int del(@Param("id") int id);

    List<DeviceChannelExtend> queryChannelsWithDeviceInfo(@Param("deviceId") String deviceId, @Param("parentChannelId") String parentChannelId, @Param("query") String query, @Param("hasSubChannel") Boolean hasSubChannel, @Param("online") Boolean online, @Param("channelIds") List<String> channelIds);

    void startPlay(@Param("channelId") Integer channelId, @Param("streamId") String streamId);

    List<ChannelReduce> queryChannelListInAll(@Param("query") String query, @Param("online") Boolean online, @Param("hasSubChannel") Boolean hasSubChannel, @Param("platformId") String platformId, @Param("catalogId") String catalogId);

    void offline(@Param("id") int id);

    int batchAdd(@Param("addChannels") List<DeviceChannel> addChannels);

    void online(@Param("id") int id);

    int batchUpdate(List<DeviceChannel> updateChannels);

    int batchUpdateForNotify(List<DeviceChannel> updateChannels);

    int updateChannelSubCount(@Param("dataDeviceId") int dataDeviceId, @Param("channelId") String channelId);

    int updatePosition(DeviceChannel deviceChannel);

    List<DeviceChannel> queryAllChannelsForRefresh(@Param("dataDeviceId") int dataDeviceId);

    List<Device> getDeviceByChannelDeviceId(@Param("channelId") String channelId);

    int batchDel(List<DeviceChannel> deleteChannelList);

    int batchUpdateStatus(List<DeviceChannel> channels);

    int getOnlineCount();

    int getAllChannelCount();

    void updateChannelStreamIdentification(DeviceChannel channel);

    void updateAllChannelStreamIdentification(@Param("streamIdentification") String streamIdentification);

    void batchUpdatePosition(List<DeviceChannel> channelList);

    DeviceChannel getOne(@Param("id") int id);

    DeviceChannel getOneForSource(@Param("id") int id);

    DeviceChannel getOneByDeviceId(@Param("dataDeviceId") int dataDeviceId, @Param("channelId") String channelId);

    DeviceChannel getOneByDeviceIdForSource(@Param("dataDeviceId") int dataDeviceId, @Param("channelId") String channelId);

    void stopPlayById(@Param("channelId") Integer channelId);

    void changeAudio(@Param("channelId") int channelId, @Param("audio") boolean audio);

    void updateStreamGPS(List<GPSMsgInfo> gpsMsgInfoList);

    void updateStatus(DeviceChannel channel);

    void updateChannelForNotify(DeviceChannel channel);

    DeviceChannel getOneBySourceChannelId(@Param("dataDeviceId") int dataDeviceId, @Param("channelId") String channelId);
}
