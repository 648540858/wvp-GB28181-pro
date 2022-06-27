package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.common.StreamInfo;
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
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
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

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {

        // 回复200 OK
        try {
            responseAck(evt, Response.OK);
        } catch (SipException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
        String NotifyType =getText(rootElement, "NotifyType");
        if ("121".equals(NotifyType)){
            logger.info("[录像流]推送完毕，收到关流通知");
            // 查询是设备
            StreamInfo streamInfo = redisCatchStorage.queryDownload(null, null, null, callIdHeader.getCallId());
            if (streamInfo != null) {
                // 设置进度100%
                streamInfo.setProgress(1);
                redisCatchStorage.startDownload(streamInfo, callIdHeader.getCallId());
            }

            // 先从会话内查找
            SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransaction(null, null, callIdHeader.getCallId(), null);
            if (ssrcTransaction != null) { // 兼容海康 媒体通知 消息from字段不是设备ID的问题
                cmder.streamByeCmd(device.getDeviceId(), ssrcTransaction.getChannelId(), null, callIdHeader.getCallId());

                // 如果级联播放，需要给上级发送此通知 TODO 多个上级同时观看一个下级 可能存在停错的问题，需要将点播CallId进行上下级绑定
                SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(null, ssrcTransaction.getChannelId(), null, null);
                if (sendRtpItem != null) {
                    ParentPlatform parentPlatform = storage.queryParentPlatByServerGBId(sendRtpItem.getPlatformId());
                    if (parentPlatform == null) {
                        logger.warn("[级联消息发送]：发送MediaStatus发现上级平台{}不存在", sendRtpItem.getPlatformId());
                        return;
                    }
                    sipCommanderFroPlatform.sendMediaStatusNotify(parentPlatform, sendRtpItem);
                }
            }
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element element) {

    }
}
