package com.genersoft.iot.vmp.conf;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Properties;

public class RedisKeyExpirationEventMessageListener extends KeyExpirationEventMessageListener {

    private UserSetting userSetting;
    private RedisMessageListenerContainer listenerContainer;
    private String keyspaceNotificationsConfigParameter = "EA";

    public RedisKeyExpirationEventMessageListener(RedisMessageListenerContainer listenerContainer, UserSetting userSetting) {
        super(listenerContainer);
        this.listenerContainer = listenerContainer;
        this.userSetting = userSetting;
    }

    @Override
    public void init() {
        if (!userSetting.getRedisConfig()) {
            // 配置springboot默认Config为空，即不让应用去修改redis的默认配置，因为Redis服务出于安全会禁用CONFIG命令给远程用户使用
            setKeyspaceNotificationsConfigParameter("");
        }else {

            RedisConnection connection = this.listenerContainer.getConnectionFactory().getConnection();
            Properties config = connection.getConfig("notify-keyspace-events");
            try {
                if (!keyspaceNotificationsConfigParameter.equals(config.getProperty("notify-keyspace-events"))) {
                    connection.setConfig("notify-keyspace-events", keyspaceNotificationsConfigParameter);
                }
            } finally {
                connection.close();
            }
        }
        super.init();
    }
}
