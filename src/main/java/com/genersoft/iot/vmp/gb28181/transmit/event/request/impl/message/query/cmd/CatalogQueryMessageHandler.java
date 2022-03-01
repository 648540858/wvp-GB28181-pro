package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.QueryMessageHandler;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
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
import java.util.List;

@Component
public class CatalogQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(CatalogQueryMessageHandler.class);
    private final String cmdType = "Catalog";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private SIPCommanderFroPlatform cmderFroPlatform;

    @Autowired
    private SipConfig config;

    @Autowired
    private EventPublisher publisher;

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
            List<ChannelReduce> channelReduces = storager.queryChannelListInParentPlatform(parentPlatform.getServerGBId());
            // 查询关联的直播通道
            List<GbStream> gbStreams = storager.queryGbStreamListInPlatform(parentPlatform.getServerGBId());
            // 回复目录信息
            List<PlatformCatalog> catalogs =  storager.queryCatalogInPlatform(parentPlatform.getServerGBId());
            int size = catalogs.size() + channelReduces.size() + gbStreams.size();
            if (catalogs.size() > 0) {
                for (PlatformCatalog catalog : catalogs) {
                    if (catalog.getParentId().equals(parentPlatform.getServerGBId())) {
                        catalog.setParentId(parentPlatform.getDeviceGBId());
                    }
                    DeviceChannel deviceChannel = new DeviceChannel();
                    deviceChannel.setChannelId(catalog.getId());
                    deviceChannel.setName(catalog.getName());
                    deviceChannel.setLongitude(0.0);
                    deviceChannel.setLatitude(0.0);
                    deviceChannel.setDeviceId(parentPlatform.getDeviceGBId());
                    deviceChannel.setManufacture("wvp-pro");
                    deviceChannel.setStatus(1);
                    deviceChannel.setParental(1);
                    deviceChannel.setParentId(catalog.getParentId());
                    deviceChannel.setRegisterWay(1);
                    deviceChannel.setCivilCode(config.getDomain().substring(0, config.getDomain().length() - 2));
                    deviceChannel.setModel("live");
                    deviceChannel.setOwner("wvp-pro");
                    deviceChannel.setSecrecy("0");
                    cmderFroPlatform.catalogQuery(deviceChannel, parentPlatform, sn, fromHeader.getTag(), size);
                    // 防止发送过快
                    Thread.sleep(100);
                }
            }
            // 回复级联的通道
            if (channelReduces.size() > 0) {
                for (ChannelReduce channelReduce : channelReduces) {
                    if (channelReduce.getCatalogId().equals(parentPlatform.getServerGBId())) {
                        channelReduce.setCatalogId(parentPlatform.getDeviceGBId());
                    }
                    DeviceChannel deviceChannel = storager.queryChannel(channelReduce.getDeviceId(), channelReduce.getChannelId());
                    deviceChannel.setParental(0);
                    deviceChannel.setParentId(channelReduce.getCatalogId());
                    cmderFroPlatform.catalogQuery(deviceChannel, parentPlatform, sn, fromHeader.getTag(), size);
                    // 防止发送过快
                    Thread.sleep(100);
                }
            }
            // 回复直播的通道
            if (gbStreams.size() > 0) {
                for (GbStream gbStream : gbStreams) {
                    if (gbStream.getCatalogId().equals(parentPlatform.getServerGBId())) {
                        gbStream.setCatalogId(parentPlatform.getDeviceGBId());
                    }
                    DeviceChannel deviceChannel = new DeviceChannel();
                    deviceChannel.setChannelId(gbStream.getGbId());
                    deviceChannel.setName(gbStream.getName());
                    deviceChannel.setLongitude(gbStream.getLongitude());
                    deviceChannel.setLatitude(gbStream.getLatitude());
                    deviceChannel.setDeviceId(parentPlatform.getDeviceGBId());
                    deviceChannel.setManufacture("wvp-pro");
                    deviceChannel.setStatus(gbStream.isStatus()?1:0);
                    deviceChannel.setParentId(gbStream.getCatalogId());
                    deviceChannel.setRegisterWay(1);
                    deviceChannel.setCivilCode(config.getDomain().substring(0, config.getDomain().length() - 2));
                    deviceChannel.setModel("live");
                    deviceChannel.setOwner("wvp-pro");
                    deviceChannel.setParental(0);
                    deviceChannel.setSecrecy("0");

                    cmderFroPlatform.catalogQuery(deviceChannel, parentPlatform, sn, fromHeader.getTag(), size);
                    // 防止发送过快
                    Thread.sleep(100);
                }
            }
            if (size == 0) {
                // 回复无通道
                cmderFroPlatform.catalogQuery(null, parentPlatform, sn, fromHeader.getTag(), size);
            }
        } catch (SipException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
