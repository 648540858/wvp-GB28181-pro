package com.genersoft.iot.vmp.gat1400.framework.domain.vo;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginUserInfoVo {

    @JsonProperty("access_token")
    private String accessToken;
    private String username;
    private String nickname;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("user_dept")
    private String dept = "1197789917762031617";
    @JsonProperty("user_post")
    private List<String> post = new ArrayList<>();
    @JsonProperty("user_role")
    private List<JSONObject> roles = new ArrayList<>();

}
