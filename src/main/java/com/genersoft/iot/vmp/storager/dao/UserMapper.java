package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface UserMapper {

    @Insert("INSERT INTO user (username, password, roleId, create_time) VALUES" +
            "('${username}', '${password}', '${roleId}', datetime('now','localtime'))")
    int add(User user);

    @Update("UPDATE user " +
            "SET username=#{username}," +
            "password=#{password}," +
            "roleId=#{roleId}" +
            "WHERE id=#{id}")
    int update(User user);

    @Delete("DELETE FROM user WHERE app=#{app} AND id=#{id}")
    int delete(User user);

    @Select("select * FROM user WHERE username= #{username} AND password=#{password}")
    User select(String username, String password);

    @Select("select * FROM user WHERE id= #{id}")
    User selectById(int id);
}
