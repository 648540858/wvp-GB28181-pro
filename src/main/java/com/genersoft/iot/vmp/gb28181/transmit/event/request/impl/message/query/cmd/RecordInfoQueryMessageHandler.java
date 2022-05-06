package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.record.RecordEndEventListener;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.QueryMessageHandler;
import com.genersoft.iot.vmp.gb28181.utils.DateUtil;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.dto.ChannelSourceInfo;
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
public class RecordInfoQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(RecordInfoQueryMessageHandler.class);
    private final String cmdType = "RecordInfo";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommanderFroPlatform cmderFroPlatform;

    @Autowired
    private SIPCommander commander;

    @Autowired
    private RecordEndEventListener recordEndEventListener;

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

        FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);

        Element snElement = rootElement.element("SN");
        int sn = Integer.parseInt(snElement.getText());
        Element deviceIDElement = rootElement.element("DeviceID");
        String channelId = deviceIDElement.getText();
        Element startTimeElement = rootElement.element("StartTime");
        String startTime = null;
        if (startTimeElement != null) {
            startTime = startTimeElement.getText();
        }
        Element endTimeElement = rootElement.element("EndTime");
        String endTime = null;
        if (endTimeElement != null) {
            endTime = endTimeElement.getText();
        }
        Element secrecyElement = rootElement.element("Secrecy");
        int secrecy = 0;
        if (secrecyElement != null) {
            secrecy = Integer.parseInt(secrecyElement.getText().trim());
        }
        String type = "all";
        Element typeElement = rootElement.element("Type");
        if (typeElement != null) {
            type =  typeElement.getText();
        }
        // 确认是直播还是国标， 国标直接请求下级，直播请求录像管理服务
        List<ChannelSourceInfo> channelSources = storager.getChannelSource(parentPlatform.getServerGBId(), channelId);

        if (channelSources.get(0).getCount() > 0) { // 国标
            // 向国标设备请求录像数据
            Device device = storager.queryVideoDeviceByPlatformIdAndChannelId(parentPlatform.getServerGBId(), channelId);
            DeviceChannel deviceChannel = storager.queryChannelInParentPlatform(parentPlatform.getServerGBId(), channelId);
            // 接收录像数据
            recordEndEventListener.addEndEventHandler(deviceChannel.getDeviceId(), channelId, (recordInfo)->{
                cmderFroPlatform.recordInfo(deviceChannel, parentPlatform, fromHeader.getTag(), recordInfo);
            });
            commander.recordInfoQuery(device, channelId, DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(startTime),
                    DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(endTime), sn, secrecy, type, (eventResult -> {
                        // 回复200 OK
                        try {
                            responseAck(evt, Response.OK);
                        } catch (SipException e) {
                            e.printStackTrace();
                        } catch (InvalidArgumentException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }),(eventResult -> {
                        // 查询失败
                        try {
                            responseAck(evt, eventResult.statusCode, eventResult.msg);
                        } catch (SipException e) {
                            e.printStackTrace();
                        } catch (InvalidArgumentException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }));

        }else if (channelSources.get(1).getCount() > 0) { // 直播流
            // TODO
            try {
                responseAck(evt, Response.NOT_IMPLEMENTED); // 回复未实现
            } catch (SipException e) {
                e.printStackTrace();
            } catch (InvalidArgumentException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }else { // 错误的请求
            try {
                responseAck(evt, Response.BAD_REQUEST);
            } catch (SipException e) {
                e.printStackTrace();
            } catch (InvalidArgumentException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
