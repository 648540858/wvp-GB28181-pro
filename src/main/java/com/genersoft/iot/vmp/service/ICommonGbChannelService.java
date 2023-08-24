package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;

public interface ICommonGbChannelService {

    CommonGbChannel getChannel(String channelId);

    int add(CommonGbChannel channel);

    int delete(String channelId);

    int update(CommonGbChannel channel);

    boolean checkChannelInPlatform(String channelId, String platformServerId);

    /**
     * 从国标设备中同步通道
     *
     * @param gbDeviceId        国标设备编号
     * @param syncCoordinate    是否同步位置信息，TRUE 则使用国标设备里的位置信息， 第一次同步按照TRUE执行，此参数无效
     * @param syncBusinessGroup 是否同步业务分组，TRUE则使用国标设备的业务分组
     * @param syncRegion        是否同步行政区划，TRUE则使用国标设备的行政区划
     */
    boolean SyncChannelFromGb28181Device(String gbDeviceId, boolean syncCoordinate, boolean syncBusinessGroup, boolean syncRegion);
}
