package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.List;


@SpringBootTest

class UserServiceImplTest {

    @Resource
    private IUserService userService;

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @org.junit.jupiter.api.Test
    void getAllUser() {
        List<User> allUsers = userService.getAllUsers();
        System.out.println(userService.getAllUsers().size());
        User admin = userService.getUser("admin", "21232f297a57a5a743894a0e4a801fc3");
        User admin1 = userService.getUserByUsername("admin");
        System.out.println(12);
    }


    @org.junit.jupiter.api.Test
    void add() {
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setUsername("admin_" + i);
            user.setPassword("admin_password_" + i);

            Role role = new Role();
            role.setId(1);
            user.setRole(role);
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
        user.setId(11);
        user.setUsername("update" );
        user.setPassword("update");
        Role role = new Role();
        role.setId(2);
        user.setRole(role);
        user.setUpdateTime(format.format(System.currentTimeMillis()));
        userService.updateUsers(user);
    }


}