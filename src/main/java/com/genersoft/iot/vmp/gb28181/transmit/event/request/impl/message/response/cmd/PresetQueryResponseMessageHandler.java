package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.gb28181.bean.PresetQuerySipReq;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
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

@Component
public class PresetQueryResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(PresetQueryResponseMessageHandler.class);
    private final String cmdType = "PresetQuery";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private DeferredResultHolder deferredResultHolder;


    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        Element rootElement = null;
        try {
            rootElement = getRootElement(evt, device.getCharset());

            Element presetListNumElement = rootElement.element("PresetList");
            Element snElement = rootElement.element("SN");
            //该字段可能为通道或则设备的id
            String deviceId = getText(rootElement, "DeviceID");
            String key = DeferredResultHolder.CALLBACK_CMD_PRESETQUERY + deviceId;
            if (snElement == null ||  presetListNumElement == null) {
                responseAck(evt, Response.BAD_REQUEST, "xml error");
                return;
            }
            int sumNum = Integer.parseInt(presetListNumElement.attributeValue("Num"));
            List<PresetQuerySipReq> presetQuerySipReqList = new ArrayList<>();
            if (sumNum == 0) {
                // 数据无预置位信息


            }else {
                for (Iterator<Element> presetIterator =  presetListNumElement.elementIterator();presetIterator.hasNext();){
                    Element itemListElement = presetIterator.next();
                    PresetQuerySipReq presetQuerySipReq = new PresetQuerySipReq();
                    for (Iterator<Element> itemListIterator =  itemListElement.elementIterator();itemListIterator.hasNext();){
                                // 遍历item
                                Element itemOne = itemListIterator.next();
                                String name = itemOne.getName();
                                String textTrim = itemOne.getTextTrim();
                                if("PresetID".equals(name)){
                                    presetQuerySipReq.setPresetId(textTrim);
                                }else {
                                    presetQuerySipReq.setPresetName(textTrim);
                                }
                    }
                    presetQuerySipReqList.add(presetQuerySipReq);


                }

            }
            RequestMessage requestMessage = new RequestMessage();
            requestMessage.setKey(key);
            requestMessage.setData(presetQuerySipReqList);
            deferredResultHolder.invokeAllResult(requestMessage);
            responseAck(evt, Response.OK);
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

    }

}
