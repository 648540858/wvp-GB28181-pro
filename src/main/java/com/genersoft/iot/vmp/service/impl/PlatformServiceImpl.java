package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlatformService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import gov.nist.javax.sip.message.SIPRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.*;

/**
 * @author lin
 */
@Service
public class PlatformServiceImpl implements IPlatformService {

    private final static String REGISTER_KEY_PREFIX = "platform_register_";

    private final static String REGISTER_FAIL_AGAIN_KEY_PREFIX = "platform_register_fail_again_";
    private final static String KEEPALIVE_KEY_PREFIX = "platform_keepalive_";

    private final static Logger logger = LoggerFactory.getLogger(PlatformServiceImpl.class);

    @Autowired
    private ParentPlatformMapper platformMapper;

    @Autowired
    private PlatformCatalogMapper catalogMapper;

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private SIPCommanderFroPlatform commanderForPlatform;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ZLMServerFactory zlmServerFactory;

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
    public boolean update(ParentPlatform parentPlatform) {
        logger.info("[国标级联]更新平台 {}", parentPlatform.getDeviceGBId());
        parentPlatform.setCharacterSet(parentPlatform.getCharacterSet().toUpperCase());
        ParentPlatform parentPlatformOld = platformMapper.getParentPlatById(parentPlatform.getId());
        ParentPlatformCatch parentPlatformCatchOld = redisCatchStorage.queryPlatformCatchInfo(parentPlatformOld.getServerGBId());
        parentPlatform.setUpdateTime(DateUtil.getNow());

        // 停止心跳定时
        final String keepaliveTaskKey = KEEPALIVE_KEY_PREFIX + parentPlatformOld.getServerGBId();
        dynamicTask.stop(keepaliveTaskKey);
        // 停止注册定时
        final String registerTaskKey = REGISTER_KEY_PREFIX + parentPlatformOld.getServerGBId();
        dynamicTask.stop(registerTaskKey);
        // 注销旧的
        try {
            if (parentPlatformOld.isStatus()) {
                logger.info("保存平台{}时发现旧平台在线，发送注销命令", parentPlatformOld.getServerGBId());
                commanderForPlatform.unregister(parentPlatformOld, parentPlatformCatchOld.getSipTransactionInfo(), null, eventResult -> {
                    logger.info("[国标级联] 注销成功， 平台：{}", parentPlatformOld.getServerGBId());
                });
            }
        } catch (InvalidArgumentException | ParseException | SipException e) {
            logger.error("[命令发送失败] 国标级联 注销: {}", e.getMessage());
        }

        // 更新数据库
        if (parentPlatform.getCatalogGroup() == 0) {
            parentPlatform.setCatalogGroup(1);
        }
        if (parentPlatform.getAdministrativeDivision() == null) {
            parentPlatform.setAdministrativeDivision(parentPlatform.getAdministrativeDivision());
        }

        platformMapper.updateParentPlatform(parentPlatform);
        // 更新redis
        redisCatchStorage.delPlatformCatchInfo(parentPlatformOld.getServerGBId());
        ParentPlatformCatch parentPlatformCatch = new ParentPlatformCatch();
        parentPlatformCatch.setParentPlatform(parentPlatform);
        parentPlatformCatch.setId(parentPlatform.getServerGBId());
        redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
        // 注册
        if (parentPlatform.isEnable()) {
            // 保存时启用就发送注册
            // 注册成功时由程序直接调用了online方法
            try {
                logger.info("[国标级联] 平台注册 {}", parentPlatform.getDeviceGBId());
                commanderForPlatform.register(parentPlatform, eventResult -> {
                    logger.info("[国标级联] {},添加向上级注册失败，请确定上级平台可用时重新保存", parentPlatform.getServerGBId());
                }, null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                logger.error("[命令发送失败] 国标级联: {}", e.getMessage());
            }
        }


        return false;
    }


    @Override
    public void online(ParentPlatform parentPlatform, SipTransactionInfo sipTransactionInfo) {
        logger.info("[国标级联]：{}, 平台上线", parentPlatform.getServerGBId());
        final String registerFailAgainTaskKey = REGISTER_FAIL_AGAIN_KEY_PREFIX + parentPlatform.getServerGBId();
        dynamicTask.stop(registerFailAgainTaskKey);

        platformMapper.updateParentPlatformStatus(parentPlatform.getServerGBId(), true);
        ParentPlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId());
        if (parentPlatformCatch == null) {
            parentPlatformCatch = new ParentPlatformCatch();
            parentPlatformCatch.setParentPlatform(parentPlatform);
            parentPlatformCatch.setId(parentPlatform.getServerGBId());
            parentPlatform.setStatus(true);
            parentPlatformCatch.setParentPlatform(parentPlatform);
        }

        parentPlatformCatch.getParentPlatform().setStatus(true);
        parentPlatformCatch.setSipTransactionInfo(sipTransactionInfo);
        redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);

        final String registerTaskKey = REGISTER_KEY_PREFIX + parentPlatform.getServerGBId();
        if (!dynamicTask.isAlive(registerTaskKey)) {
            logger.info("[国标级联]：{}, 添加定时注册任务", parentPlatform.getServerGBId());
            // 添加注册任务
            dynamicTask.startCron(registerTaskKey,
                // 注册失败（注册成功时由程序直接调用了online方法）
                ()-> registerTask(parentPlatform, sipTransactionInfo),
                    parentPlatform.getExpires() * 1000);
        }


