package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.MessageResponseTask;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.Preset;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * 设备预置位查询应答
 */
@Slf4j
@Component
public class PresetQueryResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "PresetQuery";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    private final Map<String, MessageResponseTask<Preset>> mesageMap = new ConcurrentHashMap<>();

    private final DelayQueue<MessageResponseTask<Preset>> delayQueue = new DelayQueue<>();


    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {

        SIPRequest request = (SIPRequest) evt.getRequest();

        try {
             Element rootElement = getRootElement(evt, device.getCharset());

            if (rootElement == null) {
                log.warn("[ 设备预置位查询应答 ] content cannot be null, {}", evt.getRequest());
                try {
                    responseAck(request, Response.BAD_REQUEST);
                } catch (InvalidArgumentException | ParseException | SipException e) {
                    log.error("[命令发送失败] 设备预置位查询应答处理: {}", e.getMessage());
                }
                return;
            }
            Element presetListNumElement = rootElement.element("PresetList");
            Element snElement = rootElement.element("SN");
            //该字段可能为通道或则设备的id
            if (snElement == null || presetListNumElement == null) {
                try {
                    responseAck(request, Response.BAD_REQUEST, "xml error");
                } catch (InvalidArgumentException | ParseException | SipException e) {
                    log.error("[命令发送失败] 设备预置位查询应答处理: {}", e.getMessage());
                }
                return;
            }
            int sumNum = Integer.parseInt(presetListNumElement.attributeValue("Num"));
            List<Preset> presetQuerySipReqList = new ArrayList<>();
            if (sumNum > 0) {
                for (Iterator<Element> presetIterator = presetListNumElement.elementIterator(); presetIterator.hasNext(); ) {
                    Element itemListElement = presetIterator.next();
                    Preset presetQuerySipReq = new Preset();
                    for (Iterator<Element> itemListIterator = itemListElement.elementIterator(); itemListIterator.hasNext(); ) {
                        // 遍历item
                        Element itemOne = itemListIterator.next();
                        String name = itemOne.getName();
                        String textTrim = itemOne.getTextTrim();
                        if ("PresetID".equalsIgnoreCase(name)) {
                            presetQuerySipReq.setPresetId(textTrim);
                        } else {
                            presetQuerySipReq.setPresetName(textTrim);
                        }
                    }
                    presetQuerySipReqList.add(presetQuerySipReq);
                }
            }
//            if (presetQuerySipReqList.size() == sumNum) {
//                responseMessageHandler.handMessageEvent(rootElement, presetQuerySipReqList);
//            }else {
//                String sn = getText(element, "SN");
//                addCatch(cmdType + "_" + sn, rootElement, presetQuerySipReqList);
//            }
            String sn = getText(element, "SN");
            addCatch(cmdType + "_" + sn, sumNum,  rootElement, presetQuerySipReqList);
            try {
                responseAck(request, Response.OK);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                log.error("[命令发送失败] 设备预置位查询应答处理: {}", e.getMessage());
            }
        } catch (DocumentException e) {
            log.error("[解析xml]失败: ", e);
        }
    }

    private void addCatch(String key, int sumNum, Element rootElement, List<Preset> presetQuerySipReqList) {
        if (presetQuerySipReqList.size() == sumNum) {
            responseMessageHandler.handMessageEvent(rootElement, presetQuerySipReqList);
            if (mesageMap.containsKey(key)) {
                MessageResponseTask<Preset> messageResponseTask = mesageMap.get(key);
                mesageMap.remove(key);
                boolean remove = delayQueue.remove(messageResponseTask);
                if (!remove) {
                    log.info("[移除预置位查询任务] 从延时队列内移除失败： {}", key);
                }
            }
        }else {
            if (mesageMap.containsKey(key)) {
                MessageResponseTask<Preset> messageResponseTask = mesageMap.get(key);
                List<Preset> data = messageResponseTask.getData();
                data.addAll(presetQuerySipReqList);
                if (data.size() == sumNum) {
                    responseMessageHandler.handMessageEvent(rootElement, data);
                    mesageMap.remove(key);
                    boolean remove = delayQueue.remove(messageResponseTask);
                    if (!remove) {
                        log.info("[移除预置位查询任务] 从延时队列内移除失败： {}", key);
                    }
                    return;
                }
                messageResponseTask.setDelayTime(System.currentTimeMillis() + 1000);
            }else {
                MessageResponseTask<Preset> messageResponseTask = new MessageResponseTask<>();
                messageResponseTask.setElement(rootElement);
                messageResponseTask.setData(presetQuerySipReqList);
                messageResponseTask.setDelayTime(System.currentTimeMillis() + 1000);
                messageResponseTask.setKey(key);
                mesageMap.put(key, messageResponseTask);
                delayQueue.offer(messageResponseTask);
            }
        }
    }

    // 处理过期的缓存
    @Scheduled(fixedDelay = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void expirationCheck(){
        while (!delayQueue.isEmpty()) {
            MessageResponseTask<Preset> take = null;
            try {
                take = delayQueue.take();
                try {
                    responseMessageHandler.handMessageEvent(take.getElement(), take.getData());
                    mesageMap.remove(take.getKey());
                }catch (Exception e) {
                    log.error("[预置位查询到期] {} 到期处理时出现异常", take.getKey());
                }
            } catch (InterruptedException e) {
                log.error("[设备订阅任务] ", e);
            }
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element rootElement) {

    }

}
