package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.QueryMessageHandler;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class CatalogQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(CatalogQueryMessageHandler.class);
    private final String cmdType = "Catalog";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommanderFroPlatform cmderFroPlatform;

    @Autowired
    private SipConfig config;

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private IVideoManagerStorage storage;

    @Override
    public void afterPropertiesSet() throws Exception {
        queryMessageHandler.addHandler(cmdType, this);
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
            List<DeviceChannelInPlatform> deviceChannelInPlatforms = storager.queryChannelListInParentPlatform(parentPlatform.getServerGBId());
            // 查询关联的直播通道
            List<GbStream> gbStreams = storager.queryGbStreamListInPlatform(parentPlatform.getServerGBId());
            // 回复目录信息
            List<PlatformCatalog> catalogs =  storager.queryCatalogInPlatform(parentPlatform.getServerGBId());

            List<DeviceChannel> allChannels = new ArrayList<>();
            if (catalogs.size() > 0) {
                for (PlatformCatalog catalog : catalogs) {
                    if (catalog.getParentId().equals(catalog.getPlatformId())) {
                        catalog.setParentId(parentPlatform.getDeviceGBId());
                    }
                    DeviceChannel deviceChannel = new DeviceChannel();
                    // 通道的类型，0->国标通道 1->直播流通道 2->业务分组/虚拟组织/行政区划
                    deviceChannel.setChannelType(2);
                    deviceChannel.setChannelId(catalog.getId());
                    deviceChannel.setName(catalog.getName());
                    deviceChannel.setDeviceId(parentPlatform.getDeviceGBId());
                    deviceChannel.setManufacture("wvp-pro");
                    deviceChannel.setStatus(1);
                    deviceChannel.setParental(1);
                    deviceChannel.setParentId(catalog.getParentId());
                    deviceChannel.setRegisterWay(1);
                    if (catalog.getParentId() != null &&  catalog.getParentId().length() < 10) {
                        deviceChannel.setCivilCode(catalog.getParentId());
                    }else {
                        deviceChannel.setCivilCode(parentPlatform.getAdministrativeDivision());
                    }
                    allChannels.add(deviceChannel);
                }
            }
            // 回复级联的通道
            if (deviceChannelInPlatforms.size() > 0) {
                for (DeviceChannelInPlatform channel : deviceChannelInPlatforms) {
                    if (channel.getCatalogId().equals(parentPlatform.getServerGBId())) {
                        channel.setCatalogId(parentPlatform.getDeviceGBId());
                    }
                    DeviceChannel deviceChannel = storage.queryChannel(channel.getDeviceId(), channel.getChannelId());
                    // 通道的类型，0->国标通道 1->直播流通道 2->业务分组/虚拟组织/行政区划
                    deviceChannel.setChannelType(0);
                    deviceChannel.setParental(0);
                    deviceChannel.setParentId(channel.getCatalogId());
                    if (channel.getCatalogId() != null && channel.getCatalogId().length() < 10) {
                        deviceChannel.setCivilCode(channel.getCatalogId());
                    }else {
                        deviceChannel.setCivilCode(parentPlatform.getAdministrativeDivision());
                    }
                    allChannels.add(deviceChannel);
                }
            }
            // 回复直播的通道
            if (gbStreams.size() > 0) {
                for (GbStream gbStream : gbStreams) {
                    if (gbStream.getCatalogId().equals(parentPlatform.getServerGBId())) {
                        gbStream.setCatalogId(null);
                    }
                    DeviceChannel deviceChannel = new DeviceChannel();
                    // 通道的类型，0->国标通道 1->直播流通道 2->业务分组/虚拟组织/行政区划
                    deviceChannel.setChannelType(1);
                    deviceChannel.setChannelId(gbStream.getGbId());
                    deviceChannel.setName(gbStream.getName());
                    deviceChannel.setLongitude(gbStream.getLongitude());
                    deviceChannel.setLatitude(gbStream.getLatitude());
                    deviceChannel.setDeviceId(parentPlatform.getDeviceGBId());
                    deviceChannel.setManufacture("wvp-pro");
//                    deviceChannel.setStatus(gbStream.isStatus()?1:0);
                    deviceChannel.setStatus(1);
                    deviceChannel.setParentId(gbStream.getCatalogId());
                    deviceChannel.setRegisterWay(1);
                    if (gbStream.getCatalogId() != null && gbStream.getCatalogId().length() < 10) {
                        deviceChannel.setCivilCode(gbStream.getCatalogId());
                    }else {
                        deviceChannel.setCivilCode(parentPlatform.getAdministrativeDivision());
                    }
                    deviceChannel.setModel("live");
                    deviceChannel.setOwner("wvp-pro");
                    deviceChannel.setParental(0);
                    deviceChannel.setSecrecy("0");
                    allChannels.add(deviceChannel);
                }
            }
            if (allChannels.size() > 0) {
                cmderFroPlatform.catalogQuery(allChannels, parentPlatform, sn, fromHeader.getTag());
            }else {
                // 回复无通道
                cmderFroPlatform.catalogQuery(null, parentPlatform, sn, fromHeader.getTag(), 0);
            }
        } catch (SipException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}
