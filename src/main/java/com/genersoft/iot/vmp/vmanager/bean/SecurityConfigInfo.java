package com.genersoft.iot.vmp.vmanager.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SecurityConfigInfo {

    private Boolean interfaceAuthentication;

    private List<String> interfaceAuthenticationExcludes = new ArrayList<>();

    private Long loginTimeoutMinutes;

    private Long jwtExpirationMinutes;

    private String accessTokenHeader;

    private String apiKeyHeader;

    private String passwordEncoder;

    private String sessionCreationPolicy;

    private Boolean csrfEnabled;

    private Boolean contentTypeOptionsEnabled;

    private List<String> allowedOrigins = new ArrayList<>();

    private Boolean allowAllOrigins;

    private Boolean docEnabled;

    private Boolean httpsEnabled;

    private Integer serverPort;

    private String jwkFile;

    private Boolean sipCacheServerConnections;

    private String jsonLibrary;

    private String jsonLibraryVersion;

    private Boolean fastjson1Present;

    private String fastjsonRiskStatus;

    private List<String> warnings = new ArrayList<>();
}
