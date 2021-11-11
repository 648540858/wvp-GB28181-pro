package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.storager.dao.dto.RecordInfo;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface RecordInfoDao {

    @Insert("INSERT INTO recordInfo (app, stream, mediaServerId, createTime, type, deviceId, channelId, name) VALUES" +
            "('${app}', '${stream}', '${mediaServerId}', datetime('now','localtime')), '${type}', '${deviceId}', '${channelId}', '${name}'")
    int add(RecordInfo recordInfo);

    @Delete("DELETE FROM user WHERE createTime < '${beforeTime}'")
    int deleteBefore(String beforeTime);

    @Select("select * FROM recordInfo")
    List<RecordInfo> selectAll();

    @Select(value = {"<script> SELECT dc.channelId, dc.deviceId, dc.name, de.manufacturer, de.hostAddress from device_channel dc " +
            "LEFT JOIN device de ON dc.deviceId = de.deviceId where 1=1" +
            "<if test=\"manufacturer != null and manufacturer!=''\"> AND de.manufacturer LIKE '%${manufacturer}%'</if>" +
            "<if test=\"name != null and name!=''\"> AND dc.name LIKE '%${name}%'</if>" +
            "<if test=\"deviceId != null and deviceId!=''\"> AND de.deviceId LIKE '%${deviceId}%'</if>" +
            "</script>"})
    List<ChannelReduce> selectAllChannel(ChannelReduce channelReduce);
}
