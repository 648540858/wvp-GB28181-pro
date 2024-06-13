package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IDeviceService;
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
                logger.warn("[国标级联 语音喊话] 未找到通道 platform: {}, channel: {}", platform.getServerGBId(), targetId);
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

            MediaServer mediaServerForMinimumLoad = mediaServerService.getMediaServerForMinimumLoad(null);
            commanderForPlatform.broadcastResultCmd(platform, deviceChannel, sn, true,  eventResult->{
                logger.info("[国标级联] 语音喊话 回复失败 platform： {}， 错误：{}/{}", platform.getServerGBId(), eventResult.statusCode, eventResult.msg);
            }, eventResult->{

                // 消息发送成功， 向上级发送invite，获取推流
                try {
                    platformService.broadcastInvite(platform, deviceChannel.getChannelId(), mediaServerForMinimumLoad,  (hookData)->{
                        // 上级平台推流成功
                        AudioBroadcastCatch broadcastCatch = audioBroadcastManager.get(device.getDeviceId(), targetId);
                        if (broadcastCatch != null ) {
                            if (playService.audioBroadcastInUse(device, targetId)) {
                                logger.info("[国标级联] 语音喊话 设备正在使用中 platform： {}， channel: {}",
                                        platform.getServerGBId(), deviceChannel.getChannelId());
                                //  查看语音通道已经建立且已经占用 回复BYE
                                platformService.stopBroadcast(platform, deviceChannel, hookData.getStream(),  true, hookData.getMediaServer());
                            }else {
                                // 查看语音通道已经建立但是未占用
                                broadcastCatch.setApp(hookData.getApp());
                                broadcastCatch.setStream(hookData.getStream());
                                broadcastCatch.setMediaServerItem(hookData.getMediaServer());
                                audioBroadcastManager.update(broadcastCatch);
                                // 推流到设备
                                SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(null, targetId, hookData.getStream(), null);
                                if (sendRtpItem == null) {
                                    logger.warn("[国标级联] 语音喊话 异常，未找到发流信息， channelId: {}, stream: {}", targetId, hookData.getStream());
                                    logger.info("[国标级联] 语音喊话 重新开始，channelId: {}, stream: {}", targetId, hookData.getStream());
                                    try {
                                        playService.audioBroadcastCmd(device, targetId, hookData.getMediaServer(), hookData.getApp(), hookData.getStream(), 60, true, msg -> {
                                            logger.info("[语音喊话] 通道建立成功, device: {}, channel: {}", device.getDeviceId(), targetId);
                                        });
                                    } catch (SipException | InvalidArgumentException | ParseException e) {
                                        logger.info("[消息发送失败] 国标级联 语音喊话 platform： {}", platform.getServerGBId());
                                    }
                                }else {
                                    // 发流
                                    try {
                                        mediaServerService.startSendRtp(hookData.getMediaServer(), sendRtpItem);
                                    }catch (ControllerException e) {
                                        logger.info("[语音喊话] 推流失败, 结果： {}", e.getMessage());
                                        return;
                                    }
                                    logger.info("[语音喊话] 自动推流成功, device: {}, channel: {}", device.getDeviceId(), targetId);
                                }
                            }
                        }else {
                            try {
                                playService.audioBroadcastCmd(device, targetId, hookData.getMediaServer(), hookData.getApp(), hookData.getStream(), 60, true, msg -> {
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
