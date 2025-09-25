package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.gb28181.service.IRegionService;
import com.genersoft.iot.vmp.gb28181.session.CatalogDataManager;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.genersoft.iot.vmp.utils.Coordtransform;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 目录查询的回复
 */
@Slf4j
@Component
public class CatalogResponseMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "Catalog";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    private final ConcurrentLinkedQueue<HandlerCatchData> taskQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private IRegionService regionService;

    @Autowired
    private IGroupService groupService;

    @Autowired
    private CatalogDataManager catalogDataCatch;

    @Autowired
    private SipConfig sipConfig;

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
            log.error("[命令发送失败] 目录查询回复: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 50)
    @Transactional
    public void executeTaskQueue(){
        if (taskQueue.isEmpty()) {
            return;
        }
        List<HandlerCatchData> handlerCatchDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            HandlerCatchData poll = taskQueue.poll();
            if (poll != null) {
                handlerCatchDataList.add(poll);
            }
        }
        if (handlerCatchDataList.isEmpty()) {
            return;
        }
        for (HandlerCatchData take : handlerCatchDataList) {
            if (take == null) {
                continue;
            }
            RequestEvent evt = take.getEvt();
            int sn = 0;
            // 全局异常捕获，保证下一条可以得到处理
            try {
                Element rootElement = null;
                try {
                    rootElement = getRootElement(take.getEvt(), take.getDevice().getCharset());
                } catch (DocumentException e) {
                    log.error("[xml解析] 失败： ", e);
                    continue;
                }
                if (rootElement == null) {
                    log.warn("[ 收到通道 ] content cannot be null, {}", evt.getRequest());
                    continue;
                }
                Element deviceListElement = rootElement.element("DeviceList");
                Element sumNumElement = rootElement.element("SumNum");
                Element snElement = rootElement.element("SN");
                int sumNum = Integer.parseInt(sumNumElement.getText());

                if (sumNum == 0) {
                    log.info("[收到通道]设备:{}的: 0个", take.getDevice().getDeviceId());
                    // 数据已经完整接收
                    deviceChannelService.cleanChannelsForDevice(take.getDevice().getId());
                    catalogDataCatch.setChannelSyncEnd(take.getDevice().getDeviceId(), sn, null);
                } else {
                    Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
                    if (deviceListIterator != null) {
                        List<DeviceChannel> channelList = new ArrayList<>();
                        List<Region> regionList = new ArrayList<>();
                        List<Group> groupList = new ArrayList<>();
                        // 遍历DeviceList
                        while (deviceListIterator.hasNext()) {
                            Element itemDevice = deviceListIterator.next();
                            Element channelDeviceElement = itemDevice.element("DeviceID");
                            if (channelDeviceElement == null) {
                                // 总数减一， 避免最后总数不对 无法确定问题
                                continue;
                            }
                            // 从xml解析内容到 DeviceChannel 对象
                            DeviceChannel channel = DeviceChannel.decode(itemDevice);
                            if (channel.getDeviceId() == null) {
                                log.info("[收到目录订阅]：但是解析失败 {}", new String(evt.getRequest().getRawContent()));
                                continue;
                            }
                            channel.setDataDeviceId(take.getDevice().getId());
                            if (channel.getParentId() != null && channel.getParentId().equals(sipConfig.getId())) {
                                channel.setParentId(null);
                            }
                            // 解析通道类型
                            if (channel.getDeviceId().length() <= 8) {
                                // 行政区划
                                Region region = Region.getInstance(channel);
                                regionList.add(region);
                                channel.setChannelType(1);
                            }else if (channel.getDeviceId().length() == 20){
                                // 业务分组/虚拟组织
                                Group group = Group.getInstance(channel);
                                if (group != null) {
                                    channel.setParental(1);
                                    channel.setChannelType(2);
                                    groupList.add(group);
                                }
                                if (channel.getLongitude() > 0 && channel.getLatitude() > 0) {
                                    Double[] wgs84Position = Coordtransform.GCJ02ToWGS84(channel.getLongitude(), channel.getLatitude());
                                    channel.setGbLongitude(wgs84Position[0]);
                                    channel.setGbLatitude(wgs84Position[1]);
                                }
                            }
                            channelList.add(channel);
                        }
                        sn = Integer.parseInt(snElement.getText());
                        catalogDataCatch.put(take.getDevice().getDeviceId(), sn, sumNum, take.getDevice(),
                                channelList, regionList, groupList);
                        log.info("[收到通道]设备: {} -> {}个，{}/{}", take.getDevice().getDeviceId(), channelList.size(), catalogDataCatch.size(take.getDevice().getDeviceId(), sn), sumNum);
                    }
                }
            } catch (Exception e) {
                log.warn("[收到通道] 发现未处理的异常, \r\n{}", evt.getRequest());
                log.error("[收到通道] 异常内容： ", e);
            } finally {
                if (catalogDataCatch.size(take.getDevice().getDeviceId(), sn) == catalogDataCatch.sumNum(take.getDevice().getDeviceId(), sn)) {
                    // 数据已经完整接收， 此时可能存在某个设备离线变上线的情况，但是考虑到性能，此处不做处理，
                    // 目前支持设备通道上线通知时和设备上线时向上级通知
                    boolean resetChannelsResult = saveData(take.getDevice(), sn);
                    if (!resetChannelsResult) {
                        String errorMsg = "接收成功，写入失败，共" + catalogDataCatch.sumNum(take.getDevice().getDeviceId(), sn) + "条，已接收" + catalogDataCatch.getDeviceChannelList(take.getDevice().getDeviceId(), sn).size() + "条";
                        catalogDataCatch.setChannelSyncEnd(take.getDevice().getDeviceId(), sn, errorMsg);
                    } else {
                        catalogDataCatch.setChannelSyncEnd(take.getDevice().getDeviceId(), sn, null);
                    }
                }
            }
        }
    }

    @Transactional
    public boolean saveData(Device device, int sn) {

        boolean result = true;
        List<DeviceChannel> deviceChannelList = catalogDataCatch.getDeviceChannelList(device.getDeviceId(), sn);
        if (deviceChannelList != null && !deviceChannelList.isEmpty()) {
            result &= deviceChannelService.resetChannels(device.getId(), deviceChannelList);
        }

        List<Region> regionList = catalogDataCatch.getRegionList(device.getDeviceId(), sn);
        if ( regionList!= null && !regionList.isEmpty()) {
            result &= regionService.batchAdd(regionList);
        }

        List<Group> groupList = catalogDataCatch.getGroupList(device.getDeviceId(), sn);
        if (groupList != null && !groupList.isEmpty()) {
            result &= groupService.batchAdd(groupList);
        }
        return result;
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element rootElement) {

    }

    public SyncStatus getChannelSyncProgress(String deviceId) {
        return catalogDataCatch.getSyncStatus(deviceId);
    }

    public boolean isSyncRunning(String deviceId) {
        return catalogDataCatch.isSyncRunning(deviceId);
    }

    public void setChannelSyncReady(Device device, int sn) {
        catalogDataCatch.addReady(device, sn);
    }

    public void setChannelSyncEnd(String deviceId, int sn, String errorMsg) {
        catalogDataCatch.setChannelSyncEnd(deviceId, sn, errorMsg);
    }
}
