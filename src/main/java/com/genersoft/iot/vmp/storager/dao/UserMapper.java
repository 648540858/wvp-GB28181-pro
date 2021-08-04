package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.storager.dao.dto.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserMapper {

    @Insert("INSERT INTO user (username, password, roleId, create_time, update_time) VALUES" +
            "('${username}', '${password}', '${roleId}', '${createTime}', '${updateTime}')")
    int add(User user);

    @Update(value = {" <script>" +
            "UPDATE user " +
            "SET update_time='${updateTime}' " +
            "<if test=\"roleId != null\">, roleId='${roleId}'</if>" +
            "<if test=\"password != null\">, password='${password}'</if>" +
            "<if test=\"username != null\">, username='${username}'</if>" +
            "WHERE id=#{id}" +
            " </script>"})
    int update(User user);

    @Delete("DELETE FROM user WHERE id=#{id}")
    int delete(int id);

    @Select("select * FROM user WHERE username=#{username} AND password=#{password}")
    User select(String username, String password);

    @Select("select * FROM user WHERE id=#{id}")
    User selectById(int id);

    @Select("select * FROM user WHERE username=#{username}")
    User getUserByUsername(String username);

    @Select("select * FROM user")
    List<User> selectAll();
}
