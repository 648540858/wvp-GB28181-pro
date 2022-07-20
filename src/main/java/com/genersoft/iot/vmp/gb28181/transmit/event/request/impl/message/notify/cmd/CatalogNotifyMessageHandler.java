package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.NotifyMessageHandler;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CatalogNotifyMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "Catalog";

    @Autowired
    private NotifyMessageHandler notifyMessageHandler;

    @Autowired
    private IVideoManagerStorage storage;

    @Autowired
    private SIPCommanderFroPlatform cmderFroPlatform;

    @Override
    public void afterPropertiesSet() throws Exception {
        notifyMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

        String key = DeferredResultHolder.CALLBACK_CMD_CATALOG + parentPlatform.getServerGBId();
        FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
        try {
            // 回复200 OK
            responseAck(evt, Response.OK);
            Element snElement = rootElement.element("SN");
            String sn = snElement.getText();
            // 准备回复通道信息
            List<DeviceChannelInPlatform> deviceChannels = storage.queryChannelListInParentPlatform(parentPlatform.getServerGBId());
            // 查询关联的直播通道
            List<DeviceChannel> gbStreams = storage.queryGbStreamListInPlatform(parentPlatform.getServerGBId());
            // 回复目录信息
            List<DeviceChannel> catalogs =  storage.queryCatalogInPlatform(parentPlatform.getServerGBId());

            List<DeviceChannel> allChannels = new ArrayList<>();
            if (catalogs.size() > 0) {
                allChannels.addAll(catalogs);
            }
            // 回复级联的通道
            if (deviceChannels.size() > 0) {
                allChannels.addAll(deviceChannels);
            }
            // 回复直播的通道
            if (gbStreams.size() > 0) {
                allChannels.addAll(gbStreams);
            }
            if (allChannels.size() > 0) {
                cmderFroPlatform.catalogQuery(allChannels, parentPlatform, sn, fromHeader.getTag());
            }else {
                // 回复无通道
                cmderFroPlatform.catalogQuery(null, parentPlatform, sn, fromHeader.getTag(), 0);
            }
        } catch (SipException | InvalidArgumentException | ParseException e) {
            e.printStackTrace();
        }

    }
}
