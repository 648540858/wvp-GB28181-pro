package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.UserMapper;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
