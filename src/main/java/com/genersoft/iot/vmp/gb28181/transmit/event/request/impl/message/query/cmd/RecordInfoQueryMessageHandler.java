package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.record.RecordEndEventListener;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.QueryMessageHandler;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IPlatformChannelService;
import com.genersoft.iot.vmp.service.IResourceService;
import gov.nist.javax.sip.message.SIPRequest;
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
import java.util.Map;

@Component
public class RecordInfoQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(RecordInfoQueryMessageHandler.class);
    private final String cmdType = "RecordInfo";

    @Autowired
    private QueryMessageHandler queryMessageHandler;


    @Autowired
    private ISIPCommanderForPlatform cmderFroPlatform;

    @Autowired
    private SIPCommander commander;

    @Autowired
    private RecordEndEventListener recordEndEventListener;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private Map<String, IResourceService> resourceServiceMap;

    @Override
    public void afterPropertiesSet() throws Exception {
        queryMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

        logger.info("[国标级联] 录像查询， 平台：{}",parentPlatform.getServerGBId());
        SIPRequest request = (SIPRequest) evt.getRequest();
        try {
            responseAck(request, Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 错误信息: {}", e.getMessage());
        }

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
        logger.info("[国标级联] 录像查询， 平台：{}， 通道： {}, 时间： {}-{}",
                parentPlatform.getServerGBId(), channelId, startTime, endTime);
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
        CommonGbChannel commonGbChannel = platformChannelService.queryChannelByPlatformIdAndChannelDeviceId(parentPlatform.getId(), channelId);
        if (commonGbChannel == null) {
            logger.error("[DeviceInfo查询]失败：平台未关联通道 platformId {}, channelId: {}", parentPlatform.getServerGBId(), channelId);
            return;
        }
        IResourceService resourceService = resourceServiceMap.get(commonGbChannel.getType());
        if (resourceService == null) {
            logger.error("[DeviceInfo查询]失败：平台未关联通道 platformId {}, channelId: {}", parentPlatform.getServerGBId(), channelId);
            return;
        }

        resourceService.queryrecord(commonGbChannel, sn, secrecy, type, startTime, endTime, (recordInfo -> {
            if (recordInfo == null ) {
                logger.info("[DeviceInfo查询] 录像查询, 结果为空，platformId {}, channelId: {}",
                        parentPlatform.getServerGBId(), channelId);
                return;
            }
            if (recordInfo.getRecordList() == null) {
                recordInfo.setRecordList(new ArrayList<>());
            }
            // 向上级发送录像数据
            logger.info("[国标级联]录像查询收到数据， 共{}条, platformId {}, channelId: {}",
                    recordInfo.getRecordList().size(), parentPlatform.getServerGBId(), channelId);
            try {
                cmderFroPlatform.recordInfo(commonGbChannel, parentPlatform, request.getFromTag(), recordInfo);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                logger.error("[命令发送失败] 国标级联-录像查询: {}", e.getMessage());
            }
        }));


//        if (commonGbChannel.getType().equals(CommonGbChannelType.GB28181)) { // 国标
//            // 向国标设备请求录像数据
//            Device device = deviceChannelService.getDeviceByChannelCommonGbId(commonGbChannel.getCommonGbId());
//
//            // 接收录像数据
//            recordEndEventListener.addEndEventHandler(device.getDeviceId(), channelId, (recordInfo)->{
//                try {
//                    logger.info("[国标级联] 录像查询收到数据， 通道： {}，准备转发===", channelId);
//                    cmderFroPlatform.recordInfo(commonGbChannel, parentPlatform, request.getFromTag(), recordInfo);
//                } catch (SipException | InvalidArgumentException | ParseException e) {
//                    logger.error("[命令发送失败] 国标级联 回复录像数据: {}", e.getMessage());
//                }
//            });
//            try {
//                commander.recordInfoQuery(device, channelId, DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(startTime),
//                        DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(endTime), sn, secrecy, type, (eventResult -> {
//                            // 回复200 OK
//                            try {
//                                responseAck(request, Response.OK);
//                            } catch (SipException | InvalidArgumentException | ParseException e) {
//                                logger.error("[命令发送失败] 录像查询回复: {}", e.getMessage());
//                            }
//                        }),(eventResult -> {
//                            // 查询失败
//                            try {
//                                responseAck(request, eventResult.statusCode, eventResult.msg);
//                            } catch (SipException | InvalidArgumentException | ParseException e) {
//                                logger.error("[命令发送失败] 录像查询回复: {}", e.getMessage());
//                            }
//                        }));
//            } catch (InvalidArgumentException | ParseException | SipException e) {
//                logger.error("[命令发送失败] 录像查询: {}", e.getMessage());
//            }
//
//        }else if (commonGbChannel.getType().equals(CommonGbChannelType.PUSH)
//                || commonGbChannel.getType().equals(CommonGbChannelType.PROXY)) { // 直播流
//            // TODO
//            try {
//                responseAck(request, Response.NOT_IMPLEMENTED); // 回复未实现
//            } catch (SipException | InvalidArgumentException | ParseException e) {
//                logger.error("[命令发送失败] 录像查询: {}", e.getMessage());
//            }
//        }else { // 错误的请求
//            try {
//                responseAck(request, Response.BAD_REQUEST);
//            } catch (SipException | InvalidArgumentException | ParseException e) {
//                logger.error("[命令发送失败] 录像查询: {}", e.getMessage());
//            }
//        }
    }
}
