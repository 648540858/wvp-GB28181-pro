package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.common.enums.DeviceControlType;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelReduce;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.web.gb28181.dto.DeviceChannelExtend;
import com.github.pagehelper.PageInfo;
import org.dom4j.Element;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 国标通道业务类
 * @author lin
 */
public interface IDeviceChannelService {

    /**
     * 批量添加设备通道
     */
    int updateChannels(Device device, List<DeviceChannel> channels);

    /**
     * 获取统计信息
     * @return
     */
    ResourceBaseInfo getOverview();

    /**
     * 查询所有未分配的通道
     * @param platformId
     * @return
     */
    List<ChannelReduce> queryAllChannelList(String platformId);

    PageInfo<ChannelReduce> queryAllChannelList(int page, int count, String query, Boolean online, Boolean channelType, String platformId, String catalogId);

    /**
     * 查询通道所属的设备
     */
    List<Device> getDeviceByChannelId(String channelId);

    /**
     * 批量删除通道
     * @param deleteChannelList 待删除的通道列表
     */
    int deleteChannelsForNotify(List<DeviceChannel> deleteChannelList);

    int updateChannelsStatus(List<DeviceChannel> channels);

    /**
     *  获取一个通道
     */
    DeviceChannel getOne(String deviceId, String channelId);

    DeviceChannel getOneForSource(String deviceId, String channelId);

    /**
     * 直接批量更新通道
     */
    void batchUpdateChannelForNotify(List<DeviceChannel> channels);

    /**
     * 直接批量添加
     */
    void batchAddChannel(List<DeviceChannel> deviceChannels);

    /**
     * 修改通道的码流类型
     */
    void updateChannelStreamIdentification(DeviceChannel channel);

    List<DeviceChannel> queryChaneListByDeviceId(String deviceId);

    void updateChannelGPS(Device device, DeviceChannel deviceChannel, MobilePosition mobilePosition);

    void startPlay(Integer channelId, String stream);

    void stopPlay(Integer channelId);

    void batchUpdateChannelGPS(List<DeviceChannel> channelList);

    void batchAddMobilePosition(List<MobilePosition> addMobilePositionList);

    void online(DeviceChannel channel);

    void offline(DeviceChannel channel);

    void delete(DeviceChannel channel);

    void cleanChannelsForDevice(int deviceId);

    boolean resetChannels(int deviceDbId, List<DeviceChannel> deviceChannels);

    PageInfo<DeviceChannel> getSubChannels(int deviceDbId, String channelId, String query, Boolean channelType, Boolean online, int page, int count);

    List<DeviceChannelExtend> queryChannelExtendsByDeviceId(String deviceId, List<String> channelIds, Boolean online);

    PageInfo<DeviceChannel> queryChannelsByDeviceId(String deviceId, String query, Boolean channelType, Boolean online, int page, int count);


    List<Device> queryDeviceWithAsMessageChannel();

    DeviceChannel getRawChannel(int id);

    DeviceChannel getOneById(Integer channelId);

    DeviceChannel getOneForSourceById(Integer channelId);

    DeviceChannel getBroadcastChannel(int deviceDbId);

    void changeAudio(Integer channelId, Boolean audio);

    void updateChannelStatus(DeviceChannel channel);

    void addChannel(DeviceChannel channel);

    void updateChannelForNotify(DeviceChannel channel);

    DeviceChannel getOneForSource(int deviceDbId, String channelId);

    DeviceChannel getOneBySourceId(int deviceDbId, String channelId);

    List<DeviceChannel> queryChaneListByDeviceDbId(Integer deviceDbId);

    List<Integer> queryChaneIdListByDeviceDbIds(List<Integer> deviceDbId);

    void handlePtzCmd(@NotNull Integer dataDeviceId, @NotNull Integer gbId, Element rootElement, DeviceControlType type, ErrorCallback<String> callback);

    void queryRecordInfo(Device device, DeviceChannel channel, String startTime, String endTime, ErrorCallback<RecordInfo> object);

    void queryRecordInfo(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<RecordInfo> object);

}
