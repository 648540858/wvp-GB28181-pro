package com.genersoft.iot.vmp.gat1400.framework;

import com.genersoft.iot.vmp.gat1400.framework.domain.core.LoginUser;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.stereotype.Component;

import java.time.Duration;


@Component
public class CacheService {

    private final Cache<String, LoginUser> userCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(20))
            .build();

    public void setLoginUser(String key, LoginUser value) {
        userCache.put(key, value);
    }

    public LoginUser getLoginUser(String token) {
        return userCache.getIfPresent(token);
    }

    public void invalidate(String key) {
        userCache.invalidate(key);
    }
}
