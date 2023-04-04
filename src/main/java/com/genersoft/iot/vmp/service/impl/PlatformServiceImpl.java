package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlatformService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.InviteTimeOutCallback;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private PlatformCatalogMapper catalogMapper;

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

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

    @Autowired
    private ZlmHttpHookSubscribe subscribe;

    @Autowired
    private VideoStreamSessionManager streamSession;


    @Autowired
    private IPlayService playService;



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
        if (!parentPlatformOld.getTreeType().equals(parentPlatform.getTreeType())) {
            // 目录结构发生变化，清空之前的关联关系
            logger.info("保存平台{}时发现目录结构变化，清空关联关系", parentPlatform.getDeviceGBId());
            catalogMapper.delByPlatformId(parentPlatformOld.getServerGBId());
            platformChannelMapper.delByPlatformId(parentPlatformOld.getServerGBId());
            platformGbStreamMapper.delByPlatformId(parentPlatformOld.getServerGBId());
        }


        // 停止心跳定时
        final String keepaliveTaskKey = KEEPALIVE_KEY_PREFIX + parentPlatformOld.getServerGBId();
        dynamicTask.stop(keepaliveTaskKey);
        // 停止注册定时
        final String registerTaskKey = REGISTER_KEY_PREFIX + parentPlatformOld.getServerGBId();
        dynamicTask.stop(registerTaskKey);
        // 注销旧的
        try {
            if (parentPlatformOld.isStatus()) {
                logger.info("保存平台{}时发现救平台在线，发送注销命令", parentPlatform.getDeviceGBId());
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
                commanderForPlatform.register(parentPlatform, eventResult -> {
                    logger.info("[国标级联] {},添加向上级注册失败，请确定上级平台可用时重新保存", parentPlatform.getServerGBId());
                }, null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                logger.error("[命令发送失败] 国标级联: {}", e.getMessage());
            }
        }
        // 重新开启定时注册， 使用续订消息
        // 重新开始心跳保活


        return false;
    }


    @Override
    public void online(ParentPlatform parentPlatform, SipTransactionInfo sipTransactionInfo) {
        logger.info("[国标级联]：{}, 平台上线", parentPlatform.getServerGBId());
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
                                        logger.info("[国标级联] {}，三次心跳超时后再次发起注册", parentPlatform.getServerGBId());
                                        try {
                                            commanderForPlatform.register(parentPlatform, eventResult1 -> {
                                                logger.info("[国标级联] {}，三次心跳超时后再次发起注册仍然失败，开始定时发起注册，间隔为1分钟", parentPlatform.getServerGBId());
                                                offline(parentPlatform, false);
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
                    (parentPlatform.getKeepTimeout())*1000);
        }
    }

    private void registerTask(ParentPlatform parentPlatform, SipTransactionInfo sipTransactionInfo){
        try {
            // 设置超时重发， 后续从底层支持消息重发
            String key = KEEPALIVE_KEY_PREFIX + parentPlatform.getServerGBId() + "_timeout";
            if (dynamicTask.isAlive(key)) {
                return;
            }
            dynamicTask.startDelay(key, ()->{
                registerTask(parentPlatform, sipTransactionInfo);
            }, 1000);
            logger.info("[国标级联] 平台：{}注册即将到期，开始续订", parentPlatform.getServerGBId());
            commanderForPlatform.register(parentPlatform, sipTransactionInfo,  eventResult -> {
                dynamicTask.stop(key);
                offline(parentPlatform, false);
            },eventResult -> {
                dynamicTask.stop(key);
            });
        } catch (InvalidArgumentException | ParseException | SipException e) {
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
        if (stopRegister) {
            // 清除注册定时
            logger.info("[平台离线] {}, 停止定时注册任务", parentPlatform.getServerGBId());
            final String registerTaskKey = REGISTER_KEY_PREFIX + parentPlatform.getServerGBId();
            if (dynamicTask.contains(registerTaskKey)) {
                dynamicTask.stop(registerTaskKey);
            }
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
                param.put("stream", sendRtpItem.getStream());
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

    @Override
    public void broadcastInvite(ParentPlatform platform, String channelId, MediaServerItem mediaServerItem, ZlmHttpHookSubscribe.Event hookEvent,
                                SipSubscribe.Event errorEvent, InviteTimeOutCallback timeoutCallback) throws InvalidArgumentException, ParseException, SipException {

        if (mediaServerItem == null) {
            logger.info("[国标级联] 语音喊话未找到可用的zlm. platform: {}", platform.getServerGBId());
            return;
        }
        StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(platform.getServerGBId(), channelId);
        if (streamInfo != null) {
            // 如果zlm不存在这个流，则删除数据即可
            MediaServerItem mediaServerItemForStreamInfo = mediaServerService.getOne(streamInfo.getMediaServerId());
            if (mediaServerItemForStreamInfo != null) {
                Boolean ready = zlmrtpServerFactory.isStreamReady(mediaServerItemForStreamInfo, streamInfo.getApp(), streamInfo.getStream());
                if (!ready) {
                    // 错误存在于redis中的数据
                    redisCatchStorage.stopPlay(streamInfo);
                }else {
                    // 流确实尚在推流，直接回调结果
                    JSONObject json = new JSONObject();
                    json.put("app", streamInfo.getApp());
                    json.put("stream", streamInfo.getStream());
                    hookEvent.response(mediaServerItemForStreamInfo, json);
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
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, null, ssrcCheck, false, null, true);
        if (ssrcInfo == null || ssrcInfo.getPort() < 0) {
            logger.info("[国标级联] 发起语音喊话 开启端口监听失败， platform: {}, channel： {}", platform.getServerGBId(), channelId);
            errorEvent.response(new SipSubscribe.EventResult(-1, "端口监听失败"));
            return;
        }
        logger.info("[国标级联] 语音喊话，发起Invite消息 deviceId: {}, channelId: {},收流端口： {}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                platform.getServerGBId(), channelId, ssrcInfo.getPort(), userSetting.getBroadcastForPlatform(), ssrcInfo.getSsrc(), ssrcCheck);

        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            // 执行超时任务时查询是否已经成功，成功了则不执行超时任务，防止超时任务取消失败的情况
            if (redisCatchStorage.queryPlayByDevice(platform.getServerGBId(), channelId) == null) {
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
        commanderForPlatform.broadcastInviteCmd(platform, channelId, mediaServerItem, ssrcInfo, (mediaServerItemForInvite, response)->{
            logger.info("[国标级联] 发起语音喊话 收到上级推流 deviceId: {}, channelId: {}", platform.getServerGBId(), channelId);
            dynamicTask.stop(timeOutTaskKey);
            // hook响应
            playService.onPublishHandlerForPlay(mediaServerItemForInvite, response, platform.getServerGBId(), channelId);
            // 收到流
            if (hookEvent != null) {
                hookEvent.response(mediaServerItem, response);
            }
        }, event -> {
            // 收到200OK 检测ssrc是否有变化，防止上级自定义了ssrc
            ResponseEvent responseEvent = (ResponseEvent) event.event;
            String contentString = new String(responseEvent.getResponse().getRawContent());
            // 获取ssrc
            int ssrcIndex = contentString.indexOf("y=");
            // 检查是否有y字段
            if (ssrcIndex >= 0) {
                //ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段 TODO 后续对不规范的非10位ssrc兼容
                String ssrcInResponse = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
                // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
                if (ssrcInfo.getSsrc().equals(ssrcInResponse) || ssrcCheck) {
                    return;
                }
                logger.info("[点播消息] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
                if (!mediaServerItem.isRtpEnable()) {
                    logger.info("[点播消息] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);

                    if (!mediaServerItem.getSsrcConfig().checkSsrc(ssrcInResponse)) {
                        // ssrc 不可用
                        // 释放ssrc
                        mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                        streamSession.remove(platform.getServerGBId(), channelId, ssrcInfo.getStream());
                        event.msg = "下级自定义了ssrc,但是此ssrc不可用";
                        event.statusCode = 400;
                        errorEvent.response(event);
                        return;
                    }

                    // 单端口模式streamId也有变化，需要重新设置监听
                    if (!mediaServerItem.isRtpEnable()) {
                        // 添加订阅
                        HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
                        subscribe.removeSubscribe(hookSubscribe);
                        hookSubscribe.getContent().put("stream", String.format("%08x", Integer.parseInt(ssrcInResponse)).toUpperCase());
                        subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, JSONObject response) -> {
                            logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + response.toJSONString());
                            dynamicTask.stop(timeOutTaskKey);
                            // hook响应
                            playService.onPublishHandlerForPlay(mediaServerItemInUse, response, platform.getServerGBId(), channelId);
                            hookEvent.response(mediaServerItemInUse, response);
                        });
                    }
                    // 关闭rtp server
                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                    // 重新开启ssrc server
                    mediaServerService.openRTPServer(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse, false, false, ssrcInfo.getPort(), true);


                }
            }
        }, eventResult -> {
            // 收到错误回复
            if (errorEvent != null) {
                errorEvent.response(eventResult);
            }
        });
    }

    @Override
    public void stopBroadcast(ParentPlatform platform, String channelId, String stream) throws InvalidArgumentException, ParseException, SsrcTransactionNotFoundException, SipException {
        commanderForPlatform.streamByeCmd(platform, channelId, stream, null, null);
    }
}
