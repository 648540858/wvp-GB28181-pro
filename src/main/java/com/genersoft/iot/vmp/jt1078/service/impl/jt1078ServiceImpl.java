package com.genersoft.iot.vmp.jt1078.service.impl;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.ftpServer.FtpFileSystemFactory;
import com.genersoft.iot.vmp.conf.ftpServer.FtpSetting;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.bean.config.JTAloneChanel;
import com.genersoft.iot.vmp.jt1078.bean.config.JTChannelListParam;
import com.genersoft.iot.vmp.jt1078.bean.config.JTChannelParam;
import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.dao.JTChannelMapper;
import com.genersoft.iot.vmp.jt1078.dao.JTTerminalMapper;
import com.genersoft.iot.vmp.jt1078.event.RegisterEvent;
import com.genersoft.iot.vmp.jt1078.proc.response.*;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.FtpDownloadManager;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.SynchronousQueue;

@Service
@Slf4j
public class jt1078ServiceImpl implements Ijt1078Service {

    @Autowired
    private JTTerminalMapper jtDeviceMapper;

    @Autowired
    private JTChannelMapper jtChannelMapper;

    @Autowired
    private JT1078Template jt1078Template;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private FtpSetting ftpSetting;

    @Autowired
    private FtpFileSystemFactory fileSystemFactory;

    @Autowired
    private FtpDownloadManager downloadManager;

    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {

    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {

    }

    /**
     * 设备注册的通知
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(RegisterEvent event) {
        // 首次注册设备根据终端参数获取
        JTDevice device = event.getDevice();
        List<JTChannel> channelList = jtChannelMapper.selectAllByDevicePhoneNumber(device.getPhoneNumber());
        if (!channelList.isEmpty()) {
            return;
        }
        JTDeviceConfig jtDeviceConfig = queryConfig(device.getPhoneNumber(), null);
        JTChannelParam channelParam = jtDeviceConfig.getChannelParam();
        if (channelParam != null && channelParam.getJtAloneChanelList() != null && !channelParam.getJtAloneChanelList().isEmpty()) {
           // 写入通道
            List<JTAloneChanel> jtAloneChanelList = channelParam.getJtAloneChanelList();
            for (JTAloneChanel jtAloneChanel : jtAloneChanelList) {

            }
        }

    }


    /**
     * 校验流是否是属于部标的
     */
    @Override
    public JTMediaStreamType checkStreamFromJt(String stream) {
        String[] streamParamArray = stream.split("_");
        if (streamParamArray.length == 2) {
            return JTMediaStreamType.PLAY;
        }else if (streamParamArray.length == 4) {
            return JTMediaStreamType.PLAYBACK;
        }else if (streamParamArray.length == 5) {
            return JTMediaStreamType.TALK;
        }else {
            return null;
        }
    }

    @Override
    public JTDevice getDevice(String phoneNumber) {
        return jtDeviceMapper.getDevice(phoneNumber);
    }

    @Override
    public JTChannel getChannel(Integer terminalDbId, Integer channelId) {
        return jtChannelMapper.selectChannelByChannelId(terminalDbId, channelId);
    }

    @Override
    public void updateDevice(JTDevice device) {
        device.setUpdateTime(DateUtil.getNow());
        jtDeviceMapper.updateDevice(device);
    }

    @Override
    public PageInfo<JTDevice> getDeviceList(int page, int count, String query, Boolean online) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<JTDevice> all = jtDeviceMapper.getDeviceList(query, online);
        return new PageInfo<>(all);
    }

    @Override
    public void addDevice(JTDevice device) {
        JTDevice deviceInDb = jtDeviceMapper.getDevice(device.getPhoneNumber());
        if (deviceInDb != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备" + device.getPhoneNumber() + "已存在");
        }
        device.setCreateTime(DateUtil.getNow());
        device.setUpdateTime(DateUtil.getNow());
        jtDeviceMapper.addDevice(device);
    }

