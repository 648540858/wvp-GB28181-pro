package com.genersoft.iot.vmp.service.redisMsg.service;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRpcPlayServiceImplTest {

    @Mock
    private RedisRpcConfig redisRpcConfig;

    @Mock
    private UserSetting userSetting;

    @InjectMocks
    private RedisRpcPlayServiceImpl service;

    @Test
    void stopProxyShouldSendRequestToOwningWvp() {
        RedisRpcResponse response = new RedisRpcResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        when(userSetting.getServerId()).thenReturn("wvp-local");
        when(userSetting.getPlayTimeout()).thenReturn(3000);
        when(redisRpcConfig.request(any(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(response);

        service.stopProxy("wvp-owner", 9);

        ArgumentCaptor<RedisRpcRequest> captor = ArgumentCaptor.forClass(RedisRpcRequest.class);
        verify(redisRpcConfig).request(captor.capture(), eq(3000L), eq(TimeUnit.SECONDS));
        assertEquals("wvp-owner", captor.getValue().getToId());
        assertEquals("streamProxy/stop", captor.getValue().getUri());
        assertEquals(9, captor.getValue().getParam());
    }

    @Test
    void stopProxyShouldFailWhenRemoteWvpDoesNotRespond() {
        when(userSetting.getServerId()).thenReturn("wvp-local");
        when(userSetting.getPlayTimeout()).thenReturn(3000);
        when(redisRpcConfig.request(any(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(null);

        assertThrows(ControllerException.class, () -> service.stopProxy("wvp-owner", 9));
    }
}
