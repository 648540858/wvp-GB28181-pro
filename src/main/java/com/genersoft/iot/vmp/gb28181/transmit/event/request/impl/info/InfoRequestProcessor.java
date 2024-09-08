package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.info;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * INFO 一般用于国标级联时的回放控制
 */
@Slf4j
@Component
public class InfoRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    private final String method = "INFO";

    @Autowired
    private SIPProcessorObserver sipProcessorObserver;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 添加消息处理的订阅
        sipProcessorObserver.addRequestProcessor(method, this);
    }

    @Override
    public void process(RequestEvent evt) {
        log.debug("接收到消息：" + evt.getRequest());
        SIPRequest request = (SIPRequest) evt.getRequest();
        CallIdHeader callIdHeader = request.getCallIdHeader();
        // 先从会话内查找
        SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByCallId(callIdHeader.getCallId());

        // 查询设备是否存在
        Device device = redisCatchStorage.getDevice(ssrcTransaction.getDeviceId());
        // 查询上级平台是否存在
        Platform parentPlatform = platformService.queryPlatformByServerGBId(ssrcTransaction.getDeviceId());
        try {
            if (device != null && parentPlatform != null) {
                log.warn("[重复]平台与设备编号重复：{}", ssrcTransaction.getDeviceId());
                String hostAddress = request.getRemoteAddress().getHostAddress();
                int remotePort = request.getRemotePort();
                if (device.getHostAddress().equals(hostAddress + ":" + remotePort)) {
                    parentPlatform = null;
                }else {
                    device = null;
                }
            }
            if (device == null && parentPlatform == null) {
                // 不存在则回复404
                responseAck(request, Response.NOT_FOUND, "device "+ ssrcTransaction.getDeviceId() +" not found");
                log.warn("[设备未找到 ]： {}", ssrcTransaction.getDeviceId());
                if (sipSubscribe.getErrorSubscribe(callIdHeader.getCallId()) != null){
                    DeviceNotFoundEvent deviceNotFoundEvent = new DeviceNotFoundEvent(evt.getDialog());
                    deviceNotFoundEvent.setCallId(callIdHeader.getCallId());
                    SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult(deviceNotFoundEvent);
                    sipSubscribe.getErrorSubscribe(callIdHeader.getCallId()).response(eventResult);
                };
            }else {
                ContentTypeHeader header = (ContentTypeHeader)evt.getRequest().getHeader(ContentTypeHeader.NAME);
                String contentType = header.getContentType();
                String contentSubType = header.getContentSubType();
                if ("Application".equalsIgnoreCase(contentType) && "MANSRTSP".equalsIgnoreCase(contentSubType)) {
                    SendRtpInfo sendRtpItem = sendRtpServerService.queryByCallId(callIdHeader.getCallId());
                    String streamId = sendRtpItem.getStream();
                    InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
                    if (null == inviteInfo) {
                        responseAck(request, Response.NOT_FOUND, "stream " + streamId + " not found");
                        return;
                    }
                    Device device1 = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
                    DeviceChannel deviceChannel = deviceChannelService.getOneById(inviteInfo.getChannelId());
                    if (device1 != null && deviceChannel != null && inviteInfo.getStreamInfo() != null) {
                        // 不解析协议， 直接转发给对应的设备
                        cmder.playbackControlCmd(device1, deviceChannel, inviteInfo.getStreamInfo(),new String(evt.getRequest().getRawContent()), eventResult -> {
                            // 失败的回复
                            try {
                                responseAck(request, eventResult.statusCode, eventResult.msg);
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                log.error("[命令发送失败] 国标级联 录像控制: {}", e.getMessage());
                            }
                        }, eventResult -> {
                            // 成功的回复
                            try {
                                responseAck(request, eventResult.statusCode);
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                log.error("[命令发送失败] 国标级联 录像控制: {}", e.getMessage());
                            }
                        });
                    }else {
                        responseAck(request, Response.NOT_FOUND, "not found");
                    }

                }
            }
        } catch (SipException e) {
            log.warn("SIP 回复错误", e);
        } catch (InvalidArgumentException e) {
            log.warn("参数无效", e);
        } catch (ParseException e) {
            log.warn("SIP回复时解析异常", e);
        }
    }
}
