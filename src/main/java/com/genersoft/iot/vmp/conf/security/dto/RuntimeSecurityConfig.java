package com.genersoft.iot.vmp.conf.security.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RuntimeSecurityConfig {

    private Boolean interfaceAuthentication;

    private List<String> interfaceAuthenticationExcludes = new ArrayList<>();

    private Long loginTimeoutMinutes;

    private List<String> allowedOrigins = new ArrayList<>();

    private Boolean docEnabled;

    private Boolean contentTypeOptionsEnabled;
}