        final String keepaliveTaskKey = KEEPALIVE_KEY_PREFIX + parentPlatform.getServerGBId();
        if (!dynamicTask.contains(keepaliveTaskKey)) {
            logger.info("[国标级联]：{}, 添加定时心跳任务", parentPlatform.getServerGBId());
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
                                        logger.info("[国标级联] 三次心跳超时, 平台{}({})离线", parentPlatform.getName(), parentPlatform.getServerGBId());
                                        offline(parentPlatform, false);
                                    }

                                }else {
                                    logger.warn("[国标级联]发送心跳收到错误，code： {}, msg: {}", eventResult.statusCode, eventResult.msg);
                                }

                            }, eventResult -> {
                                // 心跳成功
                                // 清空之前的心跳超时计数
                                ParentPlatformCatch platformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId());
                                if (platformCatch != null && platformCatch.getKeepAliveReply() > 0) {
                                    platformCatch.setKeepAliveReply(0);
                                    redisCatchStorage.updatePlatformCatchInfo(platformCatch);
                                }
                                logger.info("[发送心跳] 国标级联 发送心跳, code： {}, msg: {}", eventResult.statusCode, eventResult.msg);
                            });
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            logger.error("[命令发送失败] 国标级联 发送心跳: {}", e.getMessage());
                        }
                    },
                    (parentPlatform.getKeepTimeout())*1000);
        }
        if (parentPlatform.isAutoPushChannel()) {
            if (subscribeHolder.getCatalogSubscribe(parentPlatform.getServerGBId()) == null) {
                addSimulatedSubscribeInfo(parentPlatform);
            }
        }else {
            SubscribeInfo catalogSubscribe = subscribeHolder.getCatalogSubscribe(parentPlatform.getServerGBId());
            if (catalogSubscribe != null && catalogSubscribe.getExpires() == -1) {
                subscribeHolder.removeCatalogSubscribe(parentPlatform.getServerGBId());
            }
        }
    }

    @Override
    public void addSimulatedSubscribeInfo(ParentPlatform parentPlatform) {
        // 自动添加一条模拟的订阅信息
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setId(parentPlatform.getServerGBId());
        subscribeInfo.setExpires(-1);
        subscribeInfo.setEventType("Catalog");
        int random = (int) Math.floor(Math.random() * 10000);
        subscribeInfo.setEventId(random + "");
        subscribeInfo.setSimulatedCallId(UUID.randomUUID().toString().replace("-", "") + "@" + parentPlatform.getServerIP());
        subscribeInfo.setSimulatedFromTag(UUID.randomUUID().toString().replace("-", ""));
        subscribeInfo.setSimulatedToTag(UUID.randomUUID().toString().replace("-", ""));
        subscribeHolder.putCatalogSubscribe(parentPlatform.getServerGBId(), subscribeInfo);
    }

    private void registerTask(ParentPlatform parentPlatform, SipTransactionInfo sipTransactionInfo){
        try {
            // 不在同一个会话中续订则每次全新注册
            if (!userSetting.isRegisterKeepIntDialog()) {
                sipTransactionInfo = null;
            }

            if (sipTransactionInfo == null) {
                logger.info("[国标级联] 平台：{}注册即将到期，开始重新注册", parentPlatform.getServerGBId());
            }else {
                logger.info("[国标级联] 平台：{}注册即将到期，开始续订", parentPlatform.getServerGBId());
            }

            commanderForPlatform.register(parentPlatform, sipTransactionInfo,  eventResult -> {
                logger.info("[国标级联] 平台：{}注册失败，{}:{}", parentPlatform.getServerGBId(),
                        eventResult.statusCode, eventResult.msg);
                offline(parentPlatform, false);
            }, null);
        } catch (Exception e) {
            logger.error("[命令发送失败] 国标级联定时注册: {}", e.getMessage());
        }
    }

    @Override
    public void offline(ParentPlatform parentPlatform, boolean stopRegister) {
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
            // 清除心跳任务
            dynamicTask.stop(keepaliveTaskKey);
        }
        // 停止目录订阅回复
        logger.info("[平台离线] {}, 停止订阅回复", parentPlatform.getServerGBId());
        subscribeHolder.removeAllSubscribe(parentPlatform.getServerGBId());
        // 发起定时自动重新注册
        if (!stopRegister) {
            // 设置为60秒自动尝试重新注册
            final String registerFailAgainTaskKey = REGISTER_FAIL_AGAIN_KEY_PREFIX + parentPlatform.getServerGBId();
            ParentPlatform platform = platformMapper.getParentPlatById(parentPlatform.getId());
            if (platform.isEnable()) {
                dynamicTask.startCron(registerFailAgainTaskKey,
                        ()-> registerTask(platform, null),
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
                MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                Map<String, Object> param = new HashMap<>(3);
                param.put("vhost", "__defaultVhost__");
                param.put("app", sendRtpItem.getApp());
                param.put("stream", sendRtpItem.getStreamId());
                zlmServerFactory.stopSendRtpStream(mediaInfo, param);
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
