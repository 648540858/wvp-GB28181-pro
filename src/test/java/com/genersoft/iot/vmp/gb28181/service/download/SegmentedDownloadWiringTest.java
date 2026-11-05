package com.genersoft.iot.vmp.gb28181.service.download;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.service.impl.PlayServiceImpl;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 用真实Spring注入验证新增编排不会阻止原播放服务启动，外部控制依赖保持隔离。 */
class SegmentedDownloadWiringTest {

    /** 默认关闭时可初始化全部新增组件和原生适配器，不建立Redis连接或创建媒体资源。 */
    @Test
    void disabledFeatureWiresWithoutCircularDependencyOrSideEffects() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            UserSetting setting = mock(UserSetting.class);
            when(setting.getServerId()).thenReturn("synthetic-wiring-server");
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            context.getBeanFactory().registerSingleton("syntheticUserSetting", setting);
            context.getBeanFactory().registerSingleton("syntheticStringRedisTemplate", redis);
            Set<Class<?>> registered = new HashSet<>(Set.of(UserSetting.class));
            for (java.lang.reflect.Field field : PlayServiceImpl.class.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class) && field.getType() != ObjectProvider.class
                        && registered.add(field.getType())) {
                    registerMock(context, field.getType());
                }
            }
            context.register(PlayServiceImpl.class, SegmentedDownloadSettings.class, SegmentedDownloadStore.class,
                    SegmentedDownloadService.class, SegmentedArtifactFiles.class, SegmentedRecordingAssembler.class);
            context.refresh();
            assertSame(context.getBean(IPlayService.class), context.getBean(NativeDownloadGateway.class));
            assertFalse(context.getBean(SegmentedDownloadService.class).isEnabled("synthetic-other-device"));
            verifyNoInteractions(redis, context.getBean(IReceiveRtpServerService.class));
        }
    }

    /** 只替换已有播放服务的外部依赖，保留新增组件的真实构造和自动注入过程。 */
    private static <T> void registerMock(AnnotationConfigApplicationContext context, Class<T> type) {
        context.getBeanFactory().registerSingleton("synthetic-" + type.getName(), mock(type));
    }
}
