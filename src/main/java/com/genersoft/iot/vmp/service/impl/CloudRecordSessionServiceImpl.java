package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMResult;
import com.genersoft.iot.vmp.service.ICloudRecordSessionService;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.storager.dao.CloudRecordServiceMapper;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.cloudRecord.bean.CloudRecordSessionContent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

/**
 * 将同一份已有MP4映射为互不干扰的观看会话；会话仅保存在创建它的WVP实例。
 * 暂停释放媒体源并保留调用方位置，恢复创建新流，因此不依赖ZLM的零倍速能力。
 */
@Slf4j
@Service
public class CloudRecordSessionServiceImpl implements ICloudRecordSessionService {
    private static final long RETENTION_MS = 120_000L;
    private static final long MAX_SEEK_MS = 0xffff_ffffL;
    private static final int MAX_SESSIONS = 1024;
    private static final String APP = MediaStreamUtil.LOAD_MP4_APP;

    private final CloudRecordServiceMapper recordMapper;
    private final IMediaServerService mediaService;
    private final ZLMRESTfulUtils zlm;
    private final UserSetting userSetting;
    private final Clock clock;
    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final Semaphore capacity = new Semaphore(MAX_SESSIONS);

    /** 使用现有录像索引、媒体节点和ZLM客户端，不增加数据库表或设备控制链。 */
    @Autowired
    public CloudRecordSessionServiceImpl(CloudRecordServiceMapper recordMapper,
                                        IMediaServerService mediaService, ZLMRESTfulUtils zlm,
                                        UserSetting userSetting) {
        this(recordMapper, mediaService, zlm, userSetting, Clock.systemUTC());
    }

    /** 注入时钟以验证暂停、过期和失败重试，不依赖测试中的真实等待。 */
    CloudRecordSessionServiceImpl(CloudRecordServiceMapper recordMapper,
                                 IMediaServerService mediaService, ZLMRESTfulUtils zlm,
                                 UserSetting userSetting, Clock clock) {
        this.recordMapper = recordMapper;
        this.mediaService = mediaService;
        this.zlm = zlm;
        this.userSetting = userSetting;
        this.clock = clock;
    }

    /** 校验录像归属后只在原节点加载文件，节点不可用时不盲目切换文件路径。 */
    @Override
    public CloudRecordSessionContent create(int userId, int cloudRecordId, long positionMs, String streamHost) {
        CloudRecordItem record = recordMapper.queryOne(cloudRecordId);
        if (record == null) {
            throw new ControllerException(ErrorCode.ERROR404);
        }
        if (record.getServerId() != null && !record.getServerId().isBlank()
                && !record.getServerId().equals(userSetting.getServerId())) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        MediaServer node = mediaService.getOne(record.getMediaServerId());
        if (node == null || !node.isStatus() || !"zlm".equals(node.getType())) {
            throw new ControllerException(ErrorCode.ERROR486);
        }
        if (record.getFilePath() == null || record.getFilePath().isBlank()
                || !Double.isFinite(record.getTimeLen()) || record.getTimeLen() < 1
                || record.getTimeLen() > MAX_SEEK_MS) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        Session session = new Session(userId, record, node, streamHost);
        validatePosition(session, positionMs);
        if (!capacity.tryAcquire()) {
            throw new ControllerException(ErrorCode.ERROR486);
        }
        synchronized (session) {
            // 先持有会话锁再发布，防止清扫器抢先看到尚未初始化的过期时间。
            sessions.put(session.id, session);
            try {
                load(session, positionMs);
                return content(session);
            } catch (RuntimeException error) {
                // 加载失败仍保留未确认关闭的流身份，清扫器可再次精确清理。
                if (session.stream == null) {
                    remove(session);
                } else {
                    session.expiresAt = clock.millis();
                }
                throw error;
            }
        }
    }

    /** 返回当前状态，查询不会给失联或长暂停会话续期。 */
    @Override
    public CloudRecordSessionContent get(int userId, String sessionId) {
        Session session = find(userId, sessionId);
        synchronized (session) {
            requireActive(session);
            return content(session);
        }
    }

