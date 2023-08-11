package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.service.IInviteStreamService;
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
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.text.ParseException;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * 媒体通知
 */
@Component
public class MediaStatusNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(MediaStatusNotifyMessageHandler.class);
    private final String cmdType = "MediaStatus";

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private SIPCommanderFroPlatform sipCommanderFroPlatform;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorage storage;

    @Autowired
    private VideoStreamSessionManager sessionManager;

    @Autowired
    private ZlmHttpHookSubscribe subscribe;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {

        // 回复200 OK
        try {
             responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 国标级联 录像流推送完毕，回复200OK: {}", e.getMessage());
        }
        CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
        String NotifyType =getText(rootElement, "NotifyType");
        if ("121".equals(NotifyType)){
            logger.info("[录像流]推送完毕，收到关流通知");

            SsrcTransaction ssrcTransaction = streamSession.getSsrcTransaction(null, null, callIdHeader.getCallId(), null);
            if (ssrcTransaction != null) {
                logger.info("[录像流]推送完毕，关流通知， device: {}, channelId: {}", ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());
                InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId(), ssrcTransaction.getStream());
                if (inviteInfo.getStreamInfo() != null) {
                    inviteInfo.getStreamInfo().setProgress(1);
                    inviteStreamService.updateInviteInfo(inviteInfo);
                }

                try {
                    cmder.streamByeCmd(device, ssrcTransaction.getChannelId(), null, callIdHeader.getCallId());
                } catch (InvalidArgumentException | ParseException | SsrcTransactionNotFoundException | SipException e) {
                    logger.error("[录像流]推送完毕，收到关流通知， 发送BYE失败 {}", e.getMessage());
                }
                // 去除监听流注销自动停止下载的监听
                HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcTransaction.getStream(), false, "rtsp", ssrcTransaction.getMediaServerId());
                subscribe.removeSubscribe(hookSubscribe);

                // 如果级联播放，需要给上级发送此通知 TODO 多个上级同时观看一个下级 可能存在停错的问题，需要将点播CallId进行上下级绑定
                SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(null, ssrcTransaction.getChannelId(), null, null);
                if (sendRtpItem != null) {
                    ParentPlatform parentPlatform = storage.queryParentPlatByServerGBId(sendRtpItem.getPlatformId());
                    if (parentPlatform == null) {
                        logger.warn("[级联消息发送]：发送MediaStatus发现上级平台{}不存在", sendRtpItem.getPlatformId());
                        return;
                    }
                    try {
                        sipCommanderFroPlatform.sendMediaStatusNotify(parentPlatform, sendRtpItem);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] 国标级联 录像播放完毕: {}", e.getMessage());
                    }
                }
            }else {
                logger.info("[录像流]推送完毕，关流通知， 但是未找到对应的下载信息");
            }
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element element) {

    }
}
