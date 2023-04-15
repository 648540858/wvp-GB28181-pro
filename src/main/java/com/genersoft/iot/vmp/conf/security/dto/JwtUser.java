package com.genersoft.iot.vmp.conf.security.dto;

public class JwtUser {

    public enum TokenStatus{
        /**
         * 正常的使用状态
         */
        NORMAL,
        /**
         * 过期而失效
         */
        EXPIRED,
        /**
         * 即将过期
         */
        EXPIRING_SOON,
        /**
         * 异常
         */
        EXCEPTION
    }

    private String userName;

    private String password;

    private int roleId;

    private TokenStatus status;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public TokenStatus getStatus() {
        return status;
    }

    public void setStatus(TokenStatus status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }
}
