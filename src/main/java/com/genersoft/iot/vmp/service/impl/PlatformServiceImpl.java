package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlatformService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.ParentPlatformMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.TimeoutEvent;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lin
 */
@Service
public class PlatformServiceImpl implements IPlatformService {

    private final static String REGISTER_KEY_PREFIX = "platform_register_";
    private final static String KEEPALIVE_KEY_PREFIX = "platform_keepalive_";

    private final static Logger logger = LoggerFactory.getLogger(PlatformServiceImpl.class);

    @Autowired
    private ParentPlatformMapper platformMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private SIPCommanderFroPlatform commanderForPlatform;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ZLMRTPServerFactory zlmrtpServerFactory;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private UserSetting userSetting;



    @Override
    public ParentPlatform queryPlatformByServerGBId(String platformGbId) {
        return platformMapper.getParentPlatByServerGBId(platformGbId);
    }

    @Override
    public PageInfo<ParentPlatform> queryParentPlatformList(int page, int count) {
        PageHelper.startPage(page, count);
        List<ParentPlatform> all = platformMapper.getParentPlatformList();
        return new PageInfo<>(all);
    }

    @Override
    public boolean add(ParentPlatform parentPlatform) {

        if (parentPlatform.getCatalogGroup() == 0) {
            // 每次发送目录的数量默认为1
            parentPlatform.setCatalogGroup(1);
        }
        if (parentPlatform.getAdministrativeDivision() == null) {
            // 行政区划默认去编号的前6位
            parentPlatform.setAdministrativeDivision(parentPlatform.getServerGBId().substring(0,6));
        }
        parentPlatform.setCatalogId(parentPlatform.getDeviceGBId());
        int result = platformMapper.addParentPlatform(parentPlatform);
        // 添加缓存
        ParentPlatformCatch parentPlatformCatch = new ParentPlatformCatch();
        parentPlatformCatch.setParentPlatform(parentPlatform);
        parentPlatformCatch.setId(parentPlatform.getServerGBId());
        parentPlatformCatch.setParentPlatform(parentPlatform);
        redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
        if (parentPlatform.isEnable()) {
            // 保存时启用就发送注册
            // 注册成功时由程序直接调用了online方法
            try {
                commanderForPlatform.register(parentPlatform, eventResult -> {
                    logger.info("[国标级联] {},添加向上级注册失败，请确定上级平台可用时重新保存", parentPlatform.getServerGBId());
                }, null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                logger.error("[命令发送失败] 国标级联: {}", e.getMessage());
            }
        }
        return result > 0;
    }

    @Override
    public void online(ParentPlatform parentPlatform) {
        logger.info("[国标级联]：{}, 平台上线/更新注册", parentPlatform.getServerGBId());
        platformMapper.updateParentPlatformStatus(parentPlatform.getServerGBId(), true);
        ParentPlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId());
        if (parentPlatformCatch != null) {
            parentPlatformCatch.getParentPlatform().setStatus(true);
            redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
        }else {
            parentPlatformCatch = new ParentPlatformCatch();
            parentPlatformCatch.setParentPlatform(parentPlatform);
            parentPlatformCatch.setId(parentPlatform.getServerGBId());
            parentPlatform.setStatus(true);
            parentPlatformCatch.setParentPlatform(parentPlatform);
            redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
        }

        final String registerTaskKey = REGISTER_KEY_PREFIX + parentPlatform.getServerGBId();
        if (dynamicTask.contains(registerTaskKey)) {
            dynamicTask.stop(registerTaskKey);
        }
        // 添加注册任务
        dynamicTask.startDelay(registerTaskKey,
                // 注册失败（注册成功时由程序直接调用了online方法）
                ()-> {
                    try {
                        commanderForPlatform.register(parentPlatform, eventResult -> offline(parentPlatform),null);
                    } catch (InvalidArgumentException | ParseException | SipException e) {
                        logger.error("[命令发送失败] 国标级联定时注册: {}", e.getMessage());
                    }
                },
                (parentPlatform.getExpires() - 10) *1000);

        final String keepaliveTaskKey = KEEPALIVE_KEY_PREFIX + parentPlatform.getServerGBId();
        if (!dynamicTask.contains(keepaliveTaskKey)) {
            // 添加心跳任务
            dynamicTask.startCron(keepaliveTaskKey,
                    ()-> {
                        try {
                            commanderForPlatform.keepalive(parentPlatform, eventResult -> {
                                // 心跳失败
                                if (eventResult.type == SipSubscribe.EventResultType.timeout) {
                                    // 心跳超时
                                    ParentPlatformCatch platformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId());
                                    // 此时是第三次心跳超时， 平台离线
                                    if (platformCatch.getKeepAliveReply()  == 2) {
                                        // 设置平台离线，并重新注册
                                        offline(parentPlatform);
                                        logger.info("[国标级联] {}，三次心跳超时后再次发起注册", parentPlatform.getServerGBId());
                                        try {
                                            commanderForPlatform.register(parentPlatform, eventResult1 -> {
                                                logger.info("[国标级联] {}，三次心跳超时后再次发起注册仍然失败，开始定时发起注册，间隔为1分钟", parentPlatform.getServerGBId());
                                                // 添加注册任务
                                                dynamicTask.startCron(registerTaskKey,
                                                        // 注册失败（注册成功时由程序直接调用了online方法）
                                                        ()->logger.info("[国标级联] {},平台离线后持续发起注册，失败", parentPlatform.getServerGBId()),
                                                        60*1000);
                                            }, null);
                                        } catch (InvalidArgumentException | ParseException | SipException e) {
                                            logger.error("[命令发送失败] 国标级联 注册: {}", e.getMessage());
                                        }
                                    }

                                }else {
                                    logger.warn("[国标级联]发送心跳收到错误，code： {}, msg: {}", eventResult.statusCode, eventResult.msg);
                                }

                            }, eventResult -> {
                                // 心跳成功
                                // 清空之前的心跳超时计数
                                ParentPlatformCatch platformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId());
                                if (platformCatch.getKeepAliveReply() > 0) {
                                    platformCatch.setKeepAliveReply(0);
                                    redisCatchStorage.updatePlatformCatchInfo(platformCatch);
                                }
                            });
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            logger.error("[命令发送失败] 国标级联 发送心跳: {}", e.getMessage());
                        }
                    },
                    (parentPlatform.getKeepTimeout() - 10)*1000);
        }
    }

    @Override
    public void offline(ParentPlatform parentPlatform) {
        logger.info("[平台离线]：{}", parentPlatform.getServerGBId());
        ParentPlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId());
        parentPlatformCatch.setKeepAliveReply(0);
        parentPlatformCatch.setRegisterAliveReply(0);
        ParentPlatform parentPlatformInCatch = parentPlatformCatch.getParentPlatform();
        parentPlatformInCatch.setStatus(false);
        parentPlatformCatch.setParentPlatform(parentPlatformInCatch);
        redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
        platformMapper.updateParentPlatformStatus(parentPlatform.getServerGBId(), false);

        // 停止所有推流
        logger.info("[平台离线] {}, 停止所有推流", parentPlatform.getServerGBId());
        stopAllPush(parentPlatform.getServerGBId());
        // 清除注册定时
        logger.info("[平台离线] {}, 停止定时注册任务", parentPlatform.getServerGBId());
        final String registerTaskKey = REGISTER_KEY_PREFIX + parentPlatform.getServerGBId();
        if (dynamicTask.contains(registerTaskKey)) {
            dynamicTask.stop(registerTaskKey);
        }
        // 清除心跳定时
        logger.info("[平台离线] {}, 停止定时发送心跳任务", parentPlatform.getServerGBId());
        final String keepaliveTaskKey = KEEPALIVE_KEY_PREFIX + parentPlatform.getServerGBId();
        if (dynamicTask.contains(keepaliveTaskKey)) {
            // 添加心跳任务
            dynamicTask.stop(keepaliveTaskKey);
        }
        // 停止目录订阅回复
        logger.info("[平台离线] {}, 停止订阅回复", parentPlatform.getServerGBId());
        subscribeHolder.removeAllSubscribe(parentPlatform.getServerGBId());
    }

    private void stopAllPush(String platformId) {
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServer(platformId);
        if (sendRtpItems != null && sendRtpItems.size() > 0) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                redisCatchStorage.deleteSendRTPServer(platformId, sendRtpItem.getChannelId(), null, null);
                MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                Map<String, Object> param = new HashMap<>(3);
                param.put("vhost", "__defaultVhost__");
                param.put("app", sendRtpItem.getApp());
                param.put("stream", sendRtpItem.getStreamId());
                zlmrtpServerFactory.stopSendRtpStream(mediaInfo, param);
            }
        }
    }

    @Override
    public void login(ParentPlatform parentPlatform) {
        final String registerTaskKey = REGISTER_KEY_PREFIX + parentPlatform.getServerGBId();
        try {
            commanderForPlatform.register(parentPlatform, eventResult1 -> {
                logger.info("[国标级联] {}，开始定时发起注册，间隔为1分钟", parentPlatform.getServerGBId());
                // 添加注册任务
                dynamicTask.startCron(registerTaskKey,
                        // 注册失败（注册成功时由程序直接调用了online方法）
                        ()->logger.info("[国标级联] {},平台离线后持续发起注册，失败", parentPlatform.getServerGBId()),
                        60*1000);
            }, null);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            logger.error("[命令发送失败] 国标级联注册: {}", e.getMessage());
        }
    }

    @Override
    public void sendNotifyMobilePosition(String platformId) {
        ParentPlatform platform = platformMapper.getParentPlatByServerGBId(platformId);
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
                String gbId = deviceChannel.getChannelId();
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
                        logger.error("[命令发送失败] 国标级联 移动位置通知: {}", e.getMessage());
                    }
                }
            }
        }
    }
}
