package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.storager.dao.dto.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserMapper {

    int add(User user);

    int update(User user);

    int delete(int id);

    User select(@Param("username") String username, @Param("password") String password);

    User selectById(int id);

    User getUserByUsername(String username);

    List<User> selectAll();

    List<User> getUsers();

    int changePushKey(@Param("id") int id, @Param("pushKey") String pushKey);
}
