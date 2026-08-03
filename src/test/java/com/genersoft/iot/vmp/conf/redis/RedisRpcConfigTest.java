package com.genersoft.iot.vmp.conf.redis;

import com.genersoft.iot.vmp.conf.UserSetting;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class RedisRpcConfigTest {

    @Test
    void usesApplicationTaskExecutorWhenMultipleExecutorsExist() {
        TaskExecutor taskScheduler = mock(TaskExecutor.class);
        TaskExecutor applicationTaskExecutor = mock(TaskExecutor.class);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("taskScheduler", TaskExecutor.class, () -> taskScheduler);
            context.registerBean("applicationTaskExecutor", TaskExecutor.class, () -> applicationTaskExecutor);
            context.registerBean(UserSetting.class, () -> mock(UserSetting.class));
            context.registerBean("redisTemplate", RedisTemplate.class, () -> mock(RedisTemplate.class));
            context.register(RedisRpcConfig.class);
            context.refresh();

            RedisRpcConfig redisRpcConfig = context.getBean(RedisRpcConfig.class);
            assertSame(applicationTaskExecutor, ReflectionTestUtils.getField(redisRpcConfig, "taskExecutor"));
        }
    }
}