    /** 播放中定位时重载自己的源；暂停中定位不占用媒体资源。 */
    @Override
    public CloudRecordSessionContent seek(int userId, String sessionId, long positionMs) {
        Session session = find(userId, sessionId);
        synchronized (session) {
            requireActive(session);
            validatePosition(session, positionMs);
            if (session.paused) {
                session.positionMs = positionMs;
                session.expiresAt = clock.millis() + RETENTION_MS;
            } else {
                stopMedia(session);
                load(session, positionMs);
            }
            return content(session);
        }
    }

    /** 关闭成功后才报告暂停，失败时保留媒体身份以允许重试。 */
    @Override
    public CloudRecordSessionContent pause(int userId, String sessionId, long positionMs) {
        Session session = find(userId, sessionId);
        synchronized (session) {
            requireActive(session);
            validatePosition(session, positionMs);
            stopMedia(session);
            session.positionMs = positionMs;
            session.paused = true;
            session.expiresAt = clock.millis() + RETENTION_MS;
            return content(session);
        }
    }

    /** 恢复幂等；已播放的会话保留原地址，已暂停的会话从保存位置重建。 */
    @Override
    public CloudRecordSessionContent resume(int userId, String sessionId) {
        Session session = find(userId, sessionId);
        synchronized (session) {
            requireActive(session);
            if (session.paused) {
                load(session, session.positionMs);
            }
            return content(session);
        }
    }

