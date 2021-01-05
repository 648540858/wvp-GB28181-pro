package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储设备信息
 */
@Mapper
@Repository
public interface DeviceMapper {

    @Select("SELECT * FROM device WHERE deviceId = #{deviceId}")
    Device getDeviceByDeviceId(String deviceId);

    @Insert("INSERT INTO device (" +
                "deviceId, " +
                "name, " +
                "manufacturer, " +
                "model, " +
                "firmware, " +
                "transport," +
                "streamMode," +
                "ip," +
                "port," +
                "hostAddress," +
                "online" +
            ") VALUES (" +
                "#{deviceId}," +
                "#{name}," +
                "#{manufacturer}," +
                "#{model}," +
                "#{firmware}," +
                "#{transport}," +
                "#{streamMode}," +
                "#{ip}," +
                "#{port}," +
                "#{hostAddress}," +
                "#{online}" +
            ")")
    int add(Device device);


    @Update("UPDATE device " +
            "SET name=#{name}, " +
            "manufacturer=#{manufacturer}," +
            "model=#{model}," +
            "firmware=#{firmware}, " +
            "transport=#{transport}," +
            "streamMode=#{streamMode}, " +
            "ip=#{ip}, " +
            "port=#{port}, " +
            "hostAddress=#{hostAddress}, " +
            "online=#{online} " +
            "WHERE deviceId=#{deviceId}")
    int update(Device device);

    @Select("SELECT *, (SELECT count(0) FROM device_channel WHERE deviceId=de.deviceId) as channelCount  FROM device de")
    List<Device> getDevices();

    @Delete("DELETE FROM device WHERE deviceId=#{deviceId}")
    int del(String deviceId);
}
