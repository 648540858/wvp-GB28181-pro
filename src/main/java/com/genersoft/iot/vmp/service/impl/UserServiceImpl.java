package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.UserMapper;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User getUser(String username, String password) {
        return userMapper.select(username, password);
    }

    @Override
    public boolean changePassword(int id, String password) {
        User user = userMapper.selectById(id);
        user.setPassword(password);
        return userMapper.update(user) > 0;
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.getUserByUsername(username);
    }

    @Override
    public int addUser(User user) {
        User userByUsername = userMapper.getUserByUsername(user.getUsername());
        if (userByUsername != null) {
            return 0;
        }
        return userMapper.add(user);
    }
    @Override
    public int deleteUser(int id) {
        return userMapper.delete(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }

    @Override
    public int updateUsers(User user) {
        return userMapper.update(user);
    }


    @Override
    public boolean checkPushAuthority(String callId, String sign) {
        if (StringUtils.isEmpty(callId)) {
            return userMapper.checkPushAuthorityByCallId(sign).size() > 0;
        }else {
            return userMapper.checkPushAuthorityByCallIdAndSign(callId, sign).size() > 0;
        }
    }

    @Override
    public PageInfo<User> getUsers(int page, int count) {
        PageHelper.startPage(page, count);
        List<User> users = userMapper.getUsers();
        return new PageInfo<>(users);
    }

    @Override
    public int changePushKey(int id, String pushKey) {
        return userMapper.changePushKey(id,pushKey);
    }
}
