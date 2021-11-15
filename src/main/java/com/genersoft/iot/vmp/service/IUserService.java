package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.storager.dao.dto.User;

import java.util.List;

public interface IUserService {

    User getUser(String username, String password);

    boolean changePassword(int id, String password);

    User getUserByUsername(String username);

    int addUser(User user);

    int deleteUser(int id);

    List<User> getAllUsers();

    int updateUsers(User user);
}
