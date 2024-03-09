package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.IStreamSendManager;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlatformChannelService;
import com.genersoft.iot.vmp.service.IPlatformService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.*;

/**
 * @author lin
 */
@Service
@DS("master")
public class PlatformServiceImpl implements IPlatformService {

    private final static String REGISTER_KEY_PREFIX = "platform_register_";

    private final static String REGISTER_FAIL_AGAIN_KEY_PREFIX = "platform_register_fail_again_";
    private final static String KEEPALIVE_KEY_PREFIX = "platform_keepalive_";

    private final static Logger logger = LoggerFactory.getLogger(PlatformServiceImpl.class);

    @Autowired
    private ParentPlatformMapper platformMapper;

    @Autowired
    private  CommonChannelMapper commonChannelMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IStreamSendManager streamSendManager;
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
    private UserSetting userSetting;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private ZlmHttpHookSubscribe subscribe;

    @Autowired
    private VideoStreamSessionManager streamSession;


    @Autowired
    private IPlayService playService;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;


    @Override
    public ParentPlatform queryPlatformByServerGBId(String platformGbId) {
        return platformMapper.getParentPlatByServerGBId(platformGbId);
    }

    @Override
    public PageInfo<ParentPlatform> queryParentPlatformList(int page, int count, String query, Boolean online, Boolean enable) {
        PageHelper.startPage(page, count);
        List<ParentPlatform> all = platformMapper.getParentPlatformList(query, online, enable);
        PageInfo<ParentPlatform> platformPageInfo = new PageInfo<>(all);
        int allCount = commonChannelMapper.getAllCount();
        platformPageInfo.getList().stream().forEach(parentPlatform -> {
            if (parentPlatform.isShareAllChannel()) {
                parentPlatform.setChannelCount(allCount);
            }
        });
        return platformPageInfo;
    }

    @Override
    public boolean add(ParentPlatform parentPlatform) {

        if (parentPlatform.getCatalogGroup() == 0) {
            // 每次发送目录的数量默认为1
            parentPlatform.setCatalogGroup(1);
        }
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
            if (parentPlatformOld.isStatus() && parentPlatformCatchOld != null) {
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
                                if (eventResult.type != SipSubscribe.EventResultType.timeout) {
                                    logger.warn("[国标级联]发送心跳收到错误，code： {}, msg: {}", eventResult.statusCode, eventResult.msg);
                                }
                                // 心跳失败
                                ParentPlatformCatch platformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId());
                                // 此时是第三次心跳超时， 平台离线
                                if (platformCatch.getKeepAliveReply()  == 2) {
                                    // 设置平台离线，并重新注册
                                    logger.info("[国标级联] 三次心跳失败, 平台{}({})离线", parentPlatform.getName(), parentPlatform.getServerGBId());
                                    offline(parentPlatform, false);
                                }else {
                                    platformCatch.setKeepAliveReply(platformCatch.getKeepAliveReply() + 1);
                                    redisCatchStorage.updatePlatformCatchInfo(platformCatch);
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
            if (subscribeHolder.getCatalogSubscribe(parentPlatform.getId()) == null) {
                logger.info("[国标级联]：{}, 添加自动通道推送模拟订阅信息", parentPlatform.getServerGBId());
                addSimulatedSubscribeInfo(parentPlatform);
            }
        }else {
            SubscribeInfo catalogSubscribe = subscribeHolder.getCatalogSubscribe(parentPlatform.getId());
            if (catalogSubscribe != null && catalogSubscribe.getExpires() == -1) {
                subscribeHolder.removeCatalogSubscribe(parentPlatform.getId());
            }
        }
    }

