package com.genersoft.iot.vmp.storager.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SecurityConfigMapper {

    @Update("CREATE TABLE IF NOT EXISTS wvp_security_config (" +
            "server_id character varying(64) PRIMARY KEY, " +
            "config_json text NOT NULL, " +
            "update_time character varying(50) NOT NULL)")
    void createTableIfNotExists();

    @Select("SELECT config_json FROM wvp_security_config WHERE server_id=#{serverId}")
    String getConfig(@Param("serverId") String serverId);

    @Update("UPDATE wvp_security_config SET config_json=#{configJson}, update_time=#{updateTime} " +
            "WHERE server_id=#{serverId}")
    int update(@Param("serverId") String serverId,
               @Param("configJson") String configJson,
               @Param("updateTime") String updateTime);

    @Insert("INSERT INTO wvp_security_config (server_id, config_json, update_time) " +
            "VALUES (#{serverId}, #{configJson}, #{updateTime})")
    int insert(@Param("serverId") String serverId,
               @Param("configJson") String configJson,
               @Param("updateTime") String updateTime);
}
