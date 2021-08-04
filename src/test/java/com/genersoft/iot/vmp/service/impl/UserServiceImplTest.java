package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;


@SpringBootTest
@RunWith(SpringRunner.class)
class UserServiceImplTest {

    @Resource
    private IUserService userService;

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @org.junit.jupiter.api.Test
    void getAllUser() {
        System.out.println(userService.getAllUsers().size());
    }


    @org.junit.jupiter.api.Test
    void add() {
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setUsername("admin_" + i);
            user.setPassword("admin_password_" + i);
            user.setRoleId((int)(Math.random()*4 + 1));
            user.setCreateTime(format.format(System.currentTimeMillis()));
            user.setUpdateTime(format.format(System.currentTimeMillis()));
            userService.addUser(user);
        }
    }

    @org.junit.jupiter.api.Test
    void delete() {
        userService.deleteUser(1002);
    }

    @org.junit.jupiter.api.Test
    void update() {
        User user = new User();
        user.setId(1003);
        user.setUsername("update" );
        user.setPassword("update");
        user.setRoleId((int)(Math.random()*4 + 1));
        user.setUpdateTime(format.format(System.currentTimeMillis()));
        userService.updateUsers(user);
    }


}