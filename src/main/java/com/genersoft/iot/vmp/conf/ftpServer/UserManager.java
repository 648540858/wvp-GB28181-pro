package com.genersoft.iot.vmp.conf.ftpServer;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.springframework.stereotype.Component;

@Component
public class UserManager implements org.apache.ftpserver.ftplet.UserManager {

    @Override
    public User getUserByName(String username) throws FtpException {
        BaseUser use = new BaseUser();
        use.setName("admin");
        use.setPassword("admin123");
        use.setEnabled(true);
        use.setHomeDirectory("/home/lin");
        use.setMaxIdleTime(100);
        return use;
    }

    @Override
    public String[] getAllUserNames() throws FtpException {
        String[] strings = new String[1];
        strings[0] = "admin";
        return strings;
    }

    @Override
    public void delete(String username) throws FtpException {
    }

    @Override
    public void save(User user) throws FtpException {

    }

    @Override
    public boolean doesExist(String username) throws FtpException {
        return username.equals("admin");
    }

    @Override
    public User authenticate(Authentication authentication) throws AuthenticationFailedException {
        BaseUser use = new BaseUser();
        use.setName("admin");
        use.setPassword("admin123");
        use.setEnabled(true);
        use.setHomeDirectory("/home/lin");
        use.setMaxIdleTime(100);
        return use;
    }

    @Override
    public String getAdminName() throws FtpException {
        return "admin";
    }

    @Override
    public boolean isAdmin(String username) throws FtpException {
        return username.equals("admin");
    }
}
