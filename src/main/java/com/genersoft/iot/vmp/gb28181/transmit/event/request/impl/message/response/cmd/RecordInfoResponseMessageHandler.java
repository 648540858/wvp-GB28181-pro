package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.UJson;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * @author lin
 */
@Component
public class RecordInfoResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(RecordInfoResponseMessageHandler.class);
    private final String cmdType = "RecordInfo";

    private ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private DeferredResultHolder deferredResultHolder;

    @Autowired
    private EventPublisher eventPublisher;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

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
            logger.error("[命令发送失败] 国标级联 国标录像: {}", e.getMessage());
        }
        taskExecutor.execute(()->{
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
                    logger.info("无录像数据");
                    recordInfo.setCount(sumNum);
                    eventPublisher.recordEndEventPush(recordInfo);
                    releaseRequest(device.getDeviceId(), sn,recordInfo);
                } else {
                    Iterator<Element> recordListIterator = recordListElement.elementIterator();
                    if (recordListIterator != null) {
                        List<RecordItem> recordList = new ArrayList<>();
                        // 遍历DeviceList
                        while (recordListIterator.hasNext()) {
                            Element itemRecord = recordListIterator.next();
                            Element recordElement = itemRecord.element("DeviceID");
                            if (recordElement == null) {
                                logger.info("记录为空，下一个...");
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
                        long incr = redisTemplate.opsForValue().increment(resCountKey, map.size());
                        redisTemplate.expire(resCountKey, recordInfoTtl, TimeUnit.SECONDS);
                        recordInfo.setRecordList(recordList);
                        recordInfo.setCount(Math.toIntExact(incr));
                        eventPublisher.recordEndEventPush(recordInfo);
                        if (incr < sumNum) {
                            return;
                        }
                        // 已接收完成
                        List<RecordItem> resList = redisTemplate.opsForHash().entries(resKey).values().stream().map(e -> UJson.readJson(e.toString(), RecordItem.class)).collect(Collectors.toList());
                        if (resList.size() < sumNum) {
                            return;
                        }
                        recordInfo.setRecordList(resList);
                        releaseRequest(device.getDeviceId(), sn,recordInfo);
                    }
                }
            } catch (Exception e) {
                logger.error("[国标录像] 发现未处理的异常, \r\n{}", evt.getRequest());
                logger.error("[国标录像] 异常内容： ", e);
            }
        });
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element element) {

    }

    public void releaseRequest(String deviceId, String sn,RecordInfo recordInfo){
        String key = DeferredResultHolder.CALLBACK_CMD_RECORDINFO + deviceId + sn;
        // 对数据进行排序
        if(recordInfo!=null && recordInfo.getRecordList()!=null) {
            Collections.sort(recordInfo.getRecordList());
        }else{
            recordInfo.setRecordList(new ArrayList<>());
        }

        RequestMessage msg = new RequestMessage();
        msg.setKey(key);
        msg.setData(recordInfo);
        deferredResultHolder.invokeAllResult(msg);
    }
}
