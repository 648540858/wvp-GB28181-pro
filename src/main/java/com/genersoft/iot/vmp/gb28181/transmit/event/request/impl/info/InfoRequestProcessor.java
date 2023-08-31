package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.info;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import gov.nist.javax.sip.message.SIPRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Component
public class InfoRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    private final static Logger logger = LoggerFactory.getLogger(InfoRequestProcessor.class);

    private final String method = "INFO";

    @Autowired
    private SIPProcessorObserver sipProcessorObserver;

    @Autowired
    private IVideoManagerStorage storage;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private VideoStreamSessionManager sessionManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 添加消息处理的订阅
        sipProcessorObserver.addRequestProcessor(method, this);
    }

    @Override
    public void process(RequestEvent evt) {
        logger.debug("接收到消息：" + evt.getRequest());
        SIPRequest request = (SIPRequest) evt.getRequest();
        String deviceId = SipUtils.getUserIdFromFromHeader(request);
        CallIdHeader callIdHeader = request.getCallIdHeader();
        // 先从会话内查找
        SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransaction(null, null, callIdHeader.getCallId(), null);

        // 兼容海康 媒体通知 消息from字段不是设备ID的问题
        if (ssrcTransaction != null) {
            deviceId = ssrcTransaction.getDeviceId();
        }
        // 查询设备是否存在
        Device device = redisCatchStorage.getDevice(deviceId);
        // 查询上级平台是否存在
        ParentPlatform parentPlatform = storage.queryParentPlatByServerGBId(deviceId);
        try {
            if (device != null && parentPlatform != null) {
                logger.warn("[重复]平台与设备编号重复：{}", deviceId);
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
                responseAck(request, Response.NOT_FOUND, "device "+ deviceId +" not found");
                logger.warn("[设备未找到 ]： {}", deviceId);
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
                    SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(null, null, null, callIdHeader.getCallId());
                    String streamId = sendRtpItem.getStreamId();
                    InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
                    if (null == inviteInfo) {
                        responseAck(request, Response.NOT_FOUND, "stream " + streamId + " not found");
                        return;
                    }
                    Device device1 = storager.queryVideoDevice(inviteInfo.getDeviceId());
                    if (inviteInfo.getStreamInfo() != null) {
                        cmder.playbackControlCmd(device1,inviteInfo.getStreamInfo(),new String(evt.getRequest().getRawContent()),eventResult -> {
                            // 失败的回复
                            try {
                                responseAck(request, eventResult.statusCode, eventResult.msg);
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                logger.error("[命令发送失败] 国标级联 录像控制: {}", e.getMessage());
                            }
                        }, eventResult -> {
                            // 成功的回复
                            try {
                                responseAck(request, eventResult.statusCode);
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                logger.error("[命令发送失败] 国标级联 录像控制: {}", e.getMessage());
                            }
                        });
                    }

                }
            }
        } catch (SipException e) {
            logger.warn("SIP 回复错误", e);
        } catch (InvalidArgumentException e) {
            logger.warn("参数无效", e);
        } catch (ParseException e) {
            logger.warn("SIP回复时解析异常", e);
        }
    }


}
