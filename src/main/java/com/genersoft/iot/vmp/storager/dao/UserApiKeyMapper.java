package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.storager.dao.dto.UserApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectKey;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserApiKeyMapper {
    @SelectKey(databaseId = "postgresql", statement = "SELECT currval('wvp_user_api_key_id_seq'::regclass) AS id", keyProperty = "id", before = false, resultType = Integer.class)
    @SelectKey(databaseId = "mysql", statement = "SELECT LAST_INSERT_ID() AS id", keyProperty = "id", before = false, resultType = Integer.class)
    int add(UserApiKey userApiKey);

    int update(UserApiKey userApiKey);

    int enable(@Param("id") int id);

    int disable(@Param("id") int id);

    int apiKey(@Param("id") int id, @Param("apiKey") String apiKey);

    int remark(@Param("id") int id, @Param("remark") String remark);

    int delete(@Param("id") int id);

    UserApiKey selectById(@Param("id") int id);

    UserApiKey selectByApiKey(@Param("apiKey") String apiKey);

    List<UserApiKey> selectAll();

    List<UserApiKey> getUserApiKeys();

    boolean isApiKeyExists(@Param("apiKey") String apiKey);
}
