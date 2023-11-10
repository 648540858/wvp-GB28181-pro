package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Gb28181CodeType;
import com.genersoft.iot.vmp.service.bean.*;
import com.github.pagehelper.PageInfo;

import java.util.ArrayList;
import java.util.List;

public interface ICommonGbChannelService {

    CommonGbChannel getChannel(String channelId);

    int add(CommonGbChannel channel);

    int addFromGbChannel(DeviceChannel channel);

    int delete(String channelId);

    int update(CommonGbChannel channel);

    boolean checkChannelInPlatform(String channelId, String platformServerId);

    /**
     * 从国标设备中同步通道
     *
     * @param gbDeviceId  国标设备编号
     * @param syncKeys    要同步的字段
     */
    boolean syncChannelFromGb28181Device(String gbDeviceId, List<String> syncKeys, Boolean syncGroup, Boolean syncRegion);

    CommonGbChannel getCommonChannelFromDeviceChannel(DeviceChannel deviceChannel, List<String> syncKeys);

    PageInfo<CommonGbChannel> getChannelsInRegion(String regionDeviceId, String query, int page, int count);

    List<CommonGbChannel> getChannelsInBusinessGroup(String businessGroupID);

    void updateChannelFromGb28181DeviceInList(Device device, List<DeviceChannel> deviceChannels);

    void addChannelFromGb28181DeviceInList(Device device, List<DeviceChannel> deviceChannels);

    void deleteGbChannelsFromList(List<DeviceChannel> deleteChannelList);

    void channelsOnlineFromList(List<DeviceChannel> deleteChannelList);

    void channelsOfflineFromList(List<DeviceChannel> deleteChannelList);

    PageInfo<CommonGbChannel> queryChannelListInGroup(String groupDeviceId, String query, int page, int count);

    PageInfo<CommonGbChannel> queryChannelList(String query, int page, int count);

    String getRandomCode(Gb28181CodeType type);

    List<IndustryCodeType> getIndustryCodeList();

    List<DeviceType> getDeviceTypeList();

    List<NetworkIdentificationType> getNetworkIdentificationTypeList();

}
