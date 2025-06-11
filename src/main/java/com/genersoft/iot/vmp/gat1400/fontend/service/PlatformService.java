package com.genersoft.iot.vmp.gat1400.fontend.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;

import cz.data.viid.fe.domain.AdminLoginDto;
import cz.data.viid.framework.CacheService;
import cz.data.viid.framework.SpringContextHolder;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.core.LoginUser;
import cz.data.viid.framework.domain.entity.APEDevice;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.domain.vo.LoginUserInfoVo;
import cz.data.viid.framework.mapper.MetricsMapper;
import cz.data.viid.framework.service.APEDeviceService;
import cz.data.viid.framework.service.VIIDServerService;
import cz.data.viid.utils.SecurityUtil;

@Component
public class PlatformService implements InitializingBean {
    private final JwsHeader jwsHeader = JwsHeader.with(SignatureAlgorithm.RS256).build();
    private JSONObject routes;

    @Resource
    JwtEncoder jwtEncoder;
    @Resource
    APEDeviceService apeDeviceService;
    @Autowired
    CacheService cacheService;

    @Override
    public void afterPropertiesSet() throws Exception {
        ClassPathResource resource = new ClassPathResource("route.json");
        InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        String data = FileCopyUtils.copyToString(reader);
        routes = JSONObject.parseObject(data);
    }

    public LoginUserInfoVo login(AdminLoginDto request) {
        VIIDServerService service = SpringContextHolder.getBean(VIIDServerService.class);
        VIIDServer server = service.getCurrentServer();
        if (StringUtils.equals(request.getUsername(), server.getUsername())
                && StringUtils.equals(request.getPassword(), server.getAuthenticate())) {
            String jti = UUID.randomUUID().toString();
            Jwt jwt = jwtEncoder.encode(jwtEncoderParameters(jti, server.getServerName()));
            LoginUser loginUser = new LoginUser();
            loginUser.setJti(jti);
            loginUser.setUserId(server.getServerId());
            loginUser.setUsername(server.getServerName());
            cacheService.setLoginUser(jti, loginUser);
            LoginUserInfoVo userInfo = new LoginUserInfoVo();
            userInfo.setUserId(server.getServerId());
            userInfo.setUsername(server.getServerName());
            userInfo.setNickname(server.getServerName());
            userInfo.setAccessToken(jwt.getTokenValue());
            return userInfo;
        }
        return null;
    }

    public void logout() {
        LoginUser loginUser = SecurityUtil.requireDataUser();
        String jti = loginUser.getJti();
        cacheService.invalidate(jti);
    }

    public JSONObject metrics() {
        VIIDServerService service = SpringContextHolder.getBean(VIIDServerService.class);
        long serverCount = service.count(
                new QueryWrapper<VIIDServer>().lambda().eq(VIIDServer::getOnline, Constants.DeviceStatus.Offline.getValue())
                        .and(wrapper -> wrapper.ne(VIIDServer::getCategory, Constants.InstanceCategory.THIS.getValue()))
        );
        long serverOnline = service.count(
                new QueryWrapper<VIIDServer>().lambda().eq(VIIDServer::getOnline, Constants.DeviceStatus.Online.getValue())
                        .and(wrapper -> wrapper.ne(VIIDServer::getCategory, Constants.InstanceCategory.THIS.getValue()))
        );
        long deviceCount = apeDeviceService.count(new QueryWrapper<APEDevice>().lambda().eq(APEDevice::getIsOnline, Constants.DeviceStatus.Offline.getValue()));
        long deviceOnline = apeDeviceService.count(new QueryWrapper<APEDevice>().lambda().eq(APEDevice::getIsOnline, Constants.DeviceStatus.Online.getValue()));
        JSONArray server = new JSONArray();
        server.add(new JSONObject().fluentPut("name", "在线").fluentPut("value", serverOnline));
        server.add(new JSONObject().fluentPut("name", "离线").fluentPut("value", serverCount));
        JSONArray device = new JSONArray();
        device.add(new JSONObject().fluentPut("name", "在线").fluentPut("value", deviceOnline));
        device.add(new JSONObject().fluentPut("name", "离线").fluentPut("value", deviceCount));
        Pair<List<String>, List<Long>> pair = SpringContextHolder.getBean(MetricsMapper.class).toDayHistogramOfHour();
        JSONObject metric = new JSONObject();
        metric.put("keys", pair.getKey());
        metric.put("values", pair.getValue());
        JSONObject data = new JSONObject();
        data.put("server", server);
        data.put("device", device);
        data.put("metric", metric);
        return data;
    }

    public JSONObject getRoutes() {
        return routes;
    }


    private JwtEncoderParameters jwtEncoderParameters(String jti, String username) {
        return JwtEncoderParameters.from(jwsHeader, claimsBuilder(jti, username));
    }

    private JwtClaimsSet claimsBuilder(String jti, String username) {
        return JwtClaimsSet.builder()
                .id(jti)
                .subject(username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .build();
    }
}
