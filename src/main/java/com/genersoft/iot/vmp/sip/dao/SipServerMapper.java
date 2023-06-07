package com.genersoft.iot.vmp.sip.dao;

import com.genersoft.iot.vmp.sip.bean.SipServer;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SipServerMapper {

    @Insert("INSERT INTO wvp_sip_server (local_ip, local_port, server_ip, server_port, create_time, update_time, transport, status ) " +
            "VALUES (#{localIp}, #{localPort}, #{serverIp}, #{serverPort}, #{createTime}, #{updateTime}, #{transport}, #{status})")
    int add(SipServer sipServer);

    @Delete("DELETE FROM wvp_sip_server WHERE id = #{sipServerId}")
    int remove(int sipServerId);

    @Update(value = {" <script>" +
            "UPDATE wvp_sip_server " +
            "SET update_time=#{updateTime}" +
            "<if test='local_ip != null'>, local_ip=#{localIp}</if>" +
            "<if test='local_port != null'>, local_port=#{localPort}</if>" +
            "<if test='server_ip != null'>, server_ip=#{serverIp}</if>" +
            "<if test='server_port != null'>, server_port=#{server_port}</if>" +
            "<if test='transport != null'>, transport=#{transport}</if>" +
            "<if test='status != null'>, status=#{status}</if>" +
            "WHERE id = #{id}"+
            " </script>"})
    int update(SipServer sipServer);

    @Select("SELECT * FROM wvp_sip_server WHERE id = #{sipServerId}")
    SipServer query(int sipServerId);

    @Select("SELECT * FROM wvp_sip_server")
    List<SipServer> all();

    @Select("SELECT * FROM wvp_sip_server WHERE server_ip = #{host} AND server_port = #{port}")
    SipServer getOneByServerAddress(String host, int port);
}
