package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.info;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.service.*;
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
    private IGbChannelService channelService;

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
        SIPRequest request = (SIPRequest) evt.getRequest();
        CallIdHeader callIdHeader = request.getCallIdHeader();
        // 先从会话内查找
        try {
            SendRtpInfo sendRtpInfo = sendRtpServerService.queryByCallId(callIdHeader.getCallId());
            if (sendRtpInfo == null || !sendRtpInfo.isSendToPlatform()) {
                // 不存在则回复404
                log.warn("[INFO 消息] 事务未找到， callID： {}", callIdHeader.getCallId());
                responseAck(request, Response.NOT_FOUND, "transaction not found");
                return;
            }
            // 查询上级平台是否存在
            Platform platform = platformService.queryPlatformByServerGBId(sendRtpInfo.getTargetId());
            if (platform == null || !platform.isStatus()) {
                // 不存在则回复404
                log.warn("[INFO 消息] 平台未找到或者已离线： 平台： {}", sendRtpInfo.getTargetId());
                responseAck(request, Response.NOT_FOUND, "platform "+ sendRtpInfo.getTargetId() +" not found or offline");
                return;
            }
            CommonGBChannel channel = channelService.getOne(sendRtpInfo.getChannelId());
            if (channel == null) {
                // 不存在则回复404
                log.warn("[INFO 消息] 通道不存在： 通道ID： {}", sendRtpInfo.getChannelId());
                responseAck(request, Response.NOT_FOUND, "channel not found or offline");
                return;
            }
            // 判断通道类型
            if (channel.getDataType() != ChannelDataType.GB28181) {
                // 非国标通道不支持录像回放控制
                log.warn("[INFO 消息] 非国标通道不支持录像回放控制： 通道ID： {}", sendRtpInfo.getChannelId());
                responseAck(request, Response.FORBIDDEN, "");
                return;
            }

            // 根据通道ID，获取所属设备
            Device device = deviceService.getDevice(channel.getDataDeviceId());
            if (device == null) {
                // 不存在则回复404
                log.warn("[INFO 消息] 通道所属设备不存在， 通道ID： {}", sendRtpInfo.getChannelId());
                responseAck(request, Response.NOT_FOUND, "platform "+ sendRtpInfo.getChannelId() +" not found or offline");
                return;
            }
            // 获取通道的原始信息
            DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(sendRtpInfo.getChannelId());
            // 向原始通道转发控制消息
            ContentTypeHeader header = (ContentTypeHeader)evt.getRequest().getHeader(ContentTypeHeader.NAME);
            String contentType = header.getContentType();
            String contentSubType = header.getContentSubType();
            if ("Application".equalsIgnoreCase(contentType) && "MANSRTSP".equalsIgnoreCase(contentSubType)) {
                log.info("[INFO 消息] 平台： {}->{}({})/{}", platform.getServerGBId(), device.getName(),
                        device.getDeviceId(), deviceChannel.getId());
                // 不解析协议， 直接转发给对应的设备
                cmder.playbackControlCmd(device, deviceChannel, sendRtpInfo.getStream(), new String(evt.getRequest().getRawContent()), eventResult -> {
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
