package com.genersoft.iot.vmp.jt1078.session;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.jt1078.bean.JTRecordDownloadCatch;
import com.genersoft.iot.vmp.jt1078.event.FtpUploadEvent;
import com.genersoft.iot.vmp.jt1078.proc.response.J9206;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DownloadManager {

    private final Map<String, JTRecordDownloadCatch> downloadCatchMap = new ConcurrentHashMap<>();
    private final DelayQueue<JTRecordDownloadCatch> downloadCatchQueue = new DelayQueue<>();

    private final Map<String, SynchronousQueue<Object>> topicSubscribers = new ConcurrentHashMap<>();

    // 下载过期检查
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void downloadCatchCheck(){
        while (!downloadCatchQueue.isEmpty()) {
            try {
                JTRecordDownloadCatch take = downloadCatchQueue.take();
                downloadCatchMap.remove(take.getPath());
            } catch (InterruptedException e) {
                log.error("[下载过期] ", e);
            }
        }
    }

    public void addCatch(String path, String phoneNumber, J9206 j9206) {
        JTRecordDownloadCatch downloadCatch = new JTRecordDownloadCatch();
        downloadCatch.setPhoneNumber(phoneNumber);
        downloadCatch.setPath(path);
        downloadCatch.setJ9206(j9206);

        // 10分钟临时地址无法访问则删除
        downloadCatch.setDelayTime(System.currentTimeMillis() + 10 * 60 * 1000L);

        downloadCatchMap.put(path, downloadCatch);
        downloadCatchQueue.add(downloadCatch);
    }

    public JTRecordDownloadCatch getCatch(String path) {
        return downloadCatchMap.get(path);
    }

    @EventListener
    public void onApplicationEvent(FtpUploadEvent event) {
        if (topicSubscribers.isEmpty()) {
            return;
        }
        topicSubscribers.keySet().forEach(key -> {
            if (!event.getFileName().contains(key)) {
                return;
            }
            SynchronousQueue<Object> synchronousQueue = topicSubscribers.get(key);
            if (synchronousQueue != null) {
                synchronousQueue.offer(null);
            }
        });
    }


    public Object runDownload(String path, long timeOut) {
        SynchronousQueue<Object> subscribe = subscribe(path);
        if (subscribe == null) {
            log.error("[JT-下载] 暂停进程失败");
            return null;
        }
        try {
            return subscribe.poll(timeOut, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("[JT-下载] 暂停进程超时", e);
        } finally {
            this.unsubscribe(path);
            JTRecordDownloadCatch downloadCatch = getCatch(path);
            if (downloadCatch != null) {
                downloadCatchMap.remove(path);
                downloadCatchQueue.remove(downloadCatch);
            }
        }
        return null;
    }

    private SynchronousQueue<Object> subscribe(String key) {
        SynchronousQueue<Object> queue = null;
        if (!topicSubscribers.containsKey(key))
            topicSubscribers.put(key, queue = new SynchronousQueue<>());
        return queue;
    }

    private void unsubscribe(String key) {
        topicSubscribers.remove(key);
    }

}
