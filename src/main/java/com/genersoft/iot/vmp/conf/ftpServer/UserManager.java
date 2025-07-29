package com.genersoft.iot.vmp.conf.ftpServer;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class UserManager implements org.apache.ftpserver.ftplet.UserManager {

    private static final String PREFIX = "VMP_FTP_USER_";

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;


    @Override
    public User getUserByName(String username) throws FtpException {
        return (BaseUser)redisTemplate.opsForValue().get(PREFIX + username);
    }

    @Override
    public String[] getAllUserNames() throws FtpException {
        return new String[0];
    }

    @Override
    public void delete(String username) throws FtpException {

    }

    @Override
    public void save(User user) throws FtpException {}

    @Override
    public boolean doesExist(String username) throws FtpException {
        return redisTemplate.opsForValue().get(PREFIX + username) != null;
    }

    @Override
    public User authenticate(Authentication authentication) throws AuthenticationFailedException {
        UsernamePasswordAuthentication usernamePasswordAuthentication = (UsernamePasswordAuthentication) authentication;
        BaseUser user = (BaseUser)redisTemplate.opsForValue().get(PREFIX + usernamePasswordAuthentication.getUsername());
        if (user != null && usernamePasswordAuthentication.getPassword().equals(user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    public String getAdminName() throws FtpException {
        return null;
    }

    @Override
    public boolean isAdmin(String username) throws FtpException {
        return false;
    }

    public BaseUser getRandomUser(){
        BaseUser use = new BaseUser();
        use.setName(RandomStringUtils.randomAlphabetic(6).toLowerCase());
        use.setPassword(RandomStringUtils.randomAlphabetic(6).toLowerCase());
        use.setEnabled(true);
        use.setHomeDirectory("/");
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new FtpAuthority());
        use.setAuthorities(authorities);
        String key = PREFIX + use.getName();

        // 随机用户信息十分钟自动失效
        Duration duration = Duration.ofMinutes(10);
        redisTemplate.opsForValue().set(key, use, duration);
        return use;
    }
}
