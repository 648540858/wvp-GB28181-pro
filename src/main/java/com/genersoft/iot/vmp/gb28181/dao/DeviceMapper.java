package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储设备信息
 */
@Mapper
@Repository
public interface DeviceMapper {

    Device getDeviceByDeviceId(@Param("deviceId") String deviceId);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(Device device);

    int update(Device device);

    List<Device> getDevices(@Param("dataType") Integer dataType, @Param("online") Boolean online);

    int del(String deviceId);

    List<Device> getOnlineDevices();

    Device getDeviceByHostAndPort(@Param("host") String host, @Param("port") int port);

    void updateCustom(Device device);

    void addCustomDevice(Device device);

    List<Device> getAll();

    List<Device> queryDeviceWithAsMessageChannel();

    List<Device> getDeviceList(@Param("dataType") Integer dataType, @Param("query") String query, @Param("status") Boolean status);

    DeviceChannel getRawChannel(@Param("id") int id);

    Device query(@Param("id") Integer id);

    Device queryByChannelId(@Param("dataType") Integer dataType, @Param("channelId") Integer channelId);

    Device getDeviceBySourceChannelDeviceId(@Param("dataType") Integer dataType, @Param("channelDeviceId") String channelDeviceId);

    void updateSubscribeCatalog(Device device);

    void updateSubscribeMobilePosition(Device device);
}
