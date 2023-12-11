package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Gb28181CodeType;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.vmanager.bean.UpdateCommonChannelToGroup;
import com.genersoft.iot.vmp.vmanager.bean.UpdateCommonChannelToRegion;
import com.github.pagehelper.PageInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用通道资源管理
 * 接入的资源通过调用这个类实现将自己本身的数据添加到通用通道当中
 */
public interface ICommonGbChannelService {

    CommonGbChannel getChannel(String channelId);

    int add(CommonGbChannel channel);

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

    PageInfo<CommonGbChannel> queryChannelListInGroup(int page, int count, String query, String groupDeviceId,
                                                      String regionDeviceId, Boolean inGroup, Boolean inRegion,
                                                      String type);

    PageInfo<CommonGbChannel> queryChannelList(String query, int page, int count);

    List<IndustryCodeType> getIndustryCodeList();

    List<DeviceType> getDeviceTypeList();

    List<NetworkIdentificationType> getNetworkIdentificationTypeList();

    void updateChannelToGroup(UpdateCommonChannelToGroup commonGbChannel);

    void removeFromGroup(UpdateCommonChannelToGroup params);

    void removeFromRegion(UpdateCommonChannelToRegion params);

    void updateChannelToRegion(UpdateCommonChannelToRegion params);

    void startPlay(CommonGbChannel channel, IResourcePlayCallback callback);

    void stopPlay(CommonGbChannel channel, IResourcePlayCallback callback);

    void batchAdd(List<CommonGbChannel> commonGbChannels);

    void batchUpdate(List<CommonGbChannel> commonGbChannels);

    void batchDelete(List<Integer> allCommonChannelsForDelete);

    void deleteById(int commonGbChannelId);

    void deleteByIdList(List<Integer> commonChannelIdList);

    void offlineForList(List<Integer> onlinePushers);
}
