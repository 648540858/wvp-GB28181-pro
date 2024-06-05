package com.genersoft.iot.vmp.jt1078.dao;

import com.genersoft.iot.vmp.jt1078.bean.JTChannel;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface JTChannelMapper {

    @Select(value = {" <script>" +
            "SELECT * " +
            "from " +
            "wvp_jt_channel jc " +
            "WHERE " +
            "device_id = #{deviceId}" +
            " <if test='query != null'> AND " +
            "jc.name LIKE concat('%',#{query},'%') " +
            "</if> " +
            "ORDER BY jc.update_time " +
            " </script>"})
    List<JTChannel> getAll(@Param("deviceId") int deviceId, @Param("query") String query);

    @Update(value = {" <script>" +
            "UPDATE wvp_jt_channel " +
            "SET update_time=#{updateTime}" +
            "<if test=\"deviceId != null\">, device_id=#{deviceId}</if>" +
            "<if test=\"name != null\">, name=#{name}</if>" +
            "<if test=\"channelId != null\">, channelId=#{channelId}</if>" +
            "WHERE id=#{id}"+
            " </script>"})
    void update(JTChannel channel);

    @Insert("INSERT INTO wvp_jt_channel (" +
            "device_id,"+
            "channel_id,"+
            "name,"+
            "create_time,"+
            "update_time"+
            ") VALUES (" +
            "#{deviceId}," +
            "#{channelId}," +
            "#{name}," +
            "#{createTime}," +
            "#{updateTime}" +
            ")")
    void add(JTChannel channel);

    @Delete("delete from wvp_jt_channel where id = #{id}")
    void delete(@Param("id") int id);

}
