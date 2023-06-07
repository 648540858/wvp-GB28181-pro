package com.genersoft.iot.vmp.sip.dao;

import com.genersoft.iot.vmp.sip.bean.SipVideo;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SipVideoMapper {

    @Select("SELECT * FROM wvp_sip_video WHERE sip_server_id = #{serverId} AND sip_account_id = #{accountId}")
    List<SipVideo> all(int serverId, int accountId);

    @Insert("INSERT INTO wvp_sip_video (sip_server_id, sip_account_id, media_server_id, request_no, create_time, update_time, auto_reconnect_on_reboot, status ) " +
            "VALUES (#{sipServerId}, #{sipAccountId}, #{mediaServerId}, #{requestNo}, #{createTime}, #{updateTime}, #{autoReconnectOnReboot}, #{status})")
    int add(SipVideo sipVideo);

    @Update(value = {" <script>" +
            "UPDATE wvp_sip_video " +
            "SET update_time=#{updateTime}" +
            "<if test='media_server_id != null'>, media_server_id=#{mediaServerId}</if>" +
            "<if test='request_no != null'>, request_no=#{requestNo}</if>" +
            "<if test='auto_reconnect_on_reboot != null'>, auto_reconnect_on_reboot=#{autoReconnectOnReboot}</if>" +
            "<if test='status != null'>, status=#{status}</if>" +
            "WHERE id = #{id}"+
            " </script>"})
    int update(SipVideo video);

    @Delete("DELETE FROM wvp_sip_video WHERE id = #{videoId}")
    void remove(Integer videoId);
}
