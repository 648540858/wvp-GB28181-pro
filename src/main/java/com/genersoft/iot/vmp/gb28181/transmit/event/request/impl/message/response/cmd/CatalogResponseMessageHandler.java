package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.session.CatalogDataCatch;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.Coordtransform;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class CatalogResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(CatalogResponseMessageHandler.class);
    private final String cmdType = "Catalog";

    private boolean taskQueueHandlerRun = false;

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    private ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private DeferredResultHolder deferredResultHolder;

    @Autowired
    private CatalogDataCatch catalogDataCatch;

    @Autowired
    private SipConfig config;

    @Autowired
    private EventPublisher publisher;

    //by brewswang
    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        taskQueue.offer(new HandlerCatchData(evt, device, element));
        // 回复200 OK
        try {
            responseAck(evt, Response.OK);
        } catch (SipException e) {
            throw new RuntimeException(e);
        } catch (InvalidArgumentException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if (!taskQueueHandlerRun) {
            taskQueueHandlerRun = true;
            taskExecutor.execute(()-> {
                while (!taskQueue.isEmpty()) {
                    HandlerCatchData take = taskQueue.poll();
                    String key = DeferredResultHolder.CALLBACK_CMD_CATALOG + take.getDevice().getDeviceId();
                    Element rootElement = null;
                    try {
                        rootElement = getRootElement(take.getEvt(), take.getDevice().getCharset());
                        Element deviceListElement = rootElement.element("DeviceList");
                        Element sumNumElement = rootElement.element("SumNum");
                        Element snElement = rootElement.element("SN");
                        if (snElement == null || sumNumElement == null || deviceListElement == null) {
                            responseAck(take.getEvt(), Response.BAD_REQUEST, "xml error");
                            return;
                        }
                        int sumNum = Integer.parseInt(sumNumElement.getText());

                        if (sumNum == 0) {
                            // 数据已经完整接收
                            storager.cleanChannelsForDevice(take.getDevice().getDeviceId());
                            catalogDataCatch.setChannelSyncEnd(take.getDevice().getDeviceId(), null);
                        }else {
                            Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
                            if (deviceListIterator != null) {
                                List<DeviceChannel> channelList = new ArrayList<>();
                                // 遍历DeviceList
                                while (deviceListIterator.hasNext()) {
                                    Element itemDevice = deviceListIterator.next();
                                    Element channelDeviceElement = itemDevice.element("DeviceID");
                                    if (channelDeviceElement == null) {
                                        continue;
                                    }
                                    //by brewswang
    //                        if (NumericUtil.isDouble(XmlUtil.getText(itemDevice, "Longitude"))) {//如果包含位置信息，就更新一下位置
    //                            processNotifyMobilePosition(evt, itemDevice);
    //                        }
                                    DeviceChannel deviceChannel = XmlUtil.channelContentHander(itemDevice, device);
                                    deviceChannel.setDeviceId(take.getDevice().getDeviceId());

                                    channelList.add(deviceChannel);
                                }
                                int sn = Integer.parseInt(snElement.getText());
                                catalogDataCatch.put(take.getDevice().getDeviceId(), sn, sumNum, take.getDevice(), channelList);
                                logger.info("收到来自设备【{}】的通道: {}个，{}/{}", take.getDevice().getDeviceId(), channelList.size(), catalogDataCatch.get(take.getDevice().getDeviceId()) == null ? 0 :catalogDataCatch.get(take.getDevice().getDeviceId()).size(), sumNum);
                                if (catalogDataCatch.get(take.getDevice().getDeviceId()).size() == sumNum) {
                                    // 数据已经完整接收
                                    boolean resetChannelsResult = storager.resetChannels(take.getDevice().getDeviceId(), catalogDataCatch.get(take.getDevice().getDeviceId()));
                                    if (!resetChannelsResult) {
                                        String errorMsg = "接收成功，写入失败，共" + sumNum + "条，已接收" + catalogDataCatch.get(take.getDevice().getDeviceId()).size() + "条";
                                        catalogDataCatch.setChannelSyncEnd(take.getDevice().getDeviceId(), errorMsg);
                                    }else {
                                        catalogDataCatch.setChannelSyncEnd(take.getDevice().getDeviceId(), null);
                                    }
                                }
                            }

                        }
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
                taskQueueHandlerRun = false;
            });

        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

    }

    /**
     * 处理设备位置的更新
     *
     * @param evt, itemDevice
     */
    private void processNotifyMobilePosition(RequestEvent evt, Element itemDevice) {
        try {
            // 回复 200 OK
            Element rootElement = getRootElement(evt);
            MobilePosition mobilePosition = new MobilePosition();
            Element deviceIdElement = rootElement.element("DeviceID");
            String deviceId = deviceIdElement.getTextTrim().toString();
            Device device = redisCatchStorage.getDevice(deviceId);
            if (device != null) {
                if (!StringUtils.isEmpty(device.getName())) {
                    mobilePosition.setDeviceName(device.getName());
                }
            }
            mobilePosition.setDeviceId(XmlUtil.getText(rootElement, "DeviceID"));

            String time = XmlUtil.getText(itemDevice, "Time");
            if(time==null){
                time =  XmlUtil.getText(itemDevice, "EndTime");
            }
            mobilePosition.setTime(time);
            String longitude = XmlUtil.getText(itemDevice, "Longitude");
            if(longitude!=null) {
                mobilePosition.setLongitude(Double.parseDouble(longitude));
            }
            String latitude = XmlUtil.getText(itemDevice, "Latitude");
            if(latitude!=null) {
                mobilePosition.setLatitude(Double.parseDouble(latitude));
            }
            if (NumericUtil.isDouble(XmlUtil.getText(itemDevice, "Speed"))) {
                mobilePosition.setSpeed(Double.parseDouble(XmlUtil.getText(itemDevice, "Speed")));
            } else {
                mobilePosition.setSpeed(0.0);
            }
            if (NumericUtil.isDouble(XmlUtil.getText(itemDevice, "Direction"))) {
                mobilePosition.setDirection(Double.parseDouble(XmlUtil.getText(itemDevice, "Direction")));
            } else {
                mobilePosition.setDirection(0.0);
            }
            if (NumericUtil.isDouble(XmlUtil.getText(itemDevice, "Altitude"))) {
                mobilePosition.setAltitude(Double.parseDouble(XmlUtil.getText(itemDevice, "Altitude")));
            } else {
                mobilePosition.setAltitude(0.0);
            }
            mobilePosition.setReportSource("Mobile Position");
            // 默认来源坐标系为WGS-84处理
            Double[] gcj02Point = Coordtransform.WGS84ToGCJ02(mobilePosition.getLongitude(), mobilePosition.getLatitude());
            logger.info("GCJ02坐标：" + gcj02Point[0] + ", " + gcj02Point[1]);
            mobilePosition.setGeodeticSystem("GCJ-02");
            mobilePosition.setCnLng(gcj02Point[0] + "");
            mobilePosition.setCnLat(gcj02Point[1] + "");
            if (!userSetting.getSavePositionHistory()) {
                storager.clearMobilePositionsByDeviceId(deviceId);
            }
            storager.insertMobilePosition(mobilePosition);
            responseAck(evt, Response.OK);
        } catch (DocumentException | SipException | InvalidArgumentException | ParseException e) {
            e.printStackTrace();
        }
    }

    public SyncStatus getChannelSyncProgress(String deviceId) {
        if (catalogDataCatch.get(deviceId) == null) {
            return null;
        }else {
            return catalogDataCatch.getSyncStatus(deviceId);
        }
    }

    public boolean isSyncRunning(String deviceId) {
        if (catalogDataCatch.get(deviceId) == null) {
            return false;
        }else {
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
