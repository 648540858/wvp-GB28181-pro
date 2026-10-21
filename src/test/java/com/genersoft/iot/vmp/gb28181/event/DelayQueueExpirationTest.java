package com.genersoft.iot.vmp.gb28181.event;

import com.genersoft.iot.vmp.gb28181.event.sip.MessageEvent;
import com.genersoft.iot.vmp.gb28181.event.sip.SipEvent;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.SubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.SubscribeTaskRunner;
import com.genersoft.iot.vmp.gb28181.task.platformStatus.PlatformStatusTaskRunner;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd.PresetQueryResponseMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.VectorTileCatch;
import com.genersoft.iot.vmp.jt1078.session.FtpDownloadManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DelayQueueExpirationTest {

    @Test
    void sipSubscribe_shouldNotBlockWhenEntryIsRemovedAfterQueueCheck() throws Exception {
        SipSubscribe subscribe = new SipSubscribe();
        DelayQueue<SipEvent> queue = queueWithRemovalRace();
        ReflectionTestUtils.setField(subscribe, "delayQueue", queue);

        assertDoesNotThrow(subscribe::execute);
        assertPollInsteadOfBlocking(queue);
    }

    @Test
    void messageSubscribe_shouldNotBlockWhenEntryIsRemovedAfterQueueCheck() throws Exception {
        MessageSubscribe subscribe = new MessageSubscribe();
        DelayQueue<MessageEvent<?>> queue = queueWithRemovalRace();
        ReflectionTestUtils.setField(subscribe, "delayQueue", queue);

        assertDoesNotThrow(subscribe::execute);
        assertPollInsteadOfBlocking(queue);
    }

    @Test
    void subscribeTaskRunner_shouldNotBlockWhenEntryIsRemovedAfterQueueCheck() throws Exception {
        SubscribeTaskRunner runner = new SubscribeTaskRunner();
        DelayQueue<SubscribeTask> queue = queueWithRemovalRace();
        ReflectionTestUtils.setField(runner, "delayQueue", queue);

        assertDoesNotThrow(runner::expirationCheck);
        assertPollInsteadOfBlocking(queue);
    }

    @Test
    void vectorTileCleanup_shouldNotBlockWhenEntryIsRemovedAfterQueueCheck() throws Exception {
        VectorTileCatch cache = new VectorTileCatch();
        DelayQueue<Delayed> queue = queueWithRemovalRace();
        ReflectionTestUtils.setField(cache, "delayQueue", queue);

        assertDoesNotThrow(cache::expirationCheck);
        assertPollInsteadOfBlocking(queue);
    }

    @Test
    void presetQueryCleanup_shouldNotBlockWhenEntryIsRemovedAfterQueueCheck() throws Exception {
        PresetQueryResponseMessageHandler handler = new PresetQueryResponseMessageHandler();
        DelayQueue<Delayed> queue = queueWithRemovalRace();
        ReflectionTestUtils.setField(handler, "delayQueue", queue);

        assertDoesNotThrow(handler::expirationCheck);
        assertPollInsteadOfBlocking(queue);
    }

    @Test
    void platformRegisterCleanup_shouldNotBlockWhenEntryIsRemovedAfterQueueCheck() throws Exception {
        PlatformStatusTaskRunner runner = new PlatformStatusTaskRunner();
        DelayQueue<Delayed> queue = queueWithRemovalRace();
        ReflectionTestUtils.setField(runner, "registerDelayQueue", queue);

        assertDoesNotThrow(runner::expirationCheckForRegister);
        assertPollInsteadOfBlocking(queue);
    }

    @Test
    void platformKeepaliveCleanup_shouldNotBlockWhenEntryIsRemovedAfterQueueCheck() throws Exception {
        PlatformStatusTaskRunner runner = new PlatformStatusTaskRunner();
        DelayQueue<Delayed> queue = queueWithRemovalRace();
        ReflectionTestUtils.setField(runner, "keepaliveTaskDelayQueue", queue);

        assertDoesNotThrow(runner::expirationCheckForKeepalive);
        assertPollInsteadOfBlocking(queue);
    }

    @Test
    void ftpDownloadCleanup_shouldNotBlockWhenEntryIsRemovedAfterQueueCheck() throws Exception {
        FtpDownloadManager manager = new FtpDownloadManager();
        DelayQueue<Delayed> queue = queueWithRemovalRace();
        ReflectionTestUtils.setField(manager, "downloadCatchQueue", queue);

        assertDoesNotThrow(manager::downloadCatchCheck);
        assertPollInsteadOfBlocking(queue);
    }

    @Test
    void sipSubscribe_shouldRemoveEntryWhenTimeoutCallbackThrows() {
        SipSubscribe subscribe = new SipSubscribe();
        SipEvent event = SipEvent.getInstance("synthetic-subscription", null, result -> {
            throw new IllegalStateException("synthetic callback failure");
        }, -1);
        subscribe.addSubscribe(event.getKey(), event);

        assertDoesNotThrow(subscribe::execute);
        assertNull(subscribe.getSubscribe(event.getKey()));
    }

    @Test
    void messageSubscribe_shouldRemoveEntryWhenTimeoutCallbackThrows() {
        MessageSubscribe subscribe = new MessageSubscribe();
        MessageEvent<Object> event = MessageEvent.getInstance("synthetic-command", "1", "synthetic-device", -1L,
                (code, message, data) -> {
                    throw new IllegalStateException("synthetic callback failure");
                });
        subscribe.addSubscribe(event);

        assertDoesNotThrow(subscribe::execute);
        assertNull(subscribe.getSubscribe(event.getKey()));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Delayed> DelayQueue<T> queueWithRemovalRace() throws Exception {
        DelayQueue<T> queue = mock(DelayQueue.class);
        lenient().when(queue.isEmpty()).thenReturn(false, true);
        when(queue.poll()).thenReturn(null);
        lenient().doThrow(new InterruptedException("simulated concurrent removal")).when(queue).take();
        return queue;
    }

    private static void assertPollInsteadOfBlocking(DelayQueue<?> queue) throws Exception {
        verify(queue).poll();
        verify(queue, never()).take();
    }
}
