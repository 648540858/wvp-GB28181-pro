package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.conf.CivilCodeFileConf;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.CatalogDataCatch;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
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

/**
 * 目录查询的回复
 */
@Component
public class CatalogResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(CatalogResponseMessageHandler.class);
    private final String cmdType = "Catalog";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    private final ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private CatalogDataCatch catalogDataCatch;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private CivilCodeFileConf civilCodeFileConf;

    @Autowired
    private SipConfig sipConfig;
    private AtomicBoolean processing = new AtomicBoolean(false);

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        taskQueue.offer(new HandlerCatchData(evt, device, element));
        // 回复200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 目录查询回复: {}", e.getMessage());
        }
        // 已经开启消息处理则跳过
        if (processing.compareAndSet(false, true)) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    // 全局异常捕获，保证下一条可以得到处理
                    try {
                        HandlerCatchData take = taskQueue.poll();
                        Element rootElement = null;
                        try {
                            rootElement = getRootElement(take.getEvt(), take.getDevice().getCharset());
                        } catch (DocumentException e) {
                            logger.error("[xml解析] 失败： ", e);
                            continue;
                        }
                        if (rootElement == null) {
                            logger.warn("[ 收到通道 ] content cannot be null, {}", evt.getRequest());
                            continue;
                        }
                        Element deviceListElement = rootElement.element("DeviceList");
                        Element sumNumElement = rootElement.element("SumNum");
                        Element snElement = rootElement.element("SN");
                        int sumNum = Integer.parseInt(sumNumElement.getText());

                        if (sumNum == 0) {
                            logger.info("[收到通道]设备:{}的: 0个", take.getDevice().getDeviceId());
                            // 数据已经完整接收
                            storager.cleanChannelsForDevice(take.getDevice().getDeviceId());
                            catalogDataCatch.setChannelSyncEnd(take.getDevice().getDeviceId(), null);
                        } else {
                            Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
                            if (deviceListIterator != null) {
                                List<DeviceChannel> channelList = new ArrayList<>();
                                List<String> parentChannelIds = new ArrayList<>();
                                // 遍历DeviceList
                                while (deviceListIterator.hasNext()) {
                                    Element itemDevice = deviceListIterator.next();
                                    Element channelDeviceElement = itemDevice.element("DeviceID");
                                    if (channelDeviceElement == null) {
                                        continue;
                                    }
                                    DeviceChannel channel = XmlUtil.channelContentHandler(itemDevice, device, null, civilCodeFileConf);
                                    if (channel == null) {
                                        logger.info("[收到目录订阅]：但是解析失败 {}", new String(evt.getRequest().getRawContent()));
                                        continue;
                                    }
                                    if (channel.getParentId() != null && channel.getParentId().equals(sipConfig.getId())) {
                                        channel.setParentId(null);
                                    }
                                    SipUtils.updateGps(channel, device.getGeoCoordSys());
                                    channel.setDeviceId(take.getDevice().getDeviceId());

                                    channelList.add(channel);
                                }
                                int sn = Integer.parseInt(snElement.getText());
                                catalogDataCatch.put(take.getDevice().getDeviceId(), sn, sumNum, take.getDevice(), channelList);
                                logger.info("[收到通道]设备: {} -> {}个，{}/{}", take.getDevice().getDeviceId(), channelList.size(), catalogDataCatch.get(take.getDevice().getDeviceId()) == null ? 0 : catalogDataCatch.get(take.getDevice().getDeviceId()).size(), sumNum);
                                if (catalogDataCatch.get(take.getDevice().getDeviceId()).size() == sumNum) {
                                    // 数据已经完整接收， 此时可能存在某个设备离线变上线的情况，但是考虑到性能，此处不做处理，
                                    // 目前支持设备通道上线通知时和设备上线时向上级通知
                                    boolean resetChannelsResult = storager.resetChannels(take.getDevice().getDeviceId(), catalogDataCatch.get(take.getDevice().getDeviceId()));
                                    if (!resetChannelsResult) {
                                        String errorMsg = "接收成功，写入失败，共" + sumNum + "条，已接收" + catalogDataCatch.get(take.getDevice().getDeviceId()).size() + "条";
                                        catalogDataCatch.setChannelSyncEnd(take.getDevice().getDeviceId(), errorMsg);
                                    } else {
                                        catalogDataCatch.setChannelSyncEnd(take.getDevice().getDeviceId(), null);
                                    }
                                }
                            }

                        }
                    } catch (Exception e) {
                        logger.warn("[收到通道] 发现未处理的异常, \r\n{}", evt.getRequest());
                        logger.error("[收到通道] 异常内容： ", e);
                    }
                }
                processing.set(false);
            });
        }

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

    }

    public SyncStatus getChannelSyncProgress(String deviceId) {
        if (catalogDataCatch.get(deviceId) == null) {
            return null;
        } else {
            return catalogDataCatch.getSyncStatus(deviceId);
        }
    }

    public boolean isSyncRunning(String deviceId) {
        if (catalogDataCatch.get(deviceId) == null) {
            return false;
        } else {
            return catalogDataCatch.isSyncRunning(deviceId);
        }
    }

    public void setChannelSyncReady(Device device, int sn) {
        catalogDataCatch.addReady(device, sn);
    }

    public void setChannelSyncEnd(String deviceId, String errorMsg) {
        catalogDataCatch.setChannelSyncEnd(deviceId, errorMsg);
    }
}
