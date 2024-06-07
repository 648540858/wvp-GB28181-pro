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
            "terminal_db_id = #{terminalDbId}" +
            " <if test='query != null'> AND " +
            "jc.name LIKE concat('%',#{query},'%') " +
            "</if> " +
            "ORDER BY jc.channel_id " +
            " </script>"})
    List<JTChannel> getAll(@Param("terminalDbId") int terminalDbId, @Param("query") String query);

    @Update(value = {" <script>" +
            "UPDATE wvp_jt_channel " +
            "SET update_time=#{updateTime}" +
            "<if test=\"terminalDbId != null\">, terminal_db_id=#{terminalDbId}</if>" +
            "<if test=\"hasAudio != null\">, has_audio=#{hasAudio}</if>" +
            "<if test=\"name != null\">, name=#{name}</if>" +
            "<if test=\"channelId != null\">, channel_id=#{channelId}</if>" +
            "WHERE id=#{id}"+
            " </script>"})
    void update(JTChannel channel);

    @Insert("INSERT INTO wvp_jt_channel (" +
            "terminal_db_id,"+
            "channel_id,"+
            "name,"+
            "has_audio,"+
            "create_time,"+
            "update_time"+
            ") VALUES (" +
            "#{terminalDbId}," +
            "#{channelId}," +
            "#{name}," +
            "#{hasAudio}," +
            "#{createTime}," +
            "#{updateTime}" +
            ")")
    void add(JTChannel channel);

    @Delete("delete from wvp_jt_channel where id = #{id}")
    void delete(@Param("id") int id);

    @Select(value = {" <script>" +
            "SELECT * " +
            "from " +
            "wvp_jt_channel jc " +
            "WHERE " +
            "terminal_db_id = #{terminalDbId} and channel_id = #{channelId}" +
            " </script>"})
    JTChannel getChannel(@Param("terminalDbId") int terminalDbId, @Param("channelId") Integer channelId);

}
