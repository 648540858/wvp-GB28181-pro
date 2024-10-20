package com.genersoft.iot.vmp.jt1078.dao;

import com.genersoft.iot.vmp.jt1078.bean.JTChannel;
import com.genersoft.iot.vmp.jt1078.dao.provider.JTChannelProvider;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface JTChannelMapper {

    @SelectProvider(type = JTChannelProvider.class, method = "selectAll")
    List<JTChannel> selectAll(@Param("terminalDbId") int terminalDbId, @Param("query") String query);

    @Update(value = {" <script>" +
            "UPDATE wvp_jt_channel " +
            "SET update_time=#{updateTime}, terminal_db_id=#{terminalDbId}, has_audio=#{hasAudio}, name=#{name}" +
            ", channel_id=#{channelId}" +
            "WHERE id=#{id}"+
            " </script>"})
    void update(JTChannel channel);

    @Insert(value = {" <script>" +
            "INSERT INTO wvp_jt_channel (" +
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
            " )</script>"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void add(JTChannel channel);

    @Delete("delete from wvp_jt_channel where id = #{id}")
    void delete(@Param("id") int id);

    @SelectProvider(type = JTChannelProvider.class, method = "selectChannelByChannelId")
    JTChannel selectChannelByChannelId(@Param("terminalDbId") int terminalDbId, @Param("channelId") Integer channelId);

    @SelectProvider(type = JTChannelProvider.class, method = "selectChannelById")
    JTChannel selectChannelById(@Param("id") Integer id);
}
