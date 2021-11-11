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
                "expires," +
                "registerTime," +
                "keepaliveTime," +
                "createTime," +
                "updateTime," +
                "charset," +
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
                "#{expires}," +
                "#{registerTime}," +
                "#{keepaliveTime}," +
                "#{createTime}," +
                "#{updateTime}," +
                "#{charset}," +
                "#{online}" +
            ")")
    int add(Device device);

    @Update(value = {" <script>" +
                "UPDATE device " +
                "SET updateTime='${updateTime}'" +
                "<if test=\"name != null\">, name='${name}'</if>" +
                "<if test=\"manufacturer != null\">, manufacturer='${manufacturer}'</if>" +
                "<if test=\"model != null\">, model='${model}'</if>" +
                "<if test=\"firmware != null\">, firmware='${firmware}'</if>" +
                "<if test=\"transport != null\">, transport='${transport}'</if>" +
                "<if test=\"streamMode != null\">, streamMode='${streamMode}'</if>" +
                "<if test=\"ip != null\">, ip='${ip}'</if>" +
                "<if test=\"port != null\">, port=${port}</if>" +
                "<if test=\"hostAddress != null\">, hostAddress='${hostAddress}'</if>" +
                "<if test=\"online != null\">, online=${online}</if>" +
                "<if test=\"registerTime != null\">, registerTime='${registerTime}'</if>" +
                "<if test=\"keepaliveTime != null\">, keepaliveTime='${keepaliveTime}'</if>" +
                "<if test=\"expires != null\">, expires=${expires}</if>" +
                "<if test=\"charset != null\">, charset='${charset}'</if>" +
                "WHERE deviceId='${deviceId}'"+
            " </script>"})
    int update(Device device);

    @Select(value = {"<script>" +
            "SELECT *, (SELECT count(0) FROM device_channel WHERE deviceId=de.deviceId) as channelCount  FROM device de where 1 = 1" +
            "<if test=\"name != null\">and name like'%${name}%'</if>" +
            "<if test=\"manufacturer != null\">and manufacturer like '%${manufacturer}%'</if>"+
            "<if test=\"model != null\">and model like '%${model}%'</if>"+
            "<if test=\"online != null\">and online=${online}</if>"+
            "</script>"})

    List<Device> getDevices(Device device);

    @Delete("DELETE FROM device WHERE deviceId=#{deviceId}")
    int del(String deviceId);

    @Update("UPDATE device SET online=0")
    int outlineForAll();
}
