package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.RecordItem;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.session.RecordDataCatch;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.*;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

/**
 * @author lin
 */
@Component
public class RecordInfoResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(RecordInfoResponseMessageHandler.class);
    public static volatile List<String> threadNameList = new ArrayList();
    private final String cmdType = "RecordInfo";
    private final static String CACHE_RECORDINFO_KEY = "CACHE_RECORDINFO_";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private RecordDataCatch recordDataCatch;

    @Autowired
    private DeferredResultHolder deferredResultHolder;



    @Autowired
    private EventPublisher eventPublisher;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element rootElement) {

        // 回复200 OK
        try {
            responseAck(evt, Response.OK);

            rootElement = getRootElement(evt, device.getCharset());
            String sn = getText(rootElement, "SN");
            RecordInfo recordInfo = new RecordInfo();
            recordInfo.setDeviceId(device.getDeviceId());
            recordInfo.setSn(sn);
            recordInfo.setName(getText(rootElement, "Name"));
            String sumNumStr = getText(rootElement, "SumNum");
            int sumNum = 0;
            if (!StringUtils.isEmpty(sumNumStr)) {
                sumNum = Integer.parseInt(sumNumStr);
            }
            recordInfo.setSumNum(sumNum);
            Element recordListElement = rootElement.element("RecordList");
            if (recordListElement == null || sumNum == 0) {
                logger.info("无录像数据");
                eventPublisher.recordEndEventPush(recordInfo);
                recordDataCatch.put(device.getDeviceId(), sn, sumNum, new ArrayList<>());
                releaseRequest(device.getDeviceId(), sn);
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
                    recordInfo.setRecordList(recordList);
                    // 发送消息，如果是上级查询此录像，则会通过这里通知给上级
                    eventPublisher.recordEndEventPush(recordInfo);
                    int count = recordDataCatch.put(device.getDeviceId(), sn, sumNum, recordList);
                    logger.info("[国标录像]， {}->{}: {}/{}", device.getDeviceId(), sn, count, sumNum);
                }

                if (recordDataCatch.isComplete(device.getDeviceId(), sn)){
                    releaseRequest(device.getDeviceId(), sn);
                }
            }
        } catch (SipException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element element) {

    }

    public void releaseRequest(String deviceId, String sn){
        String key = DeferredResultHolder.CALLBACK_CMD_RECORDINFO + deviceId + sn;
        WVPResult<RecordInfo> wvpResult = new WVPResult<>();
        wvpResult.setCode(0);
        wvpResult.setMsg("success");
        // 对数据进行排序
        Collections.sort(recordDataCatch.getRecordInfo(deviceId, sn).getRecordList());
        wvpResult.setData(recordDataCatch.getRecordInfo(deviceId, sn));

        RequestMessage msg = new RequestMessage();
        msg.setKey(key);
        msg.setData(wvpResult);
        deferredResultHolder.invokeAllResult(msg);
        recordDataCatch.remove(deviceId, sn);
    }
}
