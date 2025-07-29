package com.genersoft.iot.vmp.conf.ftpServer;

import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class UserManager implements org.apache.ftpserver.ftplet.UserManager {

    @Value("${ftp.username}")
    private String username;

    @Value("${ftp.password}")
    private String password;


    @Override
    public User getUserByName(String username) throws FtpException {
        System.out.println("getUserByName");
        if (!username.equals(this.username)) {
            return null;
        }
        return getUser();
    }

    @Override
    public String[] getAllUserNames() throws FtpException {
        String[] strings = new String[1];
        strings[0] = this.username;
        return strings;
    }

    @Override
    public void delete(String username) throws FtpException {}

    @Override
    public void save(User user) throws FtpException {}

    @Override
    public boolean doesExist(String username) throws FtpException {
        return this.username.equals(username);
    }

    @Override
    public User authenticate(Authentication authentication) throws AuthenticationFailedException {
        UsernamePasswordAuthentication usernamePasswordAuthentication = (UsernamePasswordAuthentication) authentication;
        if (usernamePasswordAuthentication.getUsername().equals(this.username)
                && usernamePasswordAuthentication.getPassword().equals(this.password)) {
            return getUser();
        }
        return null;
    }

    @NotNull
    private User getUser() {
        BaseUser use = new BaseUser();
        use.setName(this.username);
        use.setPassword(this.password);
        use.setEnabled(true);
        File file = new File("ftp");
        if (!file.exists()) {
            file.mkdirs();
        }else if (file.isFile()) {
            file.delete();
            file.mkdirs();
        }
        use.setHomeDirectory(file.getAbsolutePath());
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new FtpAuthority());
        use.setAuthorities(authorities);
        return use;
    }

    @Override
    public String getAdminName() throws FtpException {
        return this.username;
    }

    @Override
    public boolean isAdmin(String username) throws FtpException {
        return username.equals(this.username);
    }
}
