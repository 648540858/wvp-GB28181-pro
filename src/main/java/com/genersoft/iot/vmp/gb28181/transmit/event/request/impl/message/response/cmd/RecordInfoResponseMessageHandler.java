package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.RecordItem;
import com.genersoft.iot.vmp.gb28181.transmit.callback.CheckForAllRecordsThread;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.DateUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
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
import java.util.UUID;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

@Component
public class RecordInfoResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(RecordInfoResponseMessageHandler.class);
    public static volatile List<String> threadNameList = new ArrayList();
    private final String cmdType = "RecordInfo";
    private final static String CACHE_RECORDINFO_KEY = "CACHE_RECORDINFO_";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private RedisUtil redis;

    @Autowired
    private DeferredResultHolder deferredResultHolder;

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
            String uuid = UUID.randomUUID().toString().replace("-", "");
            RecordInfo recordInfo = new RecordInfo();
            String sn = getText(rootElement, "SN");
            String key = DeferredResultHolder.CALLBACK_CMD_RECORDINFO + device.getDeviceId() + sn;
            recordInfo.setDeviceId(device.getDeviceId());
            recordInfo.setSn(sn);
            recordInfo.setName(getText(rootElement, "Name"));
            if (getText(rootElement, "SumNum") == null || getText(rootElement, "SumNum") == "") {
                recordInfo.setSumNum(0);
            } else {
                recordInfo.setSumNum(Integer.parseInt(getText(rootElement, "SumNum")));
            }
            Element recordListElement = rootElement.element("RecordList");
            if (recordListElement == null || recordInfo.getSumNum() == 0) {
                logger.info("无录像数据");
                RequestMessage msg = new RequestMessage();
                msg.setKey(key);
                msg.setData(recordInfo);
                deferredResultHolder.invokeAllResult(msg);
            } else {
                Iterator<Element> recordListIterator = recordListElement.elementIterator();
                List<RecordItem> recordList = new ArrayList<RecordItem>();
                if (recordListIterator != null) {
                    RecordItem record = new RecordItem();
                    logger.info("处理录像列表数据...");
                    // 遍历DeviceList
                    while (recordListIterator.hasNext()) {
                        Element itemRecord = recordListIterator.next();
                        Element recordElement = itemRecord.element("DeviceID");
                        if (recordElement == null) {
                            logger.info("记录为空，下一个...");
                            continue;
                        }
                        record = new RecordItem();
                        record.setDeviceId(getText(itemRecord, "DeviceID"));
                        record.setName(getText(itemRecord, "Name"));
                        record.setFilePath(getText(itemRecord, "FilePath"));
                        record.setAddress(getText(itemRecord, "Address"));
                        record.setStartTime(
                                DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(getText(itemRecord, "StartTime")));
                        record.setEndTime(
                                DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(getText(itemRecord, "EndTime")));
                        record.setSecrecy(itemRecord.element("Secrecy") == null ? 0
                                : Integer.parseInt(getText(itemRecord, "Secrecy")));
                        record.setType(getText(itemRecord, "Type"));
                        record.setRecorderId(getText(itemRecord, "RecorderID"));
                        recordList.add(record);
                    }
                    recordInfo.setRecordList(recordList);
                }

                // 改用单独线程统计已获取录像文件数量，避免多包并行分别统计不完整的问题
                String cacheKey = CACHE_RECORDINFO_KEY + device.getDeviceId() + sn;
                redis.set(cacheKey + "_" + uuid, recordList, 90);
                if (!threadNameList.contains(cacheKey)) {
                    threadNameList.add(cacheKey);
                    CheckForAllRecordsThread chk = new CheckForAllRecordsThread(cacheKey, recordInfo);
                    chk.setName(cacheKey);
                    chk.setDeferredResultHolder(deferredResultHolder);
                    chk.setRedis(redis);
                    chk.setLogger(logger);
                    chk.start();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Start Thread " + cacheKey + ".");
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Thread " + cacheKey + " already started.");
                    }
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
}
