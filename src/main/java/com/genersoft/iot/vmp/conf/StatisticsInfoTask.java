package com.genersoft.iot.vmp.conf;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.StatisticsInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.GitUtil;
import com.genersoft.iot.vmp.utils.SystemInfoUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.sql.DatabaseMetaData;
import java.util.Objects;

@Component
@Order(value=100)
@Slf4j
public class StatisticsInfoTask implements CommandLineRunner {

    @Autowired
    private GitUtil gitUtil;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try {
            StatisticsInfo statisticsInfo = new StatisticsInfo();
            statisticsInfo.setDeviceId(SystemInfoUtils.getHardwareId());
            statisticsInfo.setBranch(gitUtil.getBranch());
            statisticsInfo.setGitCommitId(gitUtil.getGitCommitId());
            statisticsInfo.setGitUrl(gitUtil.getGitUrl());
            statisticsInfo.setVersion(gitUtil.getBuildVersion());

            statisticsInfo.setOsName(System.getProperty("os.name"));
            statisticsInfo.setArch(System.getProperty("os.arch"));
            statisticsInfo.setJdkVersion(System.getProperty("java.version"));

            statisticsInfo.setDocker(new File("/.dockerenv").exists());
            try {
                statisticsInfo.setRedisVersion(getRedisVersion());
            }catch (Exception ignored) {}
            try {
                DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
                statisticsInfo.setSqlVersion(metaData.getDatabaseProductVersion());
                statisticsInfo.setSqlType(metaData.getDriverName());
            }catch (Exception ignored) {}
            statisticsInfo.setTime(DateUtil.getNow());
            sendPost(statisticsInfo);


        }catch (Exception e) {
            log.error("[获取信息失败] ", e);
        }
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

    public void sendPost(StatisticsInfo statisticsInfo) {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        OkHttpClient client = httpClientBuilder.build();

        RequestBody requestBodyJson = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(statisticsInfo));

        Request request = new Request.Builder()
                .post(requestBodyJson)
                .url("http://api.wvp-pro.cn:136/api/statistics/ping")
//                .url("http://127.0.0.1:11236/api/statistics/ping")
                .addHeader("Content-Type", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            response.close();
            Objects.requireNonNull(response.body()).close();

        }catch (Exception ignored){}
    }
}
