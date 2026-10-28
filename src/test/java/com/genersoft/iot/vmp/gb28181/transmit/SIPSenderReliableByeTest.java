package com.genersoft.iot.vmp.gb28181.transmit;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.event.sip.SipEvent;
import gov.nist.javax.sip.SipProviderImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sip.ClientTransaction;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipStack;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 验证BYE使用真实客户端事务承接UDP重传，其他SIP消息保持原有发送方式。
@ExtendWith(MockitoExtension.class)
class SIPSenderReliableByeTest {
    private static final String LOCAL_IP = "127.0.0.1";
    private static final String KEY = "synthetic-bye-call7";

    @Mock
    private SipLayer sipLayer;
    @Mock
    private SipConfig sipConfig;
    @Mock
    private SipSubscribe sipSubscribe;
    @Mock
    private SipProviderImpl provider;
    @Mock
    private ClientTransaction transaction;
    @Mock
    private SipSubscribe.Event okEvent;
    @Mock
    private SipSubscribe.Event errorEvent;
    @InjectMocks
    private SIPSender sender;

    // 客户端事务持有原始请求，重传不另造Call-ID、CSeq或Via branch。
    @Test
    void udpByeUsesClientTransactionWithOriginalIdentity() throws Exception {
        Request bye = request(Request.BYE, "UDP");
        when(sipLayer.getUdpSipProvider(LOCAL_IP)).thenReturn(provider);
        when(provider.getNewClientTransaction(bye)).thenReturn(transaction);

        sender.transmitRequest(LOCAL_IP, bye);

        verify(transaction).sendRequest();
        verify(provider, never()).sendRequest(bye);
        assertEquals("synthetic-bye-call", ((CallIdHeader) bye.getHeader(CallIdHeader.NAME)).getCallId());
        assertEquals(7, ((CSeqHeader) bye.getHeader(CSeqHeader.NAME)).getSeqNumber());
        assertEquals("z9hG4bK-synthetic-bye", ((ViaHeader) bye.getHeader(ViaHeader.NAME)).getBranch());
    }

    // TCP信令同样以BYE事务观察确认，但不改动其他消息的发送行为。
    @Test
    void tcpByeUsesClientTransaction() throws Exception {
        Request bye = request(Request.BYE, "TCP");
        when(sipLayer.getTcpSipProvider(LOCAL_IP)).thenReturn(provider);
        when(provider.getNewClientTransaction(bye)).thenReturn(transaction);

        sender.transmitRequest(LOCAL_IP, bye);

        verify(transaction).sendRequest();
        verify(provider, never()).sendRequest(bye);
    }

    // 非BYE消息不因本次修复切换成有状态发送，避免扩大现有SIP事务行为。
    @Test
    void messageRetainsStatelessSend() throws Exception {
        Request message = request(Request.MESSAGE, "UDP");
        when(sipLayer.getUdpSipProvider(LOCAL_IP)).thenReturn(provider);

        sender.transmitRequest(LOCAL_IP, message);

        verify(provider).sendRequest(message);
        verify(provider, never()).getNewClientTransaction(message);
    }

    // BYE没有UDP监听器必须向上报错，防止调用方把未发送误当确认成功。
    @Test
    void missingUdpProviderFailsBye() throws Exception {
        Request bye = request(Request.BYE, "UDP");

        assertThrows(SipException.class, () -> sender.transmitRequest(LOCAL_IP, bye));
    }

    // BYE没有TCP监听器也不能静默返回。
    @Test
    void missingTcpProviderFailsBye() throws Exception {
        Request bye = request(Request.BYE, "TCP");

        assertThrows(SipException.class, () -> sender.transmitRequest(LOCAL_IP, bye));
    }

    // 默认一秒应用超时不能提前清理BYE，等待窗口必须覆盖默认Timer F约32秒。
    @Test
    void byeSubscriptionWaitsBeyondDefaultTimerF() throws Exception {
        Request bye = request(Request.BYE, "UDP");
        when(sipLayer.getUdpSipProvider(LOCAL_IP)).thenReturn(provider);
        when(provider.getNewClientTransaction(bye)).thenReturn(transaction);
        when(sipConfig.getTimeout()).thenReturn(1000L);

        sender.transmitRequest(LOCAL_IP, bye, errorEvent, okEvent);

        ArgumentCaptor<SipEvent> event = ArgumentCaptor.forClass(SipEvent.class);
        verify(sipSubscribe).addSubscribe(eq(KEY), event.capture());
        assertTrue(event.getValue().getDelay(TimeUnit.MILLISECONDS) >= 34000,
                "BYE应用订阅不能在JAIN默认Timer F结束前删除");
    }

