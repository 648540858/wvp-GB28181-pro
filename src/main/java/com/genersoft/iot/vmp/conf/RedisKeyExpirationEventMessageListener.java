package com.genersoft.iot.vmp.conf;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.StringUtils;

import java.util.Properties;

public class RedisKeyExpirationEventMessageListener extends KeyExpirationEventMessageListener {

    private UserSetup userSetup;
    private RedisMessageListenerContainer listenerContainer;
    private String keyspaceNotificationsConfigParameter = "EA";

    public RedisKeyExpirationEventMessageListener(RedisMessageListenerContainer listenerContainer, UserSetup userSetup) {
        super(listenerContainer);
        this.listenerContainer = listenerContainer;
        this.userSetup = userSetup;
    }

    @Override
    public void init() {
        if (!userSetup.getRedisConfig()) {
            // 配置springboot默认Config为空，即不让应用去修改redis的默认配置，因为Redis服务出于安全会禁用CONFIG命令给远程用户使用
            setKeyspaceNotificationsConfigParameter("");
        }else {

            RedisConnection connection = this.listenerContainer.getConnectionFactory().getConnection();
            Properties config = connection.getConfig("notify-keyspace-events");
            try {
                if (!config.getProperty("notify-keyspace-events").equals(keyspaceNotificationsConfigParameter)) {
                    connection.setConfig("notify-keyspace-events", keyspaceNotificationsConfigParameter);
                }
            } finally {
                connection.close();
            }
        }
        super.init();
    }
}
