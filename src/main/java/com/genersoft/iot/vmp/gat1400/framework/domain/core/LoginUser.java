package com.genersoft.iot.vmp.gat1400.framework.domain.core;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LoginUser {
    private static final long serialVersionUID = 571L;

    private String jti;
    private String userId;
    private String username;
    private List<String> roles;


}
