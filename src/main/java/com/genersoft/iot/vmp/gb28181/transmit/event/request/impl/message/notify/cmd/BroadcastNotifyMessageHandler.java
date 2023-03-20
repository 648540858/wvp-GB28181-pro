package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlatformService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 状态信息(心跳)报送
 */
@Component
public class BroadcastNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(BroadcastNotifyMessageHandler.class);
    private final static String cmdType = "Broadcast";

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private IVideoManagerStorage storage;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IPlayService playService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private ZLMRTPServerFactory zlmrtpServerFactory;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform platform, Element rootElement) {
        // 来自上级平台的语音喊话请求
        SIPRequest request = (SIPRequest) evt.getRequest();
        try {
            Element snElement = rootElement.element("SN");
            if (snElement == null) {
                responseAck(request, Response.BAD_REQUEST, "sn must not null");
                return;
            }
            String sn = snElement.getText();
            Element targetIDElement = rootElement.element("TargetID");
            if (targetIDElement == null) {
                responseAck(request, Response.BAD_REQUEST, "TargetID must not null");
                return;
            }
            String targetId = targetIDElement.getText();


            logger.info("[国标级联 语音喊话] platform: {}, channel: {}", platform.getServerGBId(), targetId);

            DeviceChannel deviceChannel = storage.queryChannelInParentPlatform(platform.getServerGBId(), targetId);
            if (deviceChannel == null) {
                responseAck(request, Response.NOT_FOUND, "TargetID not found");
                return;
            }
            // 向下级发送语音的喊话请求
            Device device = deviceService.getDevice(deviceChannel.getDeviceId());
            if (device == null) {
                responseAck(request, Response.NOT_FOUND, "device not found");
                return;
            }
            responseAck(request, Response.OK);

            // 查看语音通道是否已经建立并且已经在使用
            if (playService.audioBroadcastInUse(device, targetId)) {
                commanderForPlatform.broadcastResultCmd(platform, deviceChannel, sn, false,null, null);
                return;
            }

            MediaServerItem mediaServerForMinimumLoad = mediaServerService.getMediaServerForMinimumLoad(null);
            commanderForPlatform.broadcastResultCmd(platform, deviceChannel, sn, true,  eventResult->{
                logger.info("[国标级联] 语音喊话 回复失败 platform： {}， 错误：{}/{}", platform.getServerGBId(), eventResult.statusCode, eventResult.msg);
            }, eventResult->{
                // 消息发送成功， 向上级发送invite，获取推流
                try {
                    platformService.broadcastInvite(platform, deviceChannel.getChannelId(), mediaServerForMinimumLoad,  (mediaServerItem, response)->{
                        // 上级平台推流成功
                        String app = response.getString("app");
                        String stream = response.getString("stream");
                        AudioBroadcastCatch broadcastCatch = audioBroadcastManager.get(device.getDeviceId(), targetId);
                        if (broadcastCatch != null ) {
                            if (playService.audioBroadcastInUse(device, targetId)) {
                                logger.info("[国标级联] 语音喊话 设备正正在使用中 platform： {}， channel: {}",
                                        platform.getServerGBId(), deviceChannel.getChannelId());
                                //  查看语音通道已经建立且已经占用 回复BYE
                                try {
                                    platformService.stopBroadcast(platform, deviceChannel.getChannelId(), stream);
                                } catch (InvalidArgumentException | ParseException | SsrcTransactionNotFoundException |
                                         SipException e) {
                                    logger.info("[消息发送失败] 国标级联 语音喊话 platform： {}， channel: {}", platform.getServerGBId(), deviceChannel.getChannelId());
                                }
                            }else {
                                // 查看语音通道已经建立但是未占用
                                broadcastCatch.setApp(app);
                                broadcastCatch.setStream(stream);
                                broadcastCatch.setMediaServerItem(mediaServerItem);
                                audioBroadcastManager.update(broadcastCatch);
                                // 推流到设备
                                SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(null, targetId, stream, null);
                                if (sendRtpItem == null) {
                                    logger.warn("[国标级联] 语音喊话 异常，未找到发流信息， channelId: {}, stream: {}", targetId, stream);
                                    logger.info("[国标级联] 语音喊话 重新开始，channelId: {}, stream: {}", targetId, stream);
                                    try {
                                        playService.audioBroadcastCmd(device, targetId, mediaServerItem, app, stream, 60, true, msg -> {
                                            logger.info("[语音喊话] 通道建立成功, device: {}, channel: {}", device.getDeviceId(), targetId);
                                        });
                                    } catch (SipException | InvalidArgumentException | ParseException e) {
                                        logger.info("[消息发送失败] 国标级联 语音喊话 platform： {}", platform.getServerGBId());
                                    }
                                }else {
                                    // 发流
                                    JSONObject jsonObject = zlmrtpServerFactory.startSendRtp(mediaServerItem, sendRtpItem);
                                    if (jsonObject != null && jsonObject.getInteger("code") == 0 ) {
                                        logger.info("[语音喊话] 自动推流成功, device: {}, channel: {}", device.getDeviceId(), targetId);
                                    }else {
                                        logger.info("[语音喊话] 推流失败, 结果： {}", jsonObject);
                                    }
                                }
                            }
                        }else {
                            try {
                                playService.audioBroadcastCmd(device, targetId, mediaServerItem, app, stream, 60, true, msg -> {
                                    logger.info("[语音喊话] 通道建立成功, device: {}, channel: {}", device.getDeviceId(), targetId);
                                });
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                logger.info("[消息发送失败] 国标级联 语音喊话 platform： {}", platform.getServerGBId());
                            }
                        }

                    }, eventResultForBroadcastInvite -> {
                        // 收到错误
                        logger.info("[国标级联-语音喊话] 与下级通道建立失败 device: {}, channel: {}， 错误：{}/{}", device.getDeviceId(),
                                targetId, eventResultForBroadcastInvite.statusCode, eventResultForBroadcastInvite.msg);
                    }, (code, msg)->{
                        // 超时
                        logger.info("[国标级联-语音喊话] 与下级通道建立超时 device: {}, channel: {}， 错误：{}/{}", device.getDeviceId(),
                                targetId, code, msg);
                    });
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.info("[消息发送失败] 国标级联 语音喊话 invite消息 platform： {}", platform.getServerGBId());
                }
            });
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.info("[消息发送失败] 国标级联 语音喊话 platform： {}", platform.getServerGBId());
        }

    }
}
