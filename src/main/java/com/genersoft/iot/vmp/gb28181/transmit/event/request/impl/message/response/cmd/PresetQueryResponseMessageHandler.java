package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.PresetDataCatch;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    private final ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();
    private AtomicBoolean processing = new AtomicBoolean(false);


    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        SIPRequest request = (SIPRequest) evt.getRequest();
        taskQueue.offer(new HandlerCatchData(evt, device, element));
        try {
            responseAck(request, Response.OK);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            logger.error("[命令发送失败] 设备预置位查询应答处理: {}", e.getMessage());
        }
        if (processing.compareAndSet(false, true)) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    HandlerCatchData take = taskQueue.poll();
                    Element rootElement = null;
                    try {
                        rootElement = getRootElement(take.getEvt(), take.getDevice().getCharset());
                    } catch (DocumentException e) {
                        logger.error("[设备预置位查询回复] xml解析 失败： ", e);
                        continue;
                    }
                    if (rootElement == null) {
                        logger.warn("[ 设备预置位查询回复 ] content cannot be null, {}", evt.getRequest());
                        continue;
                    }
                    Element presetListNumElement = rootElement.element("PresetList");
                    if (presetListNumElement == null) {
                        logger.warn("[ 设备预置位查询回复 ] PresetList cannot be null, {}", evt.getRequest());
                        return;
                    }
                    String snStr = getText(rootElement, "SN");

                    if (snStr == null ) {
                        logger.warn("[ 设备预置位查询回复 ] sn cannot be null, {}", evt.getRequest());
                        return;
                    }
                    String key = DeferredResultHolder.CALLBACK_CMD_PRESETQUERY + snStr;
                    String totalStr = getText(rootElement, "SumNum");
                    int sn = Integer.parseInt(snStr);
                    int sumNum = Integer.parseInt(totalStr);
                    List<PresetItem> presetItems = new ArrayList<>();
                    if (sumNum == 0) {
                        presetDataCatch.setChannelSyncEnd(sn, null );
                    }else {
                        int i = 0,j = 0;
                        for (Iterator<Element> presetIterator = presetListNumElement.elementIterator(); presetIterator.hasNext(); ) {
                            Element itemListElement = presetIterator.next();
                            PresetItem presetItem = new PresetItem();
                            i++;
                            for (Iterator<Element> itemListIterator = itemListElement.elementIterator(); itemListIterator.hasNext(); ) {
                                // 遍历item
                                Element itemOne = itemListIterator.next();
                                String name = itemOne.getName();
                                String textTrim = itemOne.getTextTrim();
                                if ("PresetID".equalsIgnoreCase(name)) {
                                    presetItem.setPresetId(Integer.parseInt(textTrim));
                                } else {
                                    presetItem.setPresetName(textTrim);
                                }
                                presetItems.add(presetItem);
                                j++;
                            }
                        }
                        presetDataCatch.put(sn, sumNum, presetItems);
                        if (presetDataCatch.get(sn).size() == sumNum) {
                            logger.warn("[ 设备预置位查询成功 ] 共{}条", presetDataCatch.get(sn).size());
                            RequestMessage requestMessage = new RequestMessage();
                            requestMessage.setKey(key);
                            requestMessage.setId(sn + "");
                            requestMessage.setData(presetDataCatch.get(sn));
                            deferredResultHolder.invokeResult(requestMessage);
                            presetDataCatch.setChannelSyncEnd(sn, null);
                        }
                    }
                }
                processing.set(false);
            });
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

    }

}
