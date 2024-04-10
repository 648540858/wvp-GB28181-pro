package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.storager.dao.dto.UserApiKey;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserApiKeyMapper {

    @SelectKey(databaseId = "postgresql", statement = "SELECT currval('wvp_user_api_key_id_seq'::regclass) AS id", keyProperty = "id", before = false, resultType = Integer.class)
    @SelectKey(databaseId = "mysql", statement = "SELECT LAST_INSERT_ID() AS id", keyProperty = "id", before = false, resultType = Integer.class)
    @Insert("INSERT INTO wvp_user_api_key (user_id, app, api_key, expired_at, remark, enable, create_time, update_time) VALUES" +
            "(#{userId}, #{app}, #{apiKey}, #{expiredAt}, #{remark}, #{enable}, #{createTime}, #{updateTime})")
    int add(UserApiKey userApiKey);

    @Update(value = {"<script>" +
            "UPDATE wvp_user_api_key " +
            "SET update_time = #{updateTime} " +
            "<if test=\"app != null\">, app = #{app}</if>" +
            "<if test=\"apiKey != null\">, api_key = #{apiKey}</if>" +
            "<if test=\"expiredAt != null\">, expired_at = #{expiredAt}</if>" +
            "<if test=\"remark != null\">, username = #{remark}</if>" +
            "<if test=\"enable != null\">, enable = #{enable}</if>" +
            "WHERE id = #{id}" +
            " </script>"})
    int update(UserApiKey userApiKey);

    @Update("UPDATE wvp_user_api_key SET enable = true WHERE id = #{id}")
    int enable(@Param("id") int id);

    @Update("UPDATE wvp_user_api_key SET enable = false WHERE id = #{id}")
    int disable(@Param("id") int id);

    @Update("UPDATE wvp_user_api_key SET api_key = #{apiKey} WHERE id = #{id}")
    int apiKey(@Param("id") int id, @Param("apiKey") String apiKey);

    @Update("UPDATE wvp_user_api_key SET remark = #{remark} WHERE id = #{id}")
    int remark(@Param("id") int id, @Param("remark") String remark);

    @Delete("DELETE FROM wvp_user_api_key WHERE id = #{id}")
    int delete(@Param("id") int id);

    @Select("SELECT uak.id, uak.user_id, uak.app, uak.api_key, uak.expired_at, uak.remark, uak.enable, uak.create_time, uak.update_time, u.username AS username FROM wvp_user_api_key uak LEFT JOIN wvp_user u on u.id = uak.user_id WHERE uak.id = #{id}")
    UserApiKey selectById(@Param("id") int id);

    @Select("SELECT uak.id, uak.user_id, uak.app, uak.api_key, uak.expired_at, uak.remark, uak.enable, uak.create_time, uak.update_time, u.username AS username FROM wvp_user_api_key uak LEFT JOIN wvp_user u on u.id = uak.user_id WHERE uak.api_key = #{apiKey}")
    UserApiKey selectByApiKey(@Param("apiKey") String apiKey);

    @Select("SELECT uak.id, uak.user_id, uak.app, uak.api_key, uak.expired_at, uak.remark, uak.enable, uak.create_time, uak.update_time, u.username AS username FROM wvp_user_api_key uak LEFT JOIN wvp_user u on u.id = uak.user_id")
    List<UserApiKey> selectAll();

    @Select("SELECT uak.id, uak.user_id, uak.app, uak.api_key, uak.expired_at, uak.remark, uak.enable, uak.create_time, uak.update_time, u.username AS username FROM wvp_user_api_key uak LEFT JOIN wvp_user u on u.id = uak.user_id")
    List<UserApiKey> getUserApiKeys();

    @Select("SELECT COUNT(0) FROM wvp_user_api_key WHERE api_key = #{apiKey}")
    boolean isApiKeyExists(@Param("apiKey") String apiKey);

}
