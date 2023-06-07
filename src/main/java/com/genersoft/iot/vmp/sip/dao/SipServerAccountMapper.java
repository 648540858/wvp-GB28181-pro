package com.genersoft.iot.vmp.sip.dao;

import com.genersoft.iot.vmp.sip.bean.SipServerAccount;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SipServerAccountMapper {

    @Insert("INSERT INTO wvp_sip_server_account (sip_server_id, username, password, device_channel_id, " +
            "create_time, update_time, push_stream_id, proxy_stream_id, status ) " +
            "VALUES (#{sipServerId}, #{username}, #{password}, #{deviceChannelId}, " +
            "#{createTime}, #{updateTime}, #{pushStreamId}, #{proxyStreamId}, #{status})")
    int add(SipServerAccount sipServerAccount);

    @Delete("DELETE FROM wvp_sip_server_account WHERE id = #{id}")
    int remove(int id);

    @Update(value = {" <script>" +
            "UPDATE wvp_sip_server_account " +
            "SET update_time=#{updateTime}" +
            "<if test='sip_server_id != null'>, sip_server_id=#{sipServerId}</if>" +
            "<if test='username != null'>, username=#{username}</if>" +
            "<if test='password != null'>, password=#{password}</if>" +
            "<if test='device_channel_id != null'>, device_channel_id=#{deviceChannelId}</if>" +
            "<if test='push_stream_id != null'>, push_stream_id=#{pushStreamId}</if>" +
            "<if test='proxy_stream_id != null'>, proxy_stream_id=#{proxyStreamId}</if>" +
            "<if test='status != null'>, status=#{status}</if>" +
            "WHERE id = #{id}"+
            " </script>"})
    int update(SipServerAccount sipServerAccount);

    @Select("SELECT * FROM wvp_sip_server_account WHERE id = #{id}")
    SipServerAccount query(int id);

    @Select("SELECT * FROM wvp_sip_server_account")
    List<SipServerAccount> all(int id);
}
