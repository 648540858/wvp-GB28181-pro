package com.genersoft.iot.vmp.gb28181.task;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@Order(value=14)
public class SipRunner implements CommandLineRunner {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private UserSetting userSetting;

    @Override
    public void run(String... args) throws Exception {
        List<Device> deviceList = deviceService.getAllOnlineDevice(userSetting.getServerId());

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
        if (devicesInDb.isEmpty()) {
            redisCatchStorage.removeAllDevice();
        }else {
            List<Device> devicesInRedis = redisCatchStorage.getAllDevices();
            if (!devicesInRedis.isEmpty()) {
                Map<String, Device> deviceMapInDb = new HashMap<>();
                devicesInDb.parallelStream().forEach(device -> {
                    deviceMapInDb.put(device.getDeviceId(), device);
                });
                devicesInRedis.parallelStream().forEach(device -> {
                    if (deviceMapInDb.get(device.getDeviceId()) == null
                            && userSetting.getServerId().equals(device.getServerId())) {
                        redisCatchStorage.removeDevice(device.getDeviceId());
                    }
                });
            }
        }


        // 查找国标推流
        List<SendRtpInfo> sendRtpItems = redisCatchStorage.queryAllSendRTPServer();
        if (!sendRtpItems.isEmpty()) {
            for (SendRtpInfo sendRtpItem : sendRtpItems) {
                MediaServer mediaServerItem = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                CommonGBChannel channel = channelService.getOne(sendRtpItem.getChannelId());
                if (channel == null){
                    continue;
                }
                sendRtpServerService.delete(sendRtpItem);
                if (mediaServerItem != null) {
                    ssrcFactory.releaseSsrc(sendRtpItem.getMediaServerId(), sendRtpItem.getSsrc());
                    boolean stopResult = mediaServerService.initStopSendRtp(mediaServerItem, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getSsrc());
                    if (stopResult) {
                        Platform platform = platformService.queryPlatformByServerGBId(sendRtpItem.getTargetId());

                        if (platform != null) {
                            try {
                                commanderForPlatform.streamByeCmd(platform, sendRtpItem, channel);
                            } catch (InvalidArgumentException | ParseException | SipException e) {
                                log.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }


}
