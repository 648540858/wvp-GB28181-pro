package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.RecordItem;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.record.RecordInfoEndEvent;
import com.genersoft.iot.vmp.gb28181.event.record.RecordInfoEvent;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.UJson;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * @author lin
 */
@Slf4j
@Component
public class RecordInfoResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "RecordInfo";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    private Long recordInfoTtl = 1800L;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {
        try {
            // 回复200 OK
             responseAck((SIPRequest) evt.getRequest(), Response.OK);
        }catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 国标录像: {}", e.getMessage());
        }
        try {
            String sn = getText(rootElement, "SN");
            String channelId = getText(rootElement, "DeviceID");
            RecordInfo recordInfo = new RecordInfo();
            recordInfo.setChannelId(channelId);
            recordInfo.setDeviceId(device.getDeviceId());
            recordInfo.setSn(sn);
            recordInfo.setName(getText(rootElement, "Name"));
            String sumNumStr = getText(rootElement, "SumNum");
            int sumNum = 0;
            if (!ObjectUtils.isEmpty(sumNumStr)) {
                sumNum = Integer.parseInt(sumNumStr);
            }
            recordInfo.setSumNum(sumNum);
            Element recordListElement = rootElement.element("RecordList");
            if (recordListElement == null || sumNum == 0) {
                log.info("无录像数据");
                recordInfo.setCount(sumNum);
                recordInfoEventPush(recordInfo);
                recordInfoEndEventPush(recordInfo);
            } else {
                Iterator<Element> recordListIterator = recordListElement.elementIterator();
                if (recordListIterator != null) {
                    List<RecordItem> recordList = new ArrayList<>();
                    // 遍历DeviceList
                    while (recordListIterator.hasNext()) {
                        Element itemRecord = recordListIterator.next();
                        Element recordElement = itemRecord.element("DeviceID");
                        if (recordElement == null) {
                            log.info("记录为空，下一个...");
                            continue;
                        }
                        RecordItem record = new RecordItem();
                        record.setDeviceId(getText(itemRecord, "DeviceID"));
                        record.setName(getText(itemRecord, "Name"));
                        record.setFilePath(getText(itemRecord, "FilePath"));
                        record.setFileSize(getText(itemRecord, "FileSize"));
                        record.setAddress(getText(itemRecord, "Address"));

                        String startTimeStr = getText(itemRecord, "StartTime");
                        record.setStartTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(startTimeStr));

                        String endTimeStr = getText(itemRecord, "EndTime");
                        record.setEndTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(endTimeStr));

                        record.setSecrecy(itemRecord.element("Secrecy") == null ? 0
                                : Integer.parseInt(getText(itemRecord, "Secrecy")));
                        record.setType(getText(itemRecord, "Type"));
                        record.setRecorderId(getText(itemRecord, "RecorderID"));
                        recordList.add(record);
                    }
                    Map<String, String> map = recordList.stream()
                            .filter(record -> record.getDeviceId() != null)
                            .collect(Collectors.toMap(record -> record.getStartTime()+ record.getEndTime(), UJson::writeJson));
                    // 获取任务结果数据
                    String resKey = VideoManagerConstants.REDIS_RECORD_INFO_RES_PRE + channelId + sn;
                    redisTemplate.opsForHash().putAll(resKey, map);
                    redisTemplate.expire(resKey, recordInfoTtl, TimeUnit.SECONDS);
                    String resCountKey = VideoManagerConstants.REDIS_RECORD_INFO_RES_COUNT_PRE + channelId + sn;
                    Long incr = redisTemplate.opsForValue().increment(resCountKey, map.size());
                    if (incr == null) {
                        incr = 0L;
                    }
                    redisTemplate.expire(resCountKey, recordInfoTtl, TimeUnit.SECONDS);
                    recordInfo.setRecordList(recordList);
                    recordInfo.setCount(Math.toIntExact(incr));
                    recordInfoEventPush(recordInfo);
                    if (incr < sumNum) {
                        return;
                    }
                    // 已接收完成
                    List<RecordItem> resList = redisTemplate.opsForHash().entries(resKey).values().stream().map(e -> UJson.readJson(e.toString(), RecordItem.class)).collect(Collectors.toList());
                    if (resList.size() < sumNum) {
                        return;
                    }
                    recordInfo.setRecordList(resList);
                    recordInfoEndEventPush(recordInfo);
                }
            }
        } catch (Exception e) {
            log.error("[国标录像] 发现未处理的异常, \r\n{}", evt.getRequest());
            log.error("[国标录像] 异常内容： ", e);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {

    }

    private void recordInfoEventPush(RecordInfo recordInfo) {
        if (recordInfo == null) {
            return;
        }
        if(recordInfo.getRecordList() != null) {
            Collections.sort(recordInfo.getRecordList());
        }else{
            recordInfo.setRecordList(new ArrayList<>());
        }
        RecordInfoEvent outEvent = new RecordInfoEvent(this);
        outEvent.setRecordInfo(recordInfo);
        applicationEventPublisher.publishEvent(outEvent);
    }

    private void recordInfoEndEventPush(RecordInfo recordInfo) {
        if (recordInfo == null) {
            return;
        }
        if(recordInfo.getRecordList() != null) {
            Collections.sort(recordInfo.getRecordList());
        }else{
            recordInfo.setRecordList(new ArrayList<>());
        }
        RecordInfoEndEvent outEvent = new RecordInfoEndEvent(this);
        outEvent.setRecordInfo(recordInfo);
        applicationEventPublisher.publishEvent(outEvent);
    }
}
