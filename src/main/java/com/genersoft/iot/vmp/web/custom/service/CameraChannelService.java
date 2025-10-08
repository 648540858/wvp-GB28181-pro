package com.genersoft.iot.vmp.web.custom.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.FrontEndControlCodeForPTZ;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelControlService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.utils.Coordtransform;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.web.custom.bean.CameraChannel;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.xiaoymin.knife4j.core.util.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@Slf4j
@Service
public class CameraChannelService implements CommandLineRunner {

    @Autowired
    private CommonGBChannelMapper channelMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private IGbChannelPlayService channelPlayService;

    @Autowired
    private IGbChannelControlService channelControlService;

    @Autowired
    private UserSetting userSetting;

    @Override
    public void run(String... args) throws Exception {
        // 启动时获取全局token

    }

    public PageInfo<CameraChannel> queryList(Integer page, Integer count, String query, String sortName, String order, String groupAlias, Boolean status, Boolean containMobileDevice) {

        // 构建组织结构信息
        Group group = groupMapper.queryGroupByAlias(groupAlias);
        Assert.notNull(group, "获取组织结构失败");
        String groupDeviceId = group.getDeviceId();

        // 构建分页
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }

        List<CameraChannel> all = channelMapper.queryListForSy(query, sortName, order, groupDeviceId, status, containMobileDevice);
        PageInfo<CameraChannel> groupPageInfo = new PageInfo<>(all);
        List<CameraChannel> list = addIconPathForCameraChannelList(groupPageInfo.getList());
        groupPageInfo.setList(list);
        return groupPageInfo;
    }

    /**
     * 为通道增加图片信息
     */
    private List<CameraChannel> addIconPathForCameraChannelList(List<CameraChannel> channels) {
        return channels;
    }

    private CommonGBChannel queryChannelByDeviceIdAndDeviceCode(String deviceId, String deviceCode) {
        CommonGBChannel channel = null;
        if (deviceCode != null) {
//            Device device = deviceMapper.getDeviceByDeviceId(deviceId);
//            Assert.notNull(device, "设备不存在：" + deviceCode);
//            Integer deviceDbId = device.getId();
            channel = channelMapper.queryGbChannelByChannelDeviceIdAndGbDeviceId(deviceId, deviceCode);
        }else {
            channel = channelMapper.queryByDeviceId(deviceId);
        }
        return channel;
    }

    public CameraChannel queryOne(String deviceId, String deviceCode, String geoCoordSys) {
        CommonGBChannel channel = queryChannelByDeviceIdAndDeviceCode(deviceId, deviceCode);
        Assert.notNull(channel, "通道不存在");

        if (geoCoordSys != null && channel.getGbLongitude() != null && channel.getGbLatitude() != null
         && channel.getGbLongitude() > 0 && channel.getGbLatitude() > 0) {
            if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                Double[] position = Coordtransform.WGS84ToGCJ02(channel.getGbLongitude(), channel.getGbLatitude());
                channel.setGbLongitude(position[0]);
                channel.setGbLatitude(position[1]);
            }else if (geoCoordSys.equalsIgnoreCase("BD09")) {
                Double[] gcj02Position = Coordtransform.WGS84ToGCJ02(channel.getGbLongitude(), channel.getGbLatitude());
                Double[] position = Coordtransform.GCJ02ToBD09(gcj02Position[0], gcj02Position[1]);
                channel.setGbLongitude(position[0]);
                channel.setGbLatitude(position[1]);
            }
        }
        CameraChannel resultChannel = (CameraChannel)channel;
        if (deviceCode != null) {
            resultChannel.setDeviceCode(deviceCode);
        }
        return resultChannel;
    }

    /**
     * 播放通道
     * @param deviceId 通道编号
     * @param deviceCode 通道对应的国标设备的编号
     * @param callback 点播结果的回放
     */
    public void play(String deviceId, String deviceCode, ErrorCallback<StreamInfo> callback) {
        CommonGBChannel channel = queryChannelByDeviceIdAndDeviceCode(deviceId, deviceCode);
        Assert.notNull(channel, "通道不存在");
        channelPlayService.play(channel, null, userSetting.getRecordSip(), callback);
    }

    /**
     * 停止播放通道
     * @param deviceId 通道编号
     * @param deviceCode 通道对应的国标设备的编号
     */
    public void stopPlay(String deviceId, String deviceCode) {
        CommonGBChannel channel = queryChannelByDeviceIdAndDeviceCode(deviceId, deviceCode);
        Assert.notNull(channel, "通道不存在");
        channelPlayService.stopPlay(channel);
    }

    public void ptz(String deviceId, String deviceCode, String command, Integer speed, ErrorCallback<String> callback) {
        CommonGBChannel channel = queryChannelByDeviceIdAndDeviceCode(deviceId, deviceCode);
        Assert.notNull(channel, "通道不存在");

        if (speed == null) {
            speed = 50;
        }else if (speed < 0 || speed > 100) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "panSpeed 为 0-100的数字");
        }

        FrontEndControlCodeForPTZ controlCode = new FrontEndControlCodeForPTZ();
        controlCode.setPanSpeed(speed);
        controlCode.setTiltSpeed(speed);
        controlCode.setZoomSpeed(speed);
        switch (command){
            case "left":
                controlCode.setPan(0);
                break;
            case "right":
                controlCode.setPan(1);
                break;
            case "up":
                controlCode.setTilt(0);
                break;
            case "down":
                controlCode.setTilt(1);
                break;
            case "upleft":
                controlCode.setPan(0);
                controlCode.setTilt(0);
                break;
            case "upright":
                controlCode.setTilt(0);
                controlCode.setPan(1);
                break;
            case "downleft":
                controlCode.setPan(0);
                controlCode.setTilt(1);
                break;
            case "downright":
                controlCode.setTilt(1);
                controlCode.setPan(1);
                break;
            case "zoomin":
                controlCode.setZoom(1);
                break;
            case "zoomout":
                controlCode.setZoom(0);
                break;
            default:
                break;
        }

        channelControlService.ptz(channel, controlCode, callback);
    }

    public CameraChannel updateCamera(String deviceId, String deviceCode, String name, Double longitude, Double latitude, String geoCoordSys) {
    }
}
