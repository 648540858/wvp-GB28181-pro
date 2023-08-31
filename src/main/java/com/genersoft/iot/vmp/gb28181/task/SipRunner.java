package com.genersoft.iot.vmp.gb28181.task;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlatformService;
import com.genersoft.iot.vmp.service.impl.PlatformServiceImpl;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 系统启动时控制设备
 * @author lin
 */
@Component
@Order(value=14)
public class SipRunner implements CommandLineRunner {

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    private final static Logger logger = LoggerFactory.getLogger(PlatformServiceImpl.class);

    @Override
    public void run(String... args) throws Exception {
        List<Device> deviceList = deviceService.getAllOnlineDevice();

        for (Device device : deviceList) {
            if (deviceService.expire(device)){
                deviceService.offline(device.getDeviceId(), "注册已过期");
            }else {
                deviceService.online(device, null);
            }
        }
        // 重置cseq计数
        redisCatchStorage.resetAllCSEQ();
        // 清理redis
        // 清理数据库不存在但是redis中存在的数据
        List<Device> devicesInDb = deviceService.getAll();
        if (devicesInDb.size() == 0) {
            redisCatchStorage.removeAllDevice();
        }else {
            List<Device> devicesInRedis = redisCatchStorage.getAllDevices();
            if (devicesInRedis.size() > 0) {
                Map<String, Device> deviceMapInDb = new HashMap<>();
                devicesInDb.parallelStream().forEach(device -> {
                    deviceMapInDb.put(device.getDeviceId(), device);
                });
                devicesInRedis.parallelStream().forEach(device -> {
                    if (deviceMapInDb.get(device.getDeviceId()) == null) {
                        redisCatchStorage.removeDevice(device.getDeviceId());
                    }
                });
            }
        }


        // 查找国标推流
        List<SendRtpItem> sendRtpItems = redisCatchStorage.queryAllSendRTPServer();
        if (sendRtpItems.size() > 0) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                MediaServerItem mediaServerItem = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                redisCatchStorage.deleteSendRTPServer(sendRtpItem.getPlatformId(),sendRtpItem.getChannelId(), sendRtpItem.getCallId(),sendRtpItem.getStreamId());
                if (mediaServerItem != null) {
                    ssrcFactory.releaseSsrc(sendRtpItem.getMediaServerId(), sendRtpItem.getSsrc());
                    Map<String, Object> param = new HashMap<>();
                    param.put("vhost","__defaultVhost__");
                    param.put("app",sendRtpItem.getApp());
                    param.put("stream",sendRtpItem.getStreamId());
                    param.put("ssrc",sendRtpItem.getSsrc());
                    JSONObject jsonObject = zlmresTfulUtils.stopSendRtp(mediaServerItem, param);
                    if (jsonObject != null && jsonObject.getInteger("code") == 0) {
                        ParentPlatform platform = platformService.queryPlatformByServerGBId(sendRtpItem.getPlatformId());
                        if (platform != null) {
                            try {
                                commanderForPlatform.streamByeCmd(platform, sendRtpItem.getCallId());
                            } catch (InvalidArgumentException | ParseException | SipException e) {
                                logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }
}
