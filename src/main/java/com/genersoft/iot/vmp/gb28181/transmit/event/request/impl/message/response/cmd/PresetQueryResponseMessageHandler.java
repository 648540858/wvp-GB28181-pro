package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.gb28181.bean.PresetQuerySipReq;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.PresetDataCatch;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.DocumentException;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * 设备预置位查询应答
 */
@Component
public class PresetQueryResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(PresetQueryResponseMessageHandler.class);
    private final String cmdType = "PresetQuery";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private DeferredResultHolder deferredResultHolder;

    @Autowired
    private PresetDataCatch presetDataCatch;



    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {

        SIPRequest request = (SIPRequest) evt.getRequest();
        String deviceId = SipUtils.getUserIdFromFromHeader(request);
        try {
             Element rootElement = getRootElement(evt, device.getCharset());

            if (rootElement == null) {
                logger.warn("[ 设备预置位查询应答 ] content cannot be null, {}", evt.getRequest());
                try {
                    responseAck(request, Response.BAD_REQUEST);
                } catch (InvalidArgumentException | ParseException | SipException e) {
                    logger.error("[命令发送失败] 设备预置位查询应答处理: {}", e.getMessage());
                }
                return;
            }
            Element presetListNumElement = rootElement.element("PresetList");
            String snStr = getText(rootElement, "SN");
            //该字段可能为通道或则设备的id
            String channelId = getText(rootElement, "DeviceID");
            if (channelId == null) {

            }
            String key = DeferredResultHolder.CALLBACK_CMD_PRESETQUERY + deviceId + channelId;
            if (snStr == null || presetListNumElement == null) {
                try {
                    responseAck(request, Response.BAD_REQUEST, "xml error");
                } catch (InvalidArgumentException | ParseException | SipException e) {
                    logger.error("[命令发送失败] 设备预置位查询应答处理: {}", e.getMessage());
                }
                return;
            }
            int sumNum = Integer.parseInt(presetListNumElement.attributeValue("Num"));
            int sn = Integer.parseInt(snStr);
            List<PresetItem> presetItems = new ArrayList<>();
            if (sumNum == 0) {
                presetDataCatch.setChannelSyncEnd(sn, null );
            }else {
                for (Iterator<Element> presetIterator = presetListNumElement.elementIterator(); presetIterator.hasNext(); ) {
                    Element itemListElement = presetIterator.next();
                    PresetItem presetItem = new PresetItem();
                    for (Iterator<Element> itemListIterator = itemListElement.elementIterator(); itemListIterator.hasNext(); ) {
                        // 遍历item
                        Element itemOne = itemListIterator.next();
                        String name = itemOne.getName();
                        String textTrim = itemOne.getTextTrim();
                        if ("PresetID".equalsIgnoreCase(name)) {
                            presetItem.setPresetID(Integer.parseInt(textTrim));
                        } else {
                            presetItem.setPresetName(textTrim);
                        }
                    }
                }
                presetDataCatch.put(sn, sumNum, presetItems);
                if (sumNum == presetDataCatch.get(sn).size()) {
                    RequestMessage requestMessage = new RequestMessage();
                    requestMessage.setKey(key);
                    requestMessage.setData(presetDataCatch.get(sn));
                    deferredResultHolder.invokeAllResult(requestMessage);
                }
            }
            try {
                responseAck(request, Response.OK);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                logger.error("[命令发送失败] 设备预置位查询应答处理: {}", e.getMessage());
            }
        } catch (DocumentException e) {
            logger.error("[解析xml]失败: ", e);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

    }

}
