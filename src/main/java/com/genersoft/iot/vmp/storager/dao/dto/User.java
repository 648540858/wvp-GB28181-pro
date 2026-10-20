package com.genersoft.iot.vmp.storager.dao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class User {

    private int id;
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String createTime;
    private String updateTime;
    private String pushKey;
    private Role role;
    /**
     * 是否使用默认密码（首次登录需强制修改密码）
     */
    private boolean defaultPassword = false;
}