    @Override
    public void deleteDeviceByPhoneNumber(String phoneNumber) {
        jtDeviceMapper.deleteDeviceByPhoneNumber(phoneNumber);
    }

    @Override
    public void updateDeviceStatus(boolean connected, String phoneNumber) {
        jtDeviceMapper.updateDeviceStatus(connected, phoneNumber);
    }


    @Override
    public void recordDownload(String phoneNumber, Integer channelId, String startTime, String endTime, Integer alarmSign,
                               Integer mediaType, Integer streamType, Integer storageType, OutputStream outputStream, CommonCallback<WVPResult<String>> fileCallback) {
        String filePath = UUID.randomUUID().toString();
        fileSystemFactory.addOutputStream(filePath, outputStream);
        dynamicTask.startDelay(filePath, ()->{
            fileSystemFactory.removeOutputStream(filePath);
        }, 2*60*60*1000);
        log.info("[JT-录像] 下载，设备:{}， 通道： {}， 开始时间： {}， 结束时间： {}，等待上传文件路径： {} ",
                phoneNumber, channelId, startTime, endTime, filePath);
        // 发送停止命令
        J9206 j92026 = new J9206();
        j92026.setChannelId(channelId);
        j92026.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime));
        j92026.setEndTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime));
        j92026.setServerIp(ftpSetting.getIp());
        j92026.setPort(ftpSetting.getPort());
        j92026.setUsername(ftpSetting.getUsername());
        j92026.setPassword(ftpSetting.getPassword());
        j92026.setPath(filePath);

        if (mediaType != null) {
            j92026.setMediaType(mediaType);
        }
        if (streamType != null) {
            j92026.setStreamType(streamType);
        }
        if (storageType != null) {
            j92026.setStorageType(storageType);
        }
        if (alarmSign != null) {
            j92026.setAlarmSign(alarmSign);
        }
        jt1078Template.fileUpload(phoneNumber, j92026, 7200);
    }

    @Override
    public void ptzControl(String phoneNumber, Integer channelId, String command, int speed) {
        // 发送停止命令
        switch (command) {
            case "left":
            case "right":
            case "up":
            case "down":
            case "stop":
                J9301 j9301 = new J9301();
                j9301.setChannel(channelId);
                switch (command) {
                    case "left":
                        j9301.setDirection(3);
                        j9301.setSpeed(speed);
                        break;
                    case "right":
                        j9301.setDirection(4);
                        j9301.setSpeed(speed);
                        break;
                    case "up":
                        j9301.setDirection(1);
                        j9301.setSpeed(speed);
                        break;
                    case "down":
                        j9301.setDirection(2);
                        j9301.setSpeed(speed);
                        break;
                    case "stop":
                        j9301.setDirection(0);
                        j9301.setSpeed(0);
                        break;
                }
                jt1078Template.ptzRotate(phoneNumber, j9301, 6);
                break;

            case "zoomin":
            case "zoomout":
                J9306 j9306 = new J9306();
                j9306.setChannel(channelId);
                if (command.equals("zoomin")) {
                    j9306.setZoom(0);
                } else {
                    j9306.setZoom(1);
                }
                jt1078Template.ptzZoom(phoneNumber, j9306, 6);
                break;
            case "irisin":
            case "irisout":
                J9303 j9303 = new J9303();
                j9303.setChannel(channelId);
                if (command.equals("irisin")) {
                    j9303.setIris(0);
                } else {
                    j9303.setIris(1);
                }
                jt1078Template.ptzIris(phoneNumber, j9303, 6);
                break;
            case "focusnear":
            case "focusfar":
                J9302 j9302 = new J9302();
                j9302.setChannel(channelId);
                if (command.equals("focusfar")) {
                    j9302.setFocalDirection(0);
                } else {
                    j9302.setFocalDirection(1);
                }
                jt1078Template.ptzFocal(phoneNumber, j9302, 6);
                break;

        }
    }

    @Override
    public void supplementaryLight(String phoneNumber, Integer channelId, String command) {
        J9305 j9305 = new J9305();
        j9305.setChannel(channelId);
        if (command.equalsIgnoreCase("on")) {
            j9305.setOn(1);
        } else {
            j9305.setOn(0);
        }
        jt1078Template.ptzSupplementaryLight(phoneNumber, j9305, 6);
    }

    @Override
    public void wiper(String phoneNumber, Integer channelId, String command) {
        J9304 j9304 = new J9304();
        j9304.setChannel(channelId);
        if (command.equalsIgnoreCase("on")) {
            j9304.setOn(1);
        } else {
            j9304.setOn(0);
        }
        jt1078Template.ptzWiper(phoneNumber, j9304, 6);
    }

    @Override
    public JTDeviceConfig queryConfig(String phoneNumber, String[] params) {
        if (phoneNumber == null) {
            return null;
        }
        if (params == null || params.length == 0) {
            J8104 j8104 = new J8104();
            return (JTDeviceConfig) jt1078Template.getDeviceConfig(phoneNumber, j8104, 20);
        } else {
            long[] paramBytes = new long[params.length];
            for (int i = 0; i < params.length; i++) {
                try {
                    Field field = JTDeviceConfig.class.getDeclaredField(params[i]);
                    if (field.isAnnotationPresent(ConfigAttribute.class)) {
                        ConfigAttribute configAttribute = field.getAnnotation(ConfigAttribute.class);
                        long id = configAttribute.id();
                        String description = configAttribute.description();
                        System.out.println(description + ":  " + id);
                        paramBytes[i] = configAttribute.id();
                    }
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }
            J8106 j8106 = new J8106();
            j8106.setParams(paramBytes);
            return (JTDeviceConfig) jt1078Template.getDeviceSpecifyConfig(phoneNumber, j8106, 20);
        }
    }

    @Override
    public void setConfig(String phoneNumber, JTDeviceConfig config) {
        J8103 j8103 = new J8103();
        j8103.setConfig(config);
        jt1078Template.setDeviceSpecifyConfig(phoneNumber, j8103, 6);
    }

    @Override
    public void connectionControl(String phoneNumber, JTDeviceConnectionControl control) {
        J8105 j8105 = new J8105();
        j8105.setConnectionControl(control);
        jt1078Template.deviceControl(phoneNumber, j8105, 6);
    }

    @Override
    public void resetControl(String phoneNumber) {
        J8105 j8105 = new J8105();
        j8105.setReset(true);
        jt1078Template.deviceControl(phoneNumber, j8105, 6);
    }

    @Override
    public void factoryResetControl(String phoneNumber) {
        J8105 j8105 = new J8105();
        j8105.setFactoryReset(true);
        jt1078Template.deviceControl(phoneNumber, j8105, 6);
    }

    @Override
    public JTDeviceAttribute attribute(String phoneNumber) {
        J8107 j8107 = new J8107();
        return (JTDeviceAttribute) jt1078Template.deviceAttribute(phoneNumber, j8107, 20);
    }

    @Override
    public JTPositionBaseInfo queryPositionInfo(String phoneNumber) {
        J8201 j8201 = new J8201();
        return (JTPositionBaseInfo) jt1078Template.queryPositionInfo(phoneNumber, j8201, 20);
    }

    @Override
    public void tempPositionTrackingControl(String phoneNumber, Integer timeInterval, Long validityPeriod) {
        J8202 j8202 = new J8202();
        j8202.setTimeInterval(timeInterval);
        j8202.setValidityPeriod(validityPeriod);
        jt1078Template.tempPositionTrackingControl(phoneNumber, j8202, 20);
    }

    @Override
    public void confirmationAlarmMessage(String phoneNumber, int alarmPackageNo, JTConfirmationAlarmMessageType alarmMessageType) {
        J8203 j8203 = new J8203();
        j8203.setAlarmMessageType(alarmMessageType);
        j8203.setAlarmPackageNo(alarmPackageNo);
        jt1078Template.confirmationAlarmMessage(phoneNumber, j8203, 6);
    }

    @Override
    public int linkDetection(String phoneNumber) {
        J8204 j8204 = new J8204();
        Object result = jt1078Template.linkDetection(phoneNumber, j8204, 6);
        if (result == null) {
            return 1;
        }else {
            return (int) result;
        }
    }

    @Override
    public int textMessage(String phoneNumber, JTTextSign sign, int textType, String content) {
        J8300 j8300 = new J8300();
        j8300.setSign(sign);
        j8300.setTextType(textType);
        j8300.setContent(content);
        return (int) jt1078Template.textMessage(phoneNumber, j8300, 6);
    }

    @Override
    public int telephoneCallback(String phoneNumber, Integer sign, String destPhoneNumber) {
        J8400 j8400 = new J8400();
        j8400.setSign(sign);
        j8400.setPhoneNumber(destPhoneNumber);
        return (int) jt1078Template.telephoneCallback(phoneNumber, j8400, 6);
    }

    @Override
    public int setPhoneBook(String phoneNumber, int type, List<JTPhoneBookContact> phoneBookContactList) {
        J8401 j8401 = new J8401();
        j8401.setType(type);
        if (phoneBookContactList != null) {
            j8401.setPhoneBookContactList(phoneBookContactList);
        }
        return (int) jt1078Template.setPhoneBook(phoneNumber, j8401, 6);
    }

    @Override
    public JTPositionBaseInfo controlDoor(String phoneNumber, Boolean open) {
        J8500 j8500 = new J8500();
        JTVehicleControl jtVehicleControl = new JTVehicleControl();
        jtVehicleControl.setControlCarDoor(open ? 1 : 0);
        j8500.setVehicleControl(jtVehicleControl);
        return (JTPositionBaseInfo) jt1078Template.vehicleControl(phoneNumber, j8500, 20);
    }

    @Override
    public int setAreaForCircle(int attribute, String phoneNumber, List<JTCircleArea> circleAreaList) {
        J8600 j8600 = new J8600();
        j8600.setAttribute(attribute);
        j8600.setCircleAreaList(circleAreaList);
        return (int) jt1078Template.setAreaForCircle(phoneNumber, j8600, 20);
    }

    @Override
    public int deleteAreaForCircle(String phoneNumber, List<Long> ids) {
        J8601 j8601 = new J8601();
        j8601.setIdList(ids);
        return (int) jt1078Template.deleteAreaForCircle(phoneNumber, j8601, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryAreaForCircle(String phoneNumber, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(1);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>) jt1078Template.queryAreaOrRoute(phoneNumber, j8608, 20);
    }

    @Override
    public int setAreaForRectangle(int attribute, String phoneNumber, List<JTRectangleArea> rectangleAreas) {
        J8602 j8602 = new J8602();
        j8602.setAttribute(attribute);
        j8602.setRectangleAreas(rectangleAreas);
        return (int) jt1078Template.setAreaForRectangle(phoneNumber, j8602, 20);
    }

    @Override
    public int deleteAreaForRectangle(String phoneNumber, List<Long> ids) {
        J8603 j8603 = new J8603();
        j8603.setIdList(ids);
        return (int) jt1078Template.deleteAreaForRectangle(phoneNumber, j8603, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryAreaForRectangle(String phoneNumber, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(2);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>) jt1078Template.queryAreaOrRoute(phoneNumber, j8608, 20);
    }

    @Override
    public int setAreaForPolygon(String phoneNumber, JTPolygonArea polygonArea) {
        J8604 j8604 = new J8604();
        j8604.setPolygonArea(polygonArea);
        return (int) jt1078Template.setAreaForPolygon(phoneNumber, j8604, 20);
    }

    @Override
    public int deleteAreaForPolygon(String phoneNumber, List<Long> ids) {
        J8605 j8605 = new J8605();
        j8605.setIdList(ids);
        return (int) jt1078Template.deleteAreaForPolygon(phoneNumber, j8605, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryAreaForPolygon(String phoneNumber, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(3);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>) jt1078Template.queryAreaOrRoute(phoneNumber, j8608, 20);
    }

    @Override
    public int setRoute(String phoneNumber, JTRoute route) {
        J8606 j8606 = new J8606();
        j8606.setRoute(route);
        return (int) jt1078Template.setRoute(phoneNumber, j8606, 20);
    }

    @Override
    public int deleteRoute(String phoneNumber, List<Long> ids) {
        J8607 j8607 = new J8607();
        j8607.setIdList(ids);
        return (int) jt1078Template.deleteRoute(phoneNumber, j8607, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryRoute(String phoneNumber, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(4);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>) jt1078Template.queryAreaOrRoute(phoneNumber, j8608, 20);
    }

    @Override
    public JTDriverInformation queryDriverInformation(String phoneNumber) {
        J8702 j8702 = new J8702();
        return (JTDriverInformation) jt1078Template.queryDriverInformation(phoneNumber, j8702, 20);
    }

    @Override
    public List<Long> shooting(String phoneNumber, JTShootingCommand shootingCommand) {
        J8801 j8801 = new J8801();
        j8801.setCommand(shootingCommand);
        return (List<Long>) jt1078Template.shooting(phoneNumber, j8801, 300);
    }

    @Override
    public List<JTMediaDataInfo> queryMediaData(String phoneNumber, JTQueryMediaDataCommand queryMediaDataCommand) {
        J8802 j8802 = new J8802();
        j8802.setCommand(queryMediaDataCommand);
        return (List<JTMediaDataInfo>) jt1078Template.queryMediaData(phoneNumber, j8802, 300);
    }

    @Override
    public void uploadMediaData(String phoneNumber, JTQueryMediaDataCommand queryMediaDataCommand) {
        J8803 j8803 = new J8803();
        j8803.setCommand(queryMediaDataCommand);
        jt1078Template.uploadMediaData(phoneNumber, j8803, 10);
    }

    @Override
    public void record(String phoneNumber, int command, Integer time, Integer save, Integer samplingRate) {
        J8804 j8804 = new J8804();
        j8804.setCommond(command);
        j8804.setDuration(time);
        j8804.setSave(save);
        j8804.setSamplingRate(samplingRate);
        jt1078Template.record(phoneNumber, j8804, 10);
    }

    @Override
    public void uploadMediaDataForSingle(String phoneNumber, Long mediaId, Integer delete) {
        J8805 j8805 = new J8805();
        j8805.setMediaId(mediaId);
        j8805.setDelete(delete);
        jt1078Template.uploadMediaDataForSingle(phoneNumber, j8805, 10);
    }

    @Override
    public JTMediaAttribute queryMediaAttribute(String phoneNumber) {
        J9003 j9003 = new J9003();
        return (JTMediaAttribute) jt1078Template.queryMediaAttribute(phoneNumber, j9003, 300);
    }

    @Override
    public void changeStreamType(String phoneNumber, Integer channelId, Integer streamType) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo == null) {
            log.info("[JT-切换码流类型] 未找到点播信息 phoneNumber： {}， channelId： {}, streamType: {}", phoneNumber, channelId, streamType);
        }
        log.info("[JT-切换码流类型] phoneNumber： {}， channelId： {}, streamType: {}", phoneNumber, channelId, streamType);
        // 发送暂停命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(1);
        j9102.setCloseType(0);
        j9102.setStreamType(streamType);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
    }

    @Override
    public PageInfo<JTChannel> getChannelList(int page, int count, int deviceId, String query) {

        JTDevice device = getDeviceById(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备不存在");
        }
        PageHelper.startPage(page, count);
        List<JTChannel> all = jtChannelMapper.selectAll(deviceId, query);
        PageInfo<JTChannel> jtChannelPageInfo = new PageInfo<>(all);
        for (JTChannel jtChannel : jtChannelPageInfo.getList()) {
            String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + device.getPhoneNumber() + ":" + jtChannel.getChannelId();
            StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
            if (streamInfo != null) {
                jtChannel.setStream(streamInfo.getStream());
            }
        }
        return new PageInfo<>(all);
    }

    @Override
    @Transactional
    public void updateChannel(JTChannel channel) {
        channel.setUpdateTime(DateUtil.getNow());
        jtChannelMapper.update(channel);
        if (!ObjectUtils.isEmpty(channel.getGbDeviceId())) {
            if (channel.getGbId() > 0) {
                channelService.update(channel.buildCommonGBChannel());
            }else {
                channelService.add(channel.buildCommonGBChannel());
            }
        }
    }

    @Override
    @Transactional
    public void addChannel(JTChannel channel) {
        JTChannel channelInDb = jtChannelMapper.selectChannelByChannelId(channel.getTerminalDbId(), channel.getChannelId());
        if (channelInDb != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "通道已存在");
        }
        channel.setCreateTime(DateUtil.getNow());
        channel.setUpdateTime(DateUtil.getNow());
        jtChannelMapper.add(channel);
        if (!ObjectUtils.isEmpty(channel.getGbDeviceId())) {
            channelService.add(channel.buildCommonGBChannel());
        }
    }

    @Override
    @Transactional
    public void deleteChannelById(Integer id) {
        JTChannel jtChannel = jtChannelMapper.selectChannelById(id);
        if (jtChannel == null) {
            return;
        }
        if (jtChannel.getGbId() > 0) {
            channelService.delete(jtChannel.getGbId());
        }
        jtChannelMapper.delete(id);
    }

    @Override
    public JTDevice getDeviceById(Integer deviceId) {
        return jtDeviceMapper.getDeviceById(deviceId);
    }

    @Override
    public void updateDevicePosition(String phoneNumber, Double longitude, Double latitude) {
        JTDevice device = new JTDevice();
        device.setPhoneNumber(phoneNumber);
        device.setLongitude(longitude);
        device.setLatitude(latitude);
        device.setUpdateTime(DateUtil.getNow());
        String key = VideoManagerConstants.INVITE_INFO_1078_POSITION + userSetting.getServerId();
        redisTemplate.opsForList().leftPush(key, device);
    }

    @Scheduled(fixedDelay = 1000)
    public void positionTask(){
        String key = VideoManagerConstants.INVITE_INFO_1078_POSITION + userSetting.getServerId();
        int count = 1000;
        List<JTDevice> devices = new ArrayList<>(count);
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return;
        }
        long readCount = Math.min(count, size);
        for (long i = 0L; i < readCount; i++) {
            devices.add((JTDevice)redisTemplate.opsForList().rightPop(key));
        }
        jtDeviceMapper.batchUpdateDevicePosition(devices);
    }

    @Override
    public JTChannel getChannelByDbId(Integer id) {
        return jtChannelMapper.selectChannelById(id);
    }



    @Override
    public String getRecordTempUrl(String phoneNumber, Integer channelId, String startTime, String endTime, Integer alarmSign, Integer mediaType, Integer streamType, Integer storageType) {
        String filePath = UUID.randomUUID().toString();

        log.info("[JT-录像] 下载，设备:{}， 通道： {}， 开始时间： {}， 结束时间： {}，等待上传文件路径： {} ",
                phoneNumber, channelId, startTime, endTime, filePath);
        // 文件上传指令
        J9206 j9206 = new J9206();
        j9206.setChannelId(channelId);
        j9206.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime));
        j9206.setEndTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime));
        j9206.setServerIp(ftpSetting.getIp());
        j9206.setPort(ftpSetting.getPort());
        j9206.setUsername(ftpSetting.getUsername());
        j9206.setPassword(ftpSetting.getPassword());
        j9206.setPath(filePath);

        if (mediaType != null) {
            j9206.setMediaType(mediaType);
        }
        if (streamType != null) {
            j9206.setStreamType(streamType);
        }
        if (storageType != null) {
            j9206.setStorageType(storageType);
        }
        if (alarmSign != null) {
            j9206.setAlarmSign(alarmSign);
        }
        downloadManager.addCatch(filePath, phoneNumber, j9206);
        return filePath;
    }


    @Override
    public void recordDownload(String filePath, ServletOutputStream outputStream) {
        JTRecordDownloadCatch downloadCatch = downloadManager.getCatch(filePath);
        Assert.notNull(downloadCatch, "地址不存在");
        fileSystemFactory.addOutputStream(filePath, outputStream);
        jt1078Template.fileUpload(downloadCatch.getPhoneNumber(), downloadCatch.getJ9206(), 7200);
        downloadManager.runDownload(filePath, 2 * 60 * 60);
        fileSystemFactory.removeOutputStream(filePath);

    }


    @Override
    public void snap(String phoneNumber, int channelId, ServletOutputStream outputStream) {
        J8801 j8801 = new J8801();

        // 设置抓图默认参数
        JTShootingCommand shootingCommand = new JTShootingCommand();
        shootingCommand.setChanelId(channelId);
        shootingCommand.setCommand(1);
        shootingCommand.setTime(0);
        shootingCommand.setSave(0);
        shootingCommand.setResolvingPower(0xFF);
        shootingCommand.setQuality(1);
        shootingCommand.setBrightness(125);
        shootingCommand.setContrastRatio(60);
        shootingCommand.setSaturation(60);
        shootingCommand.setChroma(125);

        j8801.setCommand(shootingCommand);
        log.info("[JT-抓图] 设备编号： {}， 通道编号： {}", phoneNumber, channelId);
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) jt1078Template.shooting(phoneNumber, j8801, 300);
        log.info("[JT-抓图] 抓图编号： {}， 设备编号： {}， 通道编号： {}", ids.get(0), phoneNumber, channelId);

        log.info("[JT-抓图] 请求上传图片，抓图编号： {}， 设备编号： {}， 通道编号： {}", ids.get(0), phoneNumber, channelId);
        J8805 j8805 = new J8805();
        j8805.setMediaId(ids.get(0));
        j8805.setDelete(1);
        JTMediaEventInfo mediaEventInfo = (JTMediaEventInfo)jt1078Template.uploadMediaDataForSingle(phoneNumber, j8805, 600);
        if (mediaEventInfo == null) {
            log.info("[]");
            throw new ControllerException(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg());
        }
        log.info("[JT-抓图] 图片上传完成，抓图编号： {}， 设备编号： {}， 通道编号： {}", ids.get(0), phoneNumber, channelId);
        try {
            outputStream.write(mediaEventInfo.getMediaData());
            outputStream.flush();
        } catch (IOException e) {
            log.info("[JT-抓图] 数据写入异常，抓图编号： {}， 设备编号： {}， 通道编号： {}", ids.get(0), phoneNumber, channelId, e);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "数据写入异常");
        }
    }

    @Override
    public void uploadOneMedia(String phoneNumber, Long mediaId, ServletOutputStream outputStream) {
        log.info("[JT-单条存储多媒体数据上传] 媒体编号： {}， 设备编号： {}", mediaId, phoneNumber);
        J8805 j8805 = new J8805();
        j8805.setMediaId(mediaId);
        j8805.setDelete(1);
        log.info("[JT-单条存储多媒体数据上传] 请求上传图片，媒体编号： {}， 设备编号： {}", mediaId, phoneNumber);
        JTMediaEventInfo mediaEventInfo = (JTMediaEventInfo)jt1078Template.uploadMediaDataForSingle(phoneNumber, j8805, 300);
        if (mediaEventInfo == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg());
        }
        log.info("[JT-单条存储多媒体数据上传] 图片上传完成，媒体编号： {}， 设备编号： {}", mediaId, phoneNumber);
        try {
            outputStream.write(mediaEventInfo.getMediaData());
            outputStream.flush();
        } catch (IOException e) {
            log.info("[JT-单条存储多媒体数据上传] 数据写入异常，抓图编号： {}， 设备编号： {}", mediaId, phoneNumber, e);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "数据写入异常");
        }
    }
}
