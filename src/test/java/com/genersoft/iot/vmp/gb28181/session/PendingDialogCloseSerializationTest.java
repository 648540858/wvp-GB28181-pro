package com.genersoft.iot.vmp.gb28181.session;

import com.alibaba.fastjson2.support.spring.data.redis.GenericFastJsonRedisSerializer;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.gb28181.bean.PendingDialogClose;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 使用现网Redis序列化器验证重启恢复所需的原始设备路由和对话身份。 */
class PendingDialogCloseSerializationTest {
    /** 保存再读取必须恢复具体类型、重试轮次、结束状态和双向Dialog标签。 */
    @Test
    void redisRoundTripPreservesOriginalDialogAndRoute() {
        SsrcTransaction transaction = new SsrcTransaction();
        transaction.setDeviceId("synthetic-device");
        transaction.setChannelId(17);
        transaction.setCallId("original-call");
        transaction.setType(InviteSessionType.DOWNLOAD);
        SipTransactionInfo sip = new SipTransactionInfo();
        sip.setCallId("original-call");
        sip.setFromTag("original-from");
        sip.setToTag("original-to");
        transaction.setSipTransactionInfo(sip);
        PendingDialogClose pending = new PendingDialogClose();
        pending.setTransaction(transaction);
        pending.setRemoteHost("192.0.2.1:5060");
        pending.setLocalIp("192.0.2.2");
        pending.setTransport("UDP");
        pending.setChannelDeviceId("synthetic-channel");
        pending.setAttemptId("attempt-2");
        pending.setAttempts(2);
        pending.setUpdatedAt(1790000000000L);
        pending.setState(PendingDialogClose.UNKNOWN);
        pending.setResponseCode(-1024);
        GenericFastJsonRedisSerializer serializer = new GenericFastJsonRedisSerializer();
        Object restored = serializer.deserialize(serializer.serialize(pending));
        assertInstanceOf(PendingDialogClose.class, restored);
        assertEquals(pending, restored);
        assertEquals("original-to", ((PendingDialogClose) restored).getTransaction().getSipTransactionInfo().getToTag());
    }
}
