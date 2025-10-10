package com.genersoft.iot.vmp.web.custom.conf;

import java.util.HashMap;
import java.util.Map;

public enum SyTokenManager {
    INSTANCE;

    /**
     * 普通用户 app Key 和 secret
     */
    public final Map<String, String> appMap = new HashMap<>();


    /**
     * 管理员专属token
     */
    public String adminToken;

    /**
     * sm4密钥
     */
    public String sm4Key;

    /**
     * 接口有效时长，单位分钟
     */
    public Long expires;


}