    // 显式更短的调用超时同样不能缩短BYE协议重传窗口。
    @Test
    void explicitShortByeTimeoutCannotPreemptRetransmission() throws Exception {
        Request bye = request(Request.BYE, "UDP");
        when(sipLayer.getUdpSipProvider(LOCAL_IP)).thenReturn(provider);
        when(provider.getNewClientTransaction(bye)).thenReturn(transaction);

        sender.transmitRequest(LOCAL_IP, bye, errorEvent, okEvent, 100L);

        ArgumentCaptor<SipEvent> event = ArgumentCaptor.forClass(SipEvent.class);
        verify(sipSubscribe).addSubscribe(eq(KEY), event.capture());
        assertTrue(event.getValue().getDelay(TimeUnit.MILLISECONDS) >= 34000);
    }

    // 显式更长超时仍被保留，部署可按自定义T1延长等待而不受35秒下限截短。
    @Test
    void explicitLongByeTimeoutIsPreserved() throws Exception {
        Request bye = request(Request.BYE, "UDP");
        when(sipLayer.getUdpSipProvider(LOCAL_IP)).thenReturn(provider);
        when(provider.getNewClientTransaction(bye)).thenReturn(transaction);

        sender.transmitRequest(LOCAL_IP, bye, errorEvent, okEvent, 40000L);

        ArgumentCaptor<SipEvent> event = ArgumentCaptor.forClass(SipEvent.class);
        verify(sipSubscribe).addSubscribe(eq(KEY), event.capture());
        assertTrue(event.getValue().getDelay(TimeUnit.MILLISECONDS) >= 39000);
    }

    // 非BYE消息的应用订阅仍使用既有配置，不被35秒BYE下限扩张。
    @Test
    void messageSubscriptionRetainsConfiguredTimeout() throws Exception {
        Request message = request(Request.MESSAGE, "UDP");
        when(sipLayer.getUdpSipProvider(LOCAL_IP)).thenReturn(provider);
        when(sipConfig.getTimeout()).thenReturn(1000L);

        sender.transmitRequest(LOCAL_IP, message, errorEvent, okEvent);

        ArgumentCaptor<SipEvent> event = ArgumentCaptor.forClass(SipEvent.class);
        verify(sipSubscribe).addSubscribe(eq(KEY), event.capture());
        assertTrue(event.getValue().getDelay(TimeUnit.MILLISECONDS) <= 1000);
        verify(provider).sendRequest(message);
    }

    // 缺失监听器报错时也须撤销已创建的BYE订阅，避免旧超时重复通知上层。
    @Test
    void missingByeProviderRemovesCreatedSubscription() throws Exception {
        Request bye = request(Request.BYE, "UDP");
        when(sipConfig.getTimeout()).thenReturn(1000L);

        assertThrows(SipException.class, () -> sender.transmitRequest(LOCAL_IP, bye, errorEvent, okEvent));

        verify(sipSubscribe).removeSubscribe(KEY);
    }

    // 创建BYE事务失败后移除本次响应订阅并向上传播，保留重试所需的上层会话。
    @Test
    void transactionCreationFailureRemovesSubscription() throws Exception {
        Request bye = request(Request.BYE, "UDP");
        when(sipLayer.getUdpSipProvider(LOCAL_IP)).thenReturn(provider);
        when(sipConfig.getTimeout()).thenReturn(1000L);
        when(provider.getNewClientTransaction(bye)).thenThrow(new TransactionUnavailableException("synthetic failure"));

        assertThrows(SipException.class, () -> sender.transmitRequest(LOCAL_IP, bye, errorEvent, okEvent));

        verify(sipSubscribe).removeSubscribe(KEY);
        verify(provider, never()).sendRequest(bye);
    }

