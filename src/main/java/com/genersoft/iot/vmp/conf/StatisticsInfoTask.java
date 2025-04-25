package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.common.StatisticsInfo;
import com.genersoft.iot.vmp.utils.GitUtil;
import com.genersoft.iot.vmp.utils.SystemInfoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Order(value=100)
public class StatisticsInfoTask implements CommandLineRunner {

    @Autowired
    private GitUtil gitUtil;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        StatisticsInfo statisticsInfo = new StatisticsInfo();
        statisticsInfo.setDeviceId(SystemInfoUtils.getHardwareId());
        statisticsInfo.setBranch(gitUtil.getBranch());
        statisticsInfo.setGitCommitId(gitUtil.getGitCommitId());
        statisticsInfo.setGitUrl(gitUtil.getGitUrl());

        statisticsInfo.setOsName(System.getProperty("os.name"));
        statisticsInfo.setArch(System.getProperty("os.arch"));
        statisticsInfo.setJdkVersion(System.getProperty("java.version"));

        statisticsInfo.setDocker(new File("/.dockerenv").exists());

        statisticsInfo.setRedisVersion(getRedisVersion());

//        statisticsInfo.setSqlVersion();
    }

    public String getRedisVersion() {
        if (redisTemplate.getConnectionFactory() == null) {
            return null;
        }
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
        if (connection.info() == null) {
            return null;
        }
        return connection.info().getProperty("redis_version");
    }
}
