package com.genersoft.iot.vmp.gb28181.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.PlatformMapper;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaSendRtpStoppedEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

/**
 * @author lin
 */
@Slf4j
@Service
@DS("master")
public class PlatformServiceImpl implements IPlatformService {

    private final static String REGISTER_KEY_PREFIX = "platform_register_";

    private final static String REGISTER_FAIL_AGAIN_KEY_PREFIX = "platform_register_fail_again_";
    private final static String KEEPALIVE_KEY_PREFIX = "platform_keepalive_";

    @Autowired
    private PlatformMapper platformMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private IInviteStreamService inviteStreamService;


    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByStream(event.getStream());
        if (!sendRtpItems.isEmpty()) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                if (sendRtpItem != null && sendRtpItem.getApp().equals(event.getApp())) {
                    String platformId = sendRtpItem.getPlatformId();
                    Platform platform = platformMapper.getParentPlatByServerGBId(platformId);

                    try {
                        if (platform != null) {
                            commanderForPlatform.streamByeCmd(platform, sendRtpItem);
                            redisCatchStorage.deleteSendRTPServer(platformId, sendRtpItem.getChannelId(),
                                    sendRtpItem.getCallId(), sendRtpItem.getStream());
                        }
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[命令发送失败] 发送BYE: {}", e.getMessage());
                    }
                }
            }
        }
    }


    /**
     * 发流停止
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaSendRtpStoppedEvent event) {
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByStream(event.getStream());
        if (sendRtpItems != null && !sendRtpItems.isEmpty()) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                Platform platform = platformMapper.getParentPlatByServerGBId(sendRtpItem.getPlatformId());
                ssrcFactory.releaseSsrc(sendRtpItem.getMediaServerId(), sendRtpItem.getSsrc());
                try {
                    commanderForPlatform.streamByeCmd(platform, sendRtpItem.getCallId());
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                }
                redisCatchStorage.deleteSendRTPServer(platform.getServerGBId(), sendRtpItem.getChannelId(),
                        sendRtpItem.getCallId(), sendRtpItem.getStream());
            }
        }
    }


    @Override
    public Platform queryPlatformByServerGBId(String platformGbId) {
        return platformMapper.getParentPlatByServerGBId(platformGbId);
    }

    @Override
    public PageInfo<Platform> queryPlatformList(int page, int count) {
        PageHelper.startPage(page, count);
        List<Platform> all = platformMapper.queryList();
        return new PageInfo<>(all);
    }

    @Override
    public boolean add(Platform platform) {
        log.info("[国标级联]添加平台 {}", platform.getDeviceGBId());
        if (platform.getCatalogGroup() == 0) {
            // 每次发送目录的数量默认为1
            platform.setCatalogGroup(1);
        }
        int result = platformMapper.add(platform);
        // 添加缓存
        PlatformCatch platformCatch = new PlatformCatch();
        platformCatch.setPlatform(platform);
        platformCatch.setId(platform.getServerGBId());
        redisCatchStorage.updatePlatformCatchInfo(platformCatch);
        if (platform.isEnable()) {
            // 保存时启用就发送注册
            // 注册成功时由程序直接调用了online方法
            try {
                commanderForPlatform.register(platform, eventResult -> {
                    log.info("[国标级联] {}（{}）,添加向上级注册失败，请确定上级平台可用时重新保存", platform.getName(), platform.getServerGBId());
                }, null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                log.error("[命令发送失败] 国标级联: {}", e.getMessage());
            }
        }
        return result > 0;
    }

    @Override
    public boolean update(Platform platform) {
        Assert.isTrue(platform.getId() > 0, "ID必须存在");
        log.info("[国标级联] 更新平台 {}({})", platform.getName(), platform.getDeviceGBId());
        platform.setCharacterSet(platform.getCharacterSet().toUpperCase());
        Platform platformInDb = platformMapper.query(platform.getId());
        PlatformCatch platformCatchOld = redisCatchStorage.queryPlatformCatchInfo(platformInDb.getServerGBId());
        platform.setUpdateTime(DateUtil.getNow());

        // 停止心跳定时
        final String keepaliveTaskKey = KEEPALIVE_KEY_PREFIX + platformInDb.getServerGBId();
        dynamicTask.stop(keepaliveTaskKey);
        // 停止注册定时
        final String registerTaskKey = REGISTER_KEY_PREFIX + platformInDb.getServerGBId();
        dynamicTask.stop(registerTaskKey);
        // 注销旧的
        try {
            if (platformInDb.isStatus() && platformCatchOld != null) {
                log.info("保存平台{}时发现旧平台在线，发送注销命令", platformInDb.getServerGBId());
                commanderForPlatform.unregister(platformInDb, platformCatchOld.getSipTransactionInfo(), null, eventResult -> {
                    log.info("[国标级联] 注销成功， 平台：{}", platformInDb.getServerGBId());
                });
            }
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[命令发送失败] 国标级联 注销: {}", e.getMessage());
        }

        // 更新数据库
        if (platform.getCatalogGroup() == 0) {
            platform.setCatalogGroup(1);
        }

        platformMapper.update(platform);
        // 更新redis
        redisCatchStorage.delPlatformCatchInfo(platformInDb.getServerGBId());
        PlatformCatch platformCatch = new PlatformCatch();
        platformCatch.setPlatform(platform);
        platformCatch.setId(platform.getServerGBId());
        redisCatchStorage.updatePlatformCatchInfo(platformCatch);
        // 注册
        if (platform.isEnable()) {
            // 保存时启用就发送注册
            // 注册成功时由程序直接调用了online方法
            try {
                log.info("[国标级联] 平台注册 {}", platform.getDeviceGBId());
                commanderForPlatform.register(platform, eventResult -> {
                    log.info("[国标级联] {},添加向上级注册失败，请确定上级平台可用时重新保存", platform.getServerGBId());
                }, null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                log.error("[命令发送失败] 国标级联: {}", e.getMessage());
            }
        }

        return false;
    }


    @Override
    public void online(Platform platform, SipTransactionInfo sipTransactionInfo) {
        log.info("[国标级联]：{}, 平台上线", platform.getServerGBId());
        final String registerFailAgainTaskKey = REGISTER_FAIL_AGAIN_KEY_PREFIX + platform.getServerGBId();
        dynamicTask.stop(registerFailAgainTaskKey);

        platformMapper.updateStatus(platform.getServerGBId(), true);
        PlatformCatch platformCatch = redisCatchStorage.queryPlatformCatchInfo(platform.getServerGBId());
        if (platformCatch == null) {
            platformCatch = new PlatformCatch();
            platformCatch.setPlatform(platform);
            platformCatch.setId(platform.getServerGBId());
            platform.setStatus(true);
            platformCatch.setPlatform(platform);
        }

        platformCatch.getPlatform().setStatus(true);
        platformCatch.setSipTransactionInfo(sipTransactionInfo);
        redisCatchStorage.updatePlatformCatchInfo(platformCatch);

        final String registerTaskKey = REGISTER_KEY_PREFIX + platform.getServerGBId();
        if (!dynamicTask.isAlive(registerTaskKey)) {
            log.info("[国标级联]：{}, 添加定时注册任务", platform.getServerGBId());
            // 添加注册任务
            dynamicTask.startCron(registerTaskKey,
                // 注册失败（注册成功时由程序直接调用了online方法）
                ()-> registerTask(platform, sipTransactionInfo),
                    platform.getExpires() * 1000);
        }


        final String keepaliveTaskKey = KEEPALIVE_KEY_PREFIX + platform.getServerGBId();
        if (!dynamicTask.contains(keepaliveTaskKey)) {
            log.info("[国标级联]：{}, 添加定时心跳任务", platform.getServerGBId());
            // 添加心跳任务
            dynamicTask.startCron(keepaliveTaskKey,
                    ()-> {
                        try {
                            commanderForPlatform.keepalive(platform, eventResult -> {
                                // 心跳失败
                                if (eventResult.type != SipSubscribe.EventResultType.timeout) {
                                    log.warn("[国标级联]发送心跳收到错误，code： {}, msg: {}", eventResult.statusCode, eventResult.msg);
                                }
                                // 心跳失败
                                PlatformCatch platformCatchForNow = redisCatchStorage.queryPlatformCatchInfo(platform.getServerGBId());
                                // 此时是第三次心跳超时， 平台离线
                                if (platformCatchForNow.getKeepAliveReply()  == 2) {
                                    // 设置平台离线，并重新注册
                                    log.info("[国标级联] 三次心跳失败, 平台{}({})离线", platform.getName(), platform.getServerGBId());
                                    offline(platform, false);
                                }else {
                                    platformCatchForNow.setKeepAliveReply(platformCatchForNow.getKeepAliveReply() + 1);
                                    redisCatchStorage.updatePlatformCatchInfo(platformCatchForNow);
                                }

                            }, eventResult -> {
                                // 心跳成功
                                // 清空之前的心跳超时计数
                                PlatformCatch platformCatchForNow = redisCatchStorage.queryPlatformCatchInfo(platform.getServerGBId());
                                if (platformCatchForNow != null && platformCatchForNow.getKeepAliveReply() > 0) {
                                    platformCatchForNow.setKeepAliveReply(0);
                                    redisCatchStorage.updatePlatformCatchInfo(platformCatchForNow);
                                }
                                log.info("[发送心跳] 国标级联 发送心跳, code： {}, msg: {}", eventResult.statusCode, eventResult.msg);
                            });
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            log.error("[命令发送失败] 国标级联 发送心跳: {}", e.getMessage());
                        }
                    },
                    (platform.getKeepTimeout())*1000);
        }
        if (platform.getAutoPushChannel() != null && platform.getAutoPushChannel()) {
            if (subscribeHolder.getCatalogSubscribe(platform.getServerGBId()) == null) {
                log.info("[国标级联]：{}, 添加自动通道推送模拟订阅信息", platform.getServerGBId());
                addSimulatedSubscribeInfo(platform);
            }
        }else {
            SubscribeInfo catalogSubscribe = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
            if (catalogSubscribe != null && catalogSubscribe.getExpires() == -1) {
                subscribeHolder.removeCatalogSubscribe(platform.getServerGBId());
            }
        }
    }

    @Override
    public void addSimulatedSubscribeInfo(Platform platform) {
        // 自动添加一条模拟的订阅信息
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setId(platform.getServerGBId());
        subscribeInfo.setExpires(-1);
        subscribeInfo.setEventType("Catalog");
        int random = (int) Math.floor(Math.random() * 10000);
        subscribeInfo.setEventId(random + "");
        subscribeInfo.setSimulatedCallId(UUID.randomUUID().toString().replace("-", "") + "@" + platform.getServerIp());
        subscribeInfo.setSimulatedFromTag(UUID.randomUUID().toString().replace("-", ""));
        subscribeInfo.setSimulatedToTag(UUID.randomUUID().toString().replace("-", ""));
        subscribeHolder.putCatalogSubscribe(platform.getServerGBId(), subscribeInfo);
    }

    private void registerTask(Platform platform, SipTransactionInfo sipTransactionInfo){
        try {
            // 不在同一个会话中续订则每次全新注册
            if (!userSetting.isRegisterKeepIntDialog()) {
                sipTransactionInfo = null;
            }

            if (sipTransactionInfo == null) {
                log.info("[国标级联] 平台：{}注册即将到期，开始重新注册", platform.getServerGBId());
            }else {
                log.info("[国标级联] 平台：{}注册即将到期，开始续订", platform.getServerGBId());
            }

            commanderForPlatform.register(platform, sipTransactionInfo,  eventResult -> {
                log.info("[国标级联] 平台：{}注册失败，{}:{}", platform.getServerGBId(),
                        eventResult.statusCode, eventResult.msg);
                offline(platform, false);
            }, null);
        } catch (Exception e) {
            log.error("[命令发送失败] 国标级联定时注册: {}", e.getMessage());
        }
    }

    @Override
    public void offline(Platform platform, boolean stopRegister) {
        log.info("[平台离线]：{}({})", platform.getName(), platform.getServerGBId());
        PlatformCatch platformCatch = redisCatchStorage.queryPlatformCatchInfo(platform.getServerGBId());
        platformCatch.setKeepAliveReply(0);
        platformCatch.setRegisterAliveReply(0);
        Platform catchPlatform = platformCatch.getPlatform();
        catchPlatform.setStatus(false);
        platformCatch.setPlatform(catchPlatform);
        redisCatchStorage.updatePlatformCatchInfo(platformCatch);
        platformMapper.updateStatus(platform.getServerGBId(), false);

        // 停止所有推流
        log.info("[平台离线] {}({}), 停止所有推流", platform.getName(),  platform.getServerGBId());
        stopAllPush(platform.getServerGBId());

        // 清除注册定时
        log.info("[平台离线] {}({}), 停止定时注册任务", platform.getName(), platform.getServerGBId());
        final String registerTaskKey = REGISTER_KEY_PREFIX + platform.getServerGBId();
        if (dynamicTask.contains(registerTaskKey)) {
            dynamicTask.stop(registerTaskKey);
        }
        // 清除心跳定时
        log.info("[平台离线] {}({}), 停止定时发送心跳任务", platform.getName(), platform.getServerGBId());
        final String keepaliveTaskKey = KEEPALIVE_KEY_PREFIX + platform.getServerGBId();
        if (dynamicTask.contains(keepaliveTaskKey)) {
            // 清除心跳任务
            dynamicTask.stop(keepaliveTaskKey);
        }
        // 停止订阅回复
        SubscribeInfo catalogSubscribe = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
        if (catalogSubscribe != null) {
            if (catalogSubscribe.getExpires() > 0) {
                log.info("[平台离线] {}({}), 停止目录订阅回复", platform.getName(), platform.getServerGBId());
                subscribeHolder.removeCatalogSubscribe(platform.getServerGBId());
            }
        }
        log.info("[平台离线] {}({}), 停止移动位置订阅回复", platform.getName(), platform.getServerGBId());
        subscribeHolder.removeMobilePositionSubscribe(platform.getServerGBId());
        // 发起定时自动重新注册
        if (!stopRegister) {
            // 设置为60秒自动尝试重新注册
            final String registerFailAgainTaskKey = REGISTER_FAIL_AGAIN_KEY_PREFIX + platform.getServerGBId();
            Platform platformInDb = platformMapper.query(platform.getId());
            if (platformInDb.isEnable()) {
                dynamicTask.startCron(registerFailAgainTaskKey,
                        ()-> registerTask(platformInDb, null),
                        userSetting.getRegisterAgainAfterTime() * 1000);
            }
        }
    }

    private void stopAllPush(String platformId) {
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServer(platformId);
        if (sendRtpItems != null && sendRtpItems.size() > 0) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                ssrcFactory.releaseSsrc(sendRtpItem.getMediaServerId(), sendRtpItem.getSsrc());
                redisCatchStorage.deleteSendRTPServer(platformId, sendRtpItem.getChannelId(), null, null);
                MediaServer mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                mediaServerService.stopSendRtp(mediaInfo, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
            }
        }
    }

    @Override
    public void login(Platform platform) {
        final String registerTaskKey = REGISTER_KEY_PREFIX + platform.getServerGBId();
        try {
            commanderForPlatform.register(platform, eventResult1 -> {
                log.info("[国标级联] {}，开始定时发起注册，间隔为1分钟", platform.getServerGBId());
                // 添加注册任务
                dynamicTask.startCron(registerTaskKey,
                        // 注册失败（注册成功时由程序直接调用了online方法）
                        ()-> log.info("[国标级联] {}({}),平台离线后持续发起注册，失败", platform.getName(), platform.getServerGBId()),
                        60*1000);
            }, null);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[命令发送失败] 国标级联注册: {}", e.getMessage());
        }
    }

    @Override
    public void sendNotifyMobilePosition(String platformId) {
        Platform platform = platformMapper.getParentPlatByServerGBId(platformId);
        if (platform == null) {
            return;
        }
        SubscribeInfo subscribe = subscribeHolder.getMobilePositionSubscribe(platform.getServerGBId());
        if (subscribe != null) {

            // TODO 暂时只处理视频流的回复,后续增加对国标设备的支持
            List<DeviceChannel> gbStreams = gbStreamMapper.queryGbStreamListInPlatform(platform.getServerGBId(), userSetting.isUsePushingAsStatus());
            if (gbStreams.size() == 0) {
                return;
            }
            for (DeviceChannel deviceChannel : gbStreams) {
                String gbId = deviceChannel.getDeviceId();
                GPSMsgInfo gpsMsgInfo = redisCatchStorage.getGpsMsgInfo(gbId);
                // 无最新位置不发送
                if (gpsMsgInfo != null) {
                    // 经纬度都为0不发送
                    if (gpsMsgInfo.getLng() == 0 && gpsMsgInfo.getLat() == 0) {
                        continue;
                    }
                    // 发送GPS消息
                    try {
                        commanderForPlatform.sendNotifyMobilePosition(platform, gpsMsgInfo, subscribe);
                    } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                             IllegalAccessException e) {
                        log.error("[命令发送失败] 国标级联 移动位置通知: {}", e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void broadcastInvite(Platform platform, String channelId, MediaServer mediaServerItem, HookSubscribe.Event hookEvent,
                                SipSubscribe.Event errorEvent, InviteTimeOutCallback timeoutCallback) throws InvalidArgumentException, ParseException, SipException {

        if (mediaServerItem == null) {
            log.info("[国标级联] 语音喊话未找到可用的zlm. platform: {}", platform.getServerGBId());
            return;
        }
        InviteInfo inviteInfoForOld = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, platform.getServerGBId(), channelId);

        if (inviteInfoForOld != null && inviteInfoForOld.getStreamInfo() != null) {
            // 如果zlm不存在这个流，则删除数据即可
            MediaServer mediaServerItemForStreamInfo = mediaServerService.getOne(inviteInfoForOld.getStreamInfo().getMediaServer().getId());
            if (mediaServerItemForStreamInfo != null) {
                Boolean ready = mediaServerService.isStreamReady(mediaServerItemForStreamInfo, inviteInfoForOld.getStreamInfo().getApp(), inviteInfoForOld.getStreamInfo().getStream());
                if (!ready) {
                    // 错误存在于redis中的数据
                    inviteStreamService.removeInviteInfo(inviteInfoForOld);
                }else {
                    // 流确实尚在推流，直接回调结果
                    HookData hookData = new HookData();
                    hookData.setApp(inviteInfoForOld.getStreamInfo().getApp());
                    hookData.setStream(inviteInfoForOld.getStreamInfo().getStream());
                    hookData.setMediaServer(mediaServerItemForStreamInfo);
                    hookEvent.response(hookData);
                    return;
                }
            }
        }

        String streamId = null;
        if (mediaServerItem.isRtpEnable()) {
            streamId = String.format("%s_%s", platform.getServerGBId(), channelId);
        }
        // 默认不进行SSRC校验， TODO 后续可改为配置
        boolean ssrcCheck = false;
        int tcpMode;
        if (userSetting.getBroadcastForPlatform().equalsIgnoreCase("TCP-PASSIVE")) {
            tcpMode = 1;
        }else if (userSetting.getBroadcastForPlatform().equalsIgnoreCase("TCP-ACTIVE")) {
            tcpMode = 2;
        } else {
            tcpMode = 0;
        }
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, null, ssrcCheck, false, null, true, false, false, tcpMode);
        if (ssrcInfo == null || ssrcInfo.getPort() < 0) {
            log.info("[国标级联] 发起语音喊话 开启端口监听失败， platform: {}, channel： {}", platform.getServerGBId(), channelId);
            SipSubscribe.EventResult<Object> eventResult = new SipSubscribe.EventResult<>();
            eventResult.statusCode = -1;
            eventResult.msg = "端口监听失败";
            eventResult.type = SipSubscribe.EventResultType.failedToGetPort;
            errorEvent.response(eventResult);
            return;
        }
        log.info("[国标级联] 语音喊话，发起Invite消息 deviceId: {}, channelId: {},收流端口： {}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                platform.getServerGBId(), channelId, ssrcInfo.getPort(), userSetting.getBroadcastForPlatform(), ssrcInfo.getSsrc(), ssrcCheck);

        // 初始化redis中的invite消息状态
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(platform.getServerGBId(), channelId, ssrcInfo.getStream(), ssrcInfo,
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), userSetting.getBroadcastForPlatform(), InviteSessionType.BROADCAST,
                InviteSessionStatus.ready);
        inviteStreamService.updateInviteInfo(inviteInfo);
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            // 执行超时任务时查询是否已经成功，成功了则不执行超时任务，防止超时任务取消失败的情况
            InviteInfo inviteInfoForBroadcast = inviteStreamService.getInviteInfo(InviteSessionType.BROADCAST, platform.getServerGBId(), channelId, null);
            if (inviteInfoForBroadcast == null) {
                log.info("[国标级联] 发起语音喊话 收流超时 deviceId: {}, channelId: {}，端口：{}, SSRC: {}", platform.getServerGBId(), channelId, ssrcInfo.getPort(), ssrcInfo.getSsrc());
                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                try {
                    commanderForPlatform.streamByeCmd(platform, channelId, ssrcInfo.getStream(), null, null);
                } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                    log.error("[点播超时]， 发送BYE失败 {}", e.getMessage());
                } finally {
                    timeoutCallback.run(1, "收流超时");
                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                    streamSession.remove(platform.getServerGBId(), channelId, ssrcInfo.getStream());
                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                }
            }
        }, userSetting.getPlayTimeout());
        commanderForPlatform.broadcastInviteCmd(platform, channelId, mediaServerItem, ssrcInfo, (hookData)->{
            log.info("[国标级联] 发起语音喊话 收到上级推流 deviceId: {}, channelId: {}", platform.getServerGBId(), channelId);
            dynamicTask.stop(timeOutTaskKey);
            // hook响应
            onPublishHandlerForBroadcast(hookData.getMediaServer(), hookData.getMediaInfo(), platform.getServerGBId(), channelId);
            // 收到流
            if (hookEvent != null) {
                hookEvent.response(hookData);
            }
        }, event -> {

            inviteOKHandler(event, ssrcInfo, tcpMode, ssrcCheck, mediaServerItem, platform, channelId, timeOutTaskKey,
                    null, inviteInfo, InviteSessionType.BROADCAST);
//            // 收到200OK 检测ssrc是否有变化，防止上级自定义了ssrc
//            ResponseEvent responseEvent = (ResponseEvent) event.event;
//            String contentString = new String(responseEvent.getResponse().getRawContent());
//            // 获取ssrc
//            int ssrcIndex = contentString.indexOf("y=");
//            // 检查是否有y字段
//            if (ssrcIndex >= 0) {
//                //ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段 TODO 后续对不规范的非10位ssrc兼容
//                String ssrcInResponse = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
//                // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
//                if (ssrcInfo.getSsrc().equals(ssrcInResponse) || ssrcCheck) {
//                    tcpActiveHandler(platform, )
//                    return;
//                }
//                logger.info("[点播消息] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
//                if (!mediaServerItem.isRtpEnable()) {
//                    logger.info("[点播消息] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
//                    // 释放ssrc
//                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
//                    // 单端口模式streamId也有变化，需要重新设置监听
//                    if (!mediaServerItem.isRtpEnable()) {
//                        // 添加订阅
//                        HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
//                        subscribe.removeSubscribe(hookSubscribe);
//                        hookSubscribe.getContent().put("stream", String.format("%08x", Integer.parseInt(ssrcInResponse)).toUpperCase());
//                        subscribe.addSubscribe(hookSubscribe, (mediaServerItemInUse, hookParam) -> {
//                            logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + hookParam);
//                            dynamicTask.stop(timeOutTaskKey);
//                            // hook响应
//                            playService.onPublishHandlerForPlay(mediaServerItemInUse, hookParam, platform.getServerGBId(), channelId);
//                            hookEvent.response(mediaServerItemInUse, hookParam);
//                        });
//                    }
//                    // 关闭rtp server
//                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
//                    // 重新开启ssrc server
//                    mediaServerService.openRTPServer(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse, false, false, ssrcInfo.getPort(), true, false, tcpMode);
//                }
//            }
        }, eventResult -> {
            // 收到错误回复
            if (errorEvent != null) {
                errorEvent.response(eventResult);
            }
        });
    }

    public void onPublishHandlerForBroadcast(MediaServer mediaServerItem, MediaInfo mediaInfo, String platFormServerId, String channelId) {
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, mediaInfo.getApp(), mediaInfo.getStream(), mediaInfo, null);
        streamInfo.setChannelId(channelId);

        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, platFormServerId, channelId);
        if (inviteInfo != null) {
            inviteInfo.setStatus(InviteSessionStatus.ok);
            inviteInfo.setStreamInfo(streamInfo);
            inviteStreamService.updateInviteInfo(inviteInfo);
        }
    }

    private void inviteOKHandler(SipSubscribe.EventResult eventResult, SSRCInfo ssrcInfo, int tcpMode, boolean ssrcCheck, MediaServer mediaServerItem,
                                 Platform platform, String channelId, String timeOutTaskKey, ErrorCallback<Object> callback,
                                 InviteInfo inviteInfo, InviteSessionType inviteSessionType){
        inviteInfo.setStatus(InviteSessionStatus.ok);
        ResponseEvent responseEvent = (ResponseEvent) eventResult.event;
        String contentString = new String(responseEvent.getResponse().getRawContent());
        System.out.println(contentString);
        String ssrcInResponse = SipUtils.getSsrcFromSdp(contentString);
        // 兼容回复的消息中缺少ssrc(y字段)的情况
        if (ssrcInResponse == null) {
            ssrcInResponse = ssrcInfo.getSsrc();
        }
        if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
            // ssrc 一致
            if (mediaServerItem.isRtpEnable()) {
                // 多端口
                if (tcpMode == 2) {
                    tcpActiveHandler(platform, channelId, contentString, mediaServerItem, tcpMode, ssrcCheck,
                            timeOutTaskKey, ssrcInfo, callback);
                }
            }else {
                // 单端口
                if (tcpMode == 2) {
                    log.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                }
            }
        }else {
            log.info("[Invite 200OK] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
            // ssrc 不一致
            if (mediaServerItem.isRtpEnable()) {
                // 多端口
                if (ssrcCheck) {
                    // ssrc检验
                    // 更新ssrc
                    log.info("[Invite 200OK] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
                    // 释放ssrc
                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                    Boolean result = mediaServerService.updateRtpServerSSRC(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse);
                    if (!result) {
                        try {
                            log.warn("[Invite 200OK] 更新ssrc失败，停止喊话 {}/{}", platform.getServerGBId(), channelId);
                            commanderForPlatform.streamByeCmd(platform, channelId, ssrcInfo.getStream(), null, null);
                        } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                            log.error("[命令发送失败] 停止播放， 发送BYE: {}", e.getMessage());
                        }

                        dynamicTask.stop(timeOutTaskKey);
                        // 释放ssrc
                        mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                        streamSession.remove(platform.getServerGBId(), channelId, ssrcInfo.getStream());

                        callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                "下级自定义了ssrc,重新设置收流信息失败", null);
                        inviteStreamService.call(inviteSessionType, platform.getServerGBId(), channelId, null,
                                InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                "下级自定义了ssrc,重新设置收流信息失败", null);

                    }else {
                        ssrcInfo.setSsrc(ssrcInResponse);
                        inviteInfo.setSsrcInfo(ssrcInfo);
                        inviteInfo.setStream(ssrcInfo.getStream());
                        if (tcpMode == 2) {
                            if (mediaServerItem.isRtpEnable()) {
                                tcpActiveHandler(platform, channelId, contentString, mediaServerItem, tcpMode, ssrcCheck,
                                        timeOutTaskKey, ssrcInfo, callback);
                            }else {
                                log.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                            }
                        }
                        inviteStreamService.updateInviteInfo(inviteInfo);
                    }
                }else {
                    ssrcInfo.setSsrc(ssrcInResponse);
                    inviteInfo.setSsrcInfo(ssrcInfo);
                    inviteInfo.setStream(ssrcInfo.getStream());
                    if (tcpMode == 2) {
                        if (mediaServerItem.isRtpEnable()) {
                            tcpActiveHandler(platform, channelId, contentString, mediaServerItem, tcpMode, ssrcCheck,
                                    timeOutTaskKey, ssrcInfo, callback);
                        }else {
                            log.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                        }
                    }
                    inviteStreamService.updateInviteInfo(inviteInfo);
                }
            }else {
                if (ssrcInResponse != null) {
                    // 单端口
                    // 重新订阅流上线
                    SsrcTransaction ssrcTransaction = streamSession.getSsrcTransaction(inviteInfo.getDeviceId(),
                            inviteInfo.getChannelId(), null, inviteInfo.getStream());
                    streamSession.remove(inviteInfo.getDeviceId(),
                            inviteInfo.getChannelId(), inviteInfo.getStream());
                    inviteStreamService.updateInviteInfoForSSRC(inviteInfo, ssrcInResponse);
                    streamSession.put(platform.getServerGBId(), channelId, ssrcTransaction.getCallId(),
                            inviteInfo.getStream(), ssrcInResponse, mediaServerItem.getId(), (SIPResponse) responseEvent.getResponse(), inviteSessionType);
                }
            }
        }
    }


    private void tcpActiveHandler(Platform platform, String channelId, String contentString,
                                  MediaServer mediaServerItem, int tcpMode, boolean ssrcCheck,
                                  String timeOutTaskKey, SSRCInfo ssrcInfo, ErrorCallback<Object> callback){
        if (tcpMode != 2) {
            return;
        }

        String substring;
        if (contentString.indexOf("y=") > 0) {
            substring = contentString.substring(0, contentString.indexOf("y="));
        }else {
            substring = contentString;
        }
        try {
            SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(substring);
            int port = -1;
            Vector mediaDescriptions = sdp.getMediaDescriptions(true);
            for (Object description : mediaDescriptions) {
                MediaDescription mediaDescription = (MediaDescription) description;
                Media media = mediaDescription.getMedia();

                Vector mediaFormats = media.getMediaFormats(false);
                if (mediaFormats.contains("8") || mediaFormats.contains("0")) {
                    port = media.getMediaPort();
                    break;
                }
            }
            log.info("[TCP主动连接对方] serverGbId: {}, channelId: {}, 连接对方的地址：{}:{}, SSRC: {}, SSRC校验：{}",
                    platform.getServerGBId(), channelId, sdp.getConnection().getAddress(), port, ssrcInfo.getSsrc(), ssrcCheck);
            Boolean result = mediaServerService.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getStream());
            log.info("[TCP主动连接对方] 结果： {}", result);
        } catch (SdpException e) {
            log.error("[TCP主动连接对方] serverGbId: {}, channelId: {}, 解析200OK的SDP信息失败", platform.getServerGBId(), channelId, e);
            dynamicTask.stop(timeOutTaskKey);
            mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

            streamSession.remove(platform.getServerGBId(), channelId, ssrcInfo.getStream());

            callback.run(InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
            inviteStreamService.call(InviteSessionType.PLAY, platform.getServerGBId(), channelId, null,
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
        }
    }

    @Override
    public void stopBroadcast(Platform platform, CommonGBChannel channel, String stream, boolean sendBye, MediaServer mediaServerItem) {

        try {
            if (sendBye) {
                commanderForPlatform.streamByeCmd(platform, channel.getGbDeviceId(), stream, null, null);
            }
        } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
            log.warn("[消息发送失败] 停止语音对讲， 平台：{}，通道：{}", platform.getId(), channel.getGbDeviceId() );
        } finally {
            mediaServerService.closeRTPServer(mediaServerItem, stream);
            InviteInfo inviteInfo = inviteStreamService.getInviteInfo(null, platform.getServerGBId(), channel.getGbDeviceId(), stream);
            if (inviteInfo != null) {
                // 释放ssrc
                mediaServerService.releaseSsrc(mediaServerItem.getId(), inviteInfo.getSsrcInfo().getSsrc());
                inviteStreamService.removeInviteInfo(inviteInfo);
            }
            streamSession.remove(platform.getServerGBId(), channel.getGbDeviceId(), stream);
        }
    }

    @Override
    public Platform queryOne(Integer platformId) {
        return platformMapper.query(platformId);
    }

    @Override
    public List<Platform> queryEnablePlatformList() {
        return platformMapper.queryEnablePlatformList();
    }
}