    /** 精确关闭本会话的唯一媒体源，重复关闭已经结束的会话视为成功。 */
    @Override
    public void close(int userId, String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return;
        }
        checkOwner(session, userId);
        synchronized (session) {
            if (sessions.get(sessionId) == session) {
                stopMedia(session);
                remove(session);
            }
        }
    }

    /** 仅消费当前源、原节点的主协议离开事件，旧源的迟到事件不能终止新源。 */
    @Async
    @EventListener
    public void onMediaDeparture(MediaDepartureEvent event) {
        if (!APP.equals(event.getApp()) || event.getMediaServer() == null
                || (event.getSchema() != null && !"rtsp".equals(event.getSchema()))) {
            return;
        }
        for (Session session : sessions.values()) {
            synchronized (session) {
                if (event.getStream().equals(session.stream)
                        && event.getMediaServer().getId().equals(session.node.getId())) {
                    session.stream = null;
                    session.playback = null;
                    remove(session);
                    return;
                }
            }
        }
    }

    /** 清理暂停、未打开或失联会话的内存状态，媒体关闭失败时保留身份下轮重试。 */
    @Scheduled(fixedDelay = 10_000)
    public void expireSessions() {
        for (Session session : sessions.values()) {
            synchronized (session) {
                if (clock.millis() >= session.expiresAt) {
                    try {
                        stopMedia(session);
                        remove(session);
                    } catch (RuntimeException error) {
                        log.warn("[云录像会话] 过期清理未完成，将重试，session={}", session.id);
                    }
                }
            }
        }
    }

    /** 正常停机尽力释放本实例创建的媒体源；异常停机仍由ZLM无人观看和EOF回收。 */
    @PreDestroy
    public void shutdown() {
        for (Session session : sessions.values()) {
            try {
                close(session.userId, session.id);
            } catch (RuntimeException error) {
                log.warn("[云录像会话] 停机清理未完成，session={}", session.id);
            }
        }
    }

    /** 同步加载后复用原URL构造器，不创建可能滞留的Hook订阅。 */
    private void load(Session session, long positionMs) {
        String stream = "session_" + UUID.randomUUID().toString().replace("-", "");
        session.stream = stream;
        session.playback = null;
        session.paused = true;
        session.positionMs = positionMs;
        session.expiresAt = clock.millis() + RETENTION_MS;
        try {
            checkResult(zlm.loadMP4File(session.node, APP, stream, session.record.getFilePath(), positionMs));
            StreamInfo info = mediaService.getStreamInfoByAppAndStream(session.node, APP, stream,
                    null, session.streamHost, null, true);
            if (info == null) {
                throw new ControllerException(ErrorCode.ERROR486);
            }
            info.setDuration((double) session.durationMs);
            session.playback = new StreamContent(info);
            session.paused = false;
            session.expiresAt = clock.millis() + session.durationMs - positionMs + RETENTION_MS;
        } catch (RuntimeException error) {
            try {
                stopMedia(session);
            } catch (RuntimeException cleanupError) {
                // 不丢弃关闭失败的唯一流名，防止失败请求留下无法精确回收的媒体源。
                session.expiresAt = clock.millis();
            }
            throw new ControllerException(ErrorCode.ERROR486);
        }
    }

    /** 只关闭服务端生成的精确app和stream；成功前保留身份，禁止通配关闭。 */
    private void stopMedia(Session session) {
        if (session.stream == null) {
            return;
        }
        checkResult(zlm.closeStreams(session.node, APP, session.stream));
        session.stream = null;
        session.playback = null;
    }

    /** 使用既有错误协议，避免将ZLM路径和原始错误返回给调用方。 */
    private void checkResult(ZLMResult<?> result) {
        if (result == null || result.getCode() != ErrorCode.SUCCESS.getCode()) {
            throw new ControllerException(ErrorCode.ERROR486);
        }
    }

    /** 查找归当前WVP用户持有的会话，未知会话不触发媒体操作。 */
    private Session find(int userId, String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new ControllerException(ErrorCode.ERROR404);
        }
        checkOwner(session, userId);
        return session;
    }

    /** 当前用户检查先于过期清理，拒绝请求不能产生关闭媒体的副作用。 */
    private void checkOwner(Session session, int userId) {
        if (session.userId != userId) {
            throw new ControllerException(ErrorCode.ERROR403);
        }
    }

    /** 锁内重新确认会话仍属本次对象，阻止关闭或过期后的在途控制重新建流。 */
    private void requireActive(Session session) {
        if (sessions.get(session.id) != session) {
            throw new ControllerException(ErrorCode.ERROR404);
        }
        if (clock.millis() >= session.expiresAt) {
            stopMedia(session);
            remove(session);
            throw new ControllerException(ErrorCode.ERROR404);
        }
    }

    /** 毫秒位置须位于文件内并满足ZLM seek_ms的uint32范围，禁止负值和溢出。 */
    private void validatePosition(Session session, long positionMs) {
        if (positionMs < 0 || positionMs >= session.durationMs || positionMs > MAX_SEEK_MS) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
    }

    /** 条件删除只释放一次容量，迟到事件不能重复释放或删除替代会话。 */
    private void remove(Session session) {
        if (sessions.remove(session.id, session)) {
            capacity.release();
        }
    }

    /** 显式构造安全响应，只复用已有StreamContent媒体地址契约。 */
    private CloudRecordSessionContent content(Session session) {
        CloudRecordSessionContent result = new CloudRecordSessionContent();
        result.setSessionId(session.id);
        result.setCloudRecordId(session.record.getId());
        result.setState(session.paused ? "paused" : "playing");
        result.setPositionMs(session.positionMs);
        result.setDurationMs(session.durationMs);
        result.setExpiresAt(session.expiresAt);
        result.setServerId(userSetting.getServerId());
        result.setPlayback(session.playback);
        return result;
    }

    /** 只在会话锁内变更当前媒体身份；文件记录与节点归属在创建后固定。 */
    private static final class Session {
        private final String id = UUID.randomUUID().toString();
        private final int userId;
        private final CloudRecordItem record;
        private final MediaServer node;
        private final String streamHost;
        private final long durationMs;
        private boolean paused;
        private String stream;
        private StreamContent playback;
        private long positionMs;
        private long expiresAt;

        /** 固定录像和原节点，避免后续全局负载选择改变文件所在位置。 */
        private Session(int userId, CloudRecordItem record, MediaServer node, String streamHost) {
            this.userId = userId;
            this.record = record;
            this.node = node;
            this.streamHost = streamHost;
            this.durationMs = (long) record.getTimeLen();
        }
    }
}