    @Override
    public void addSimulatedSubscribeInfo(ParentPlatform parentPlatform) {
        // 自动添加一条模拟的订阅信息
        SubscribeInfo subscribeInfo = SipUtils.buildVirtuallyCatalogSubSubscribe(parentPlatform);
        subscribeHolder.putCatalogSubscribe(parentPlatform.getId(), subscribeInfo);
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
        // 停止订阅回复
        SubscribeInfo catalogSubscribe = subscribeHolder.getCatalogSubscribe(parentPlatform.getId());
        if (catalogSubscribe != null) {
            if (catalogSubscribe.getExpires() > 0) {
                logger.info("[平台离线] {}, 停止目录订阅回复", parentPlatform.getServerGBId());
                subscribeHolder.removeCatalogSubscribe(parentPlatform.getId());
            }
        }
        logger.info("[平台离线] {}, 停止移动位置订阅回复", parentPlatform.getServerGBId());
        subscribeHolder.removeMobilePositionSubscribe(parentPlatform.getId());
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
        List<SendRtpItem> sendRtpItems = streamSendManager.getByDestId(platformId);

        if (sendRtpItems != null && !sendRtpItems.isEmpty()) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                ssrcFactory.releaseSsrc(sendRtpItem.getMediaServerId(), sendRtpItem.getSsrc());
                streamSendManager.remove(sendRtpItem);
                MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                Map<String, Object> param = new HashMap<>(3);
                param.put("vhost", "__defaultVhost__");
                param.put("app", sendRtpItem.getApp());
                param.put("stream", sendRtpItem.getStream());
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
    public void sendNotifyMobilePosition(Integer platformId) {
        ParentPlatform platform = platformMapper.getParentPlatById(platformId);
        if (platform == null) {
            return;
        }
        SubscribeInfo subscribe = subscribeHolder.getMobilePositionSubscribe(platform.getId());
        if (subscribe != null) {

            List<CommonGbChannel> channelList = platformChannelService.queryCommonGbChannellList(platform.getId());
            if (channelList.isEmpty()) {
                return;
            }
            for (CommonGbChannel channel : channelList) {
                GPSMsgInfo gpsMsgInfo = redisCatchStorage.getGpsMsgInfo(channel.getCommonGbDeviceID());
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

    @Override
    public boolean delete(String serverGBId) {
        ParentPlatform parentPlatform = platformMapper.getParentPlatByServerGBId(serverGBId);
        ParentPlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(serverGBId);
        if (parentPlatform == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "平台不存在");
        }
        // TODO 发送取消订阅的消息，等待市场检验后确定需要再添加此项，暂时记录
        //  可通过发送subscription-state 头域为terminated 的 NOTIFY 消息主动结束订阅, NOTIFY消息体可为空,
        //  订阅方接收到该消息后回复200 OK 响应。
        // 停止推流
        stopAllPush(parentPlatform.getServerGBId());
        // 停止发送位置订阅定时任务
        String sendMobilePositionTaskKey = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX + userSetting.getServerId() +  "_MobilePosition_" + parentPlatform.getServerGBId();
        if (dynamicTask.contains(sendMobilePositionTaskKey)) {
            dynamicTask.stop(sendMobilePositionTaskKey);
        }
        // 停止注册
        final String registerTaskKey = REGISTER_KEY_PREFIX + parentPlatform.getServerGBId();
        if (dynamicTask.contains(registerTaskKey)) {
            dynamicTask.stop(registerTaskKey);
        }
        // 清除定时心跳
        final String keepaliveTaskKey = KEEPALIVE_KEY_PREFIX + parentPlatform.getServerGBId();
        if (dynamicTask.contains(keepaliveTaskKey)) {
            dynamicTask.stop(keepaliveTaskKey);
        }
        // 删除缓存的订阅信息
        subscribeHolder.removeAllSubscribe(parentPlatform.getId());
        parentPlatform.setEnable(false);
        update(parentPlatform);
        // 发送注销的请求
        if (parentPlatformCatch != null && parentPlatformCatch.getSipTransactionInfo() != null) {
            // 发送离线消息,无论是否成功都删除缓存
            try {
                commanderForPlatform.unregister(parentPlatform, parentPlatformCatch.getSipTransactionInfo(), null, null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                logger.error("[命令发送失败] 国标级联 注销: {}", e.getMessage());
            } finally {
                // 清空redis缓存
                redisCatchStorage.delPlatformCatchInfo(parentPlatform.getServerGBId());
                redisCatchStorage.delPlatformKeepalive(parentPlatform.getServerGBId());
                redisCatchStorage.delPlatformRegister(parentPlatform.getServerGBId());
            }
        }
        return platformMapper.delParentPlatform(parentPlatform) > 0;
    }

    @Override
    public ParentPlatform query(Integer platformId) {
        return platformMapper.getParentPlatById(platformId);
    }

    @Override
    public List<ParentPlatform> queryAllWithShareAll() {
        return platformMapper.queryAllWithShareAll();
    }

    @Override
    public List<ParentPlatform> querySharePlatform(List<CommonGbChannel> channel, List<Integer> platformIdList) {
        return platformMapper.querySharePlatform(channel, platformIdList);
    }

    @Override
    public void broadcastInvite(ParentPlatform platform, String channelId, MediaServerItem mediaServerItem, ZlmHttpHookSubscribe.Event hookEvent,
                                SipSubscribe.Event errorEvent, InviteTimeOutCallback timeoutCallback) throws InvalidArgumentException, ParseException, SipException {

        if (mediaServerItem == null) {
            logger.info("[国标级联] 语音喊话未找到可用的zlm. platform: {}", platform.getServerGBId());
            return;
        }
        InviteInfo inviteInfoForOld = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, platform.getServerGBId(), channelId);

        if (inviteInfoForOld != null && inviteInfoForOld.getStreamInfo() != null) {
            // 如果zlm不存在这个流，则删除数据即可
            MediaServerItem mediaServerItemForStreamInfo = mediaServerService.getOne(inviteInfoForOld.getStreamInfo().getMediaServerId());
            if (mediaServerItemForStreamInfo != null) {
                Boolean ready = zlmServerFactory.isStreamReady(mediaServerItemForStreamInfo, inviteInfoForOld.getStreamInfo().getApp(), inviteInfoForOld.getStreamInfo().getStream());
                if (!ready) {
                    // 错误存在于redis中的数据
                    inviteStreamService.removeInviteInfo(inviteInfoForOld);
                }else {
                    // 流确实尚在推流，直接回调结果
                    OnStreamChangedHookParam hookParam = new OnStreamChangedHookParam();
                    hookParam.setApp(inviteInfoForOld.getStreamInfo().getApp());
                    hookParam.setStream(inviteInfoForOld.getStreamInfo().getStream());

                    hookEvent.response(mediaServerItemForStreamInfo, hookParam);
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
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, null, ssrcCheck, false, null, true, false, tcpMode);
        if (ssrcInfo == null || ssrcInfo.getPort() < 0) {
            logger.info("[国标级联] 发起语音喊话 开启端口监听失败， platform: {}, channel： {}", platform.getServerGBId(), channelId);
            SipSubscribe.EventResult<Object> eventResult = new SipSubscribe.EventResult<>();
            eventResult.statusCode = -1;
            eventResult.msg = "端口监听失败";
            eventResult.type = SipSubscribe.EventResultType.failedToGetPort;
            errorEvent.response(eventResult);
            return;
        }
        logger.info("[国标级联] 语音喊话，发起Invite消息 deviceId: {}, channelId: {},收流端口： {}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
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
                logger.info("[国标级联] 发起语音喊话 收流超时 deviceId: {}, channelId: {}，端口：{}, SSRC: {}", platform.getServerGBId(), channelId, ssrcInfo.getPort(), ssrcInfo.getSsrc());
                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                try {
                    commanderForPlatform.streamByeCmd(platform, channelId, ssrcInfo.getStream(), null, null);
                } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                    logger.error("[点播超时]， 发送BYE失败 {}", e.getMessage());
                } finally {
                    timeoutCallback.run(1, "收流超时");
                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                    streamSession.remove(platform.getServerGBId(), channelId, ssrcInfo.getStream());
                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                }
            }
        }, userSetting.getPlayTimeout());
        commanderForPlatform.broadcastInviteCmd(platform, channelId, mediaServerItem, ssrcInfo, (mediaServerItemForInvite, hookParam)->{
            logger.info("[国标级联] 发起语音喊话 收到上级推流 deviceId: {}, channelId: {}", platform.getServerGBId(), channelId);
            dynamicTask.stop(timeOutTaskKey);
            // hook响应
            playService.onPublishHandlerForPlay(mediaServerItemForInvite, hookParam, platform.getServerGBId(), channelId);
            // 收到流
            if (hookEvent != null) {
                hookEvent.response(mediaServerItem, hookParam);
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

    private void inviteOKHandler(SipSubscribe.EventResult eventResult, SSRCInfo ssrcInfo, int tcpMode, boolean ssrcCheck, MediaServerItem mediaServerItem,
                                 ParentPlatform platform, String channelId, String timeOutTaskKey, ErrorCallback<Object> callback,
                                 InviteInfo inviteInfo, InviteSessionType inviteSessionType){
        inviteInfo.setStatus(InviteSessionStatus.ok);
        ResponseEvent responseEvent = (ResponseEvent) eventResult.event;
        String contentString = new String(responseEvent.getResponse().getRawContent());
        System.out.println(1111);
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
                    logger.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                }
            }
        }else {
            logger.info("[Invite 200OK] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
            // ssrc 不一致
            if (mediaServerItem.isRtpEnable()) {
                // 多端口
                if (ssrcCheck) {
                    // ssrc检验
                    // 更新ssrc
                    logger.info("[Invite 200OK] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
                    // 释放ssrc
                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                    Boolean result = mediaServerService.updateRtpServerSSRC(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse);
                    if (!result) {
                        try {
                            logger.warn("[Invite 200OK] 更新ssrc失败，停止喊话 {}/{}", platform.getServerGBId(), channelId);
                            commanderForPlatform.streamByeCmd(platform, channelId, ssrcInfo.getStream(), null, null);
                        } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                            logger.error("[命令发送失败] 停止播放， 发送BYE: {}", e.getMessage());
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
                                logger.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
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
                            logger.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
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


    private void tcpActiveHandler(ParentPlatform platform, String channelId, String contentString,
                                  MediaServerItem mediaServerItem, int tcpMode, boolean ssrcCheck,
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
            logger.info("[TCP主动连接对方] serverGbId: {}, channelId: {}, 连接对方的地址：{}:{}, SSRC: {}, SSRC校验：{}",
                    platform.getServerGBId(), channelId, sdp.getConnection().getAddress(), port, ssrcInfo.getSsrc(), ssrcCheck);
            JSONObject jsonObject = zlmresTfulUtils.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getStream());
            logger.info("[TCP主动连接对方] 结果： {}", jsonObject);
        } catch (SdpException e) {
            logger.error("[TCP主动连接对方] serverGbId: {}, channelId: {}, 解析200OK的SDP信息失败", platform.getServerGBId(), channelId, e);
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
    public void stopBroadcast(ParentPlatform platform, DeviceChannel channel, String stream, boolean sendBye, MediaServerItem mediaServerItem) {

        try {
            if (sendBye) {
                commanderForPlatform.streamByeCmd(platform, channel.getChannelId(), stream, null, null);
            }
        } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
            logger.warn("[消息发送失败] 停止语音对讲， 平台：{}，通道：{}", platform.getId(), channel.getChannelId() );
        } finally {
            mediaServerService.closeRTPServer(mediaServerItem, stream);
            InviteInfo inviteInfo = inviteStreamService.getInviteInfo(null, platform.getServerGBId(), channel.getChannelId(), stream);
            if (inviteInfo != null) {
                // 释放ssrc
                mediaServerService.releaseSsrc(mediaServerItem.getId(), inviteInfo.getSsrcInfo().getSsrc());
                inviteStreamService.removeInviteInfo(inviteInfo);
            }
            streamSession.remove(platform.getServerGBId(), channel.getChannelId(), stream);
        }
    }
}