    // 真实回环UDP对端丢弃首包，证明JAIN自动重传同一BYE并能收到200，无需接触设备。
    @Test
    void realUdpTransactionRetransmitsDroppedFirstByeWithoutChangingIdentity() throws Exception {
        InetAddress loopback = InetAddress.getByName(LOCAL_IP);
        int localPort;
        try (DatagramSocket reservation = new DatagramSocket(0, loopback)) {
            localPort = reservation.getLocalPort();
        }
        SipFactory factory = SipFactory.getInstance();
        factory.setPathName("gov.nist");
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "synthetic-bye-loss-" + UUID.randomUUID());
        properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "off");
        properties.setProperty("gov.nist.javax.sip.AUTOMATIC_DIALOG_ERROR_HANDLING", "false");
        properties.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER", "true");
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "0");
        SipStack stack = factory.createSipStack(properties);
        try (DatagramSocket peer = new DatagramSocket(0, loopback)) {
            peer.setSoTimeout(5000);
            SipProviderImpl actualProvider = (SipProviderImpl) stack.createSipProvider(
                    stack.createListeningPoint(LOCAL_IP, localPort, "UDP"));
            actualProvider.setDialogErrorsAutomaticallyHandled();
            SipListener listener = mock(SipListener.class);
            CompletableFuture<ResponseEvent> confirmed = new CompletableFuture<>();
            doAnswer(invocation -> {
                confirmed.complete(invocation.getArgument(0));
                return null;
            }).when(listener).processResponse(any(ResponseEvent.class));
            actualProvider.addSipListener(listener);
            stack.start();
            Request bye = request(Request.BYE, "UDP");
            ((SipURI) bye.getRequestURI()).setPort(peer.getLocalPort());
            ((ViaHeader) bye.getHeader(ViaHeader.NAME)).setPort(localPort);
            when(sipLayer.getUdpSipProvider(LOCAL_IP)).thenReturn(actualProvider);
            CompletableFuture<byte[][]> packets = CompletableFuture.supplyAsync(() -> {
                try {
                    byte[][] received = new byte[2][];
                    DatagramPacket packet = new DatagramPacket(new byte[8192], 8192);
                    for (int index = 0; index < received.length; index++) {
                        packet.setLength(8192);
                        peer.receive(packet);
                        received[index] = java.util.Arrays.copyOfRange(packet.getData(), packet.getOffset(),
                                packet.getOffset() + packet.getLength());
                    }
                    // 故意只回应第二包，第一包没有任何响应或人工补发。
                    Request retransmission = factory.createMessageFactory().createRequest(
                            new String(received[1], StandardCharsets.UTF_8));
                    byte[] response = factory.createMessageFactory().createResponse(Response.OK, retransmission)
                            .toString().getBytes(StandardCharsets.UTF_8);
                    peer.send(new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort()));
                    return received;
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });

            sender.transmitRequest(LOCAL_IP, bye);

            byte[][] received = packets.get(6, TimeUnit.SECONDS);
            assertArrayEquals(received[0], received[1], "自动重传必须复用原包的Call-ID、CSeq和Via branch");
            assertEquals(Response.OK, confirmed.get(6, TimeUnit.SECONDS).getResponse().getStatusCode());
        } finally {
            stack.stop();
        }
    }

    // 构造仅使用回环地址的合法请求，测试不启动SIP监听器或接触设备。
    private Request request(String method, String transport) throws Exception {
        SipFactory factory = SipFactory.getInstance();
        factory.setPathName("gov.nist");
        return factory.createMessageFactory().createRequest(method + " sip:synthetic-channel@127.0.0.1:65060 SIP/2.0\r\n"
                + "Via: SIP/2.0/" + transport + " 127.0.0.1:15060;branch=z9hG4bK-synthetic-bye\r\n"
                + "From: <sip:synthetic-platform@127.0.0.1>;tag=synthetic-from\r\n"
                + "To: <sip:synthetic-channel@127.0.0.1>;tag=synthetic-to\r\n"
                + "Call-ID: synthetic-bye-call\r\nCSeq: 7 " + method + "\r\nMax-Forwards: 70\r\n"
                + "User-Agent: synthetic-tests\r\nContent-Length: 0\r\n\r\n");
    }
}
