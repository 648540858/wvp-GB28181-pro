package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;

import java.util.List;

/**
 * 国标通道业务类
 * @author lin
 */
public interface IDeviceChannelService {

    /**
     * 更新gps信息
     */
    DeviceChannel updateGps(DeviceChannel deviceChannel, Device device);

    /**
     * 添加设备通道
     *
     * @param deviceId 设备id
     * @param channel 通道
     */
    void updateChannel(String deviceId, DeviceChannel channel);

    /**
     * 批量添加设备通道
     *
     * @param deviceId 设备id
     * @param channels 多个通道
     */
    int updateChannels(String deviceId, List<DeviceChannel> channels);

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

    /**
     * 数据位置信息格式处理
     */
    boolean updateAllGps(Device device);

    /**
     * 查询通道所属的设备
     */
    List<Device> getDeviceByChannelId(String channelId);

    /**
     * 批量删除通道
     * @param deleteChannelList 待删除的通道列表
     */
    int deleteChannels(List<DeviceChannel> deleteChannelList);

    /**
     * 批量上线
     */
    int channelsOnline(List<DeviceChannel> channels);

    /**
     * 批量下线
     */
    int channelsOffline(List<DeviceChannel> channels);

    /**
     *  获取一个通道
     */
    DeviceChannel getOne(String deviceId, String channelId);

    /**
     * 直接批量更新通道
     */
    void batchUpdateChannel(List<DeviceChannel> channels);

    /**
     * 直接批量添加
     */
    void batchAddChannel(List<DeviceChannel> deviceChannels);
}
