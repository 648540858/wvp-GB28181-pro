package com.genersoft.iot.vmp.web.custom.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.FrontEndControlCodeForPTZ;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelControlService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.utils.Coordtransform;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.web.custom.bean.CameraChannel;
import com.genersoft.iot.vmp.web.custom.bean.CameraGroup;
import com.genersoft.iot.vmp.web.custom.bean.Point;
import com.genersoft.iot.vmp.web.custom.bean.PolygonQueryParam;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.xiaoymin.knife4j.core.util.Assert;
import com.google.common.base.CaseFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private IGroupService groupService;

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


    public PageInfo<CameraChannel> queryList(Integer page, Integer count, String groupAlias, Boolean status, String geoCoordSys) {
        // 构建组织结构信息
        Group group = groupMapper.queryGroupByAlias(groupAlias);
        Assert.notNull(group, "获取组织结构失败");
        String groupDeviceId = group.getDeviceId();

        // 构建分页
        PageHelper.startPage(page, count);

        List<CameraChannel> all = channelMapper.queryListForSy(groupDeviceId, status);
        PageInfo<CameraChannel> groupPageInfo = new PageInfo<>(all);
        List<CameraChannel> list = addIconPathAndPositionForCameraChannelList(groupPageInfo.getList(), geoCoordSys);
        groupPageInfo.setList(list);
        return groupPageInfo;
    }

    public PageInfo<CameraChannel> queryListWithChild(Integer page, Integer count, String query, String sortName, Boolean order, String groupAlias, Boolean status, String geoCoordSys) {
        // 构建组织结构信息
        CameraGroup group = groupMapper.queryGroupByAlias(groupAlias);
        Assert.notNull(group, "获取组织结构失败");
        String groupDeviceId = group.getDeviceId();
        // 获取所有子节点
        List<CameraGroup> groupList = queryAllGroupChildren(group.getId(), group.getBusinessGroup());
        groupList.add(group);
        // 构建分页
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        if (order == null) {
            order = true;
        }
        List<CameraChannel> all = channelMapper.queryListWithChildForSy(query, sortName, order, groupList, status);
        PageInfo<CameraChannel> groupPageInfo = new PageInfo<>(all);
        List<CameraChannel> list = addIconPathAndPositionForCameraChannelList(groupPageInfo.getList(), geoCoordSys);
        groupPageInfo.setList(list);
        return groupPageInfo;
    }

    // 获取所有子节点
    private List<CameraGroup> queryAllGroupChildren(int groupId, String businessGroup) {
        Map<Integer, CameraGroup> groupMap = groupMapper.queryByBusinessGroupForMap(businessGroup);
        for (CameraGroup cameraGroup : groupMap.values()) {
            cameraGroup.setParent(groupMap.get(cameraGroup.getParentId()));
        }
        CameraGroup cameraGroup = groupMap.get(groupId);
        if (cameraGroup == null) {
            return Collections.emptyList();
        }else {
            return cameraGroup.getChild();
        }
    }

    /**
     * 为通道增加图片信息和转换坐标系
     */
    private List<CameraChannel> addIconPathAndPositionForCameraChannelList(List<CameraChannel> channels, String geoCoordSys) {
        // 读取redis 图标信息
        JSONArray jsonArray = (JSONArray) redisTemplate.opsForValue().get("machineInfo");
        Map<String, String> pathMap = new HashMap<>();
        if (jsonArray != null && !jsonArray.isEmpty()) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String machineType = jsonObject.getString("machineType");
                String imagesPath = jsonObject.getString("imagesPath");
                if (machineType != null && imagesPath != null) {
                    pathMap.put(machineType, imagesPath);
                }
            }
        }else {
            log.warn("[读取通道图标信息失败]");
        }
        for (CameraChannel channel : channels) {
            if (channel.getGbModel() != null && pathMap.get(channel.getGbModel()) != null) {
                channel.setIcon(pathMap.get(channel.getGbModel()));
            }
            // 坐标系转换
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
        }
        return channels;
    }

    public CameraChannel queryOne(String deviceId, String deviceCode, String geoCoordSys) {
        CameraChannel channel = channelMapper.queryGbChannelByChannelDeviceIdAndGbDeviceId(deviceId, deviceCode);
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
        if (deviceCode != null) {
            channel.setDeviceCode(deviceCode);
        }
        return channel;
    }

    /**
     * 播放通道
     * @param deviceId 通道编号
     * @param deviceCode 通道对应的国标设备的编号
     * @param callback 点播结果的回放
     */
    public void play(String deviceId, String deviceCode, ErrorCallback<StreamInfo> callback) {
        CommonGBChannel channel = channelMapper.queryGbChannelByChannelDeviceIdAndGbDeviceId(deviceId, deviceCode);
        Assert.notNull(channel, "通道不存在");
        channelPlayService.play(channel, null, userSetting.getRecordSip(), callback);
    }

    /**
     * 停止播放通道
     * @param deviceId 通道编号
     * @param deviceCode 通道对应的国标设备的编号
     */
    public void stopPlay(String deviceId, String deviceCode) {
        CommonGBChannel channel = channelMapper.queryGbChannelByChannelDeviceIdAndGbDeviceId(deviceId, deviceCode);
        Assert.notNull(channel, "通道不存在");
        channelPlayService.stopPlay(channel);
    }

    public void ptz(String deviceId, String deviceCode, String command, Integer speed, ErrorCallback<String> callback) {
        CommonGBChannel channel = channelMapper.queryGbChannelByChannelDeviceIdAndGbDeviceId(deviceId, deviceCode);
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

    public void updateCamera(String deviceId, String deviceCode, String name, Double longitude, Double latitude, String geoCoordSys) {
        CommonGBChannel commonGBChannel = channelMapper.queryGbChannelByChannelDeviceIdAndGbDeviceId(deviceId, deviceCode);
        Assert.notNull(commonGBChannel, "通道不存在");
        commonGBChannel.setGbName(name);
        if (geoCoordSys != null && longitude != null && latitude != null
                && longitude > 0 && latitude > 0) {
            if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                Double[] position = Coordtransform.WGS84ToGCJ02(longitude, latitude);
                commonGBChannel.setGbLongitude(position[0]);
                commonGBChannel.setGbLatitude(position[1]);
            }else if (geoCoordSys.equalsIgnoreCase("BD09")) {
                Double[] gcj02Position = Coordtransform.WGS84ToGCJ02(longitude, latitude);
                Double[] position = Coordtransform.GCJ02ToBD09(gcj02Position[0], gcj02Position[1]);
                commonGBChannel.setGbLongitude(position[0]);
                commonGBChannel.setGbLatitude(position[1]);
            }else {
                commonGBChannel.setGbLongitude(longitude);
                commonGBChannel.setGbLatitude(latitude);
            }
        }else {
            commonGBChannel.setGbLongitude(longitude);
            commonGBChannel.setGbLatitude(latitude);
        }
        channelMapper.update(commonGBChannel);
    }

    public List<CameraChannel> queryListByDeviceIds(List<String> deviceIds, String geoCoordSys) {
        List<CameraChannel> cameraChannels = channelMapper.queryListByDeviceIds(deviceIds);
        return addIconPathAndPositionForCameraChannelList(cameraChannels, geoCoordSys);
    }

    public List<CameraChannel> queryListByAddressAndDirectionType(String address, Integer directionType, String geoCoordSys) {
        List<CameraChannel> cameraChannels = channelMapper.queryListByAddressAndDirectionType(address, directionType);
        return addIconPathAndPositionForCameraChannelList(cameraChannels, geoCoordSys);
    }


    public List<CameraChannel> queryListInBox(Double minLongitude, Double maxLongitude, Double minLatitude, Double maxLatitude, Integer level, String groupAlias, String geoCoordSys) {
        // 构建组织结构信息
        CameraGroup group = groupMapper.queryGroupByAlias(groupAlias);
        Assert.notNull(group, "获取组织结构失败");
        // 获取所有子节点
        List<CameraGroup> groupList = queryAllGroupChildren(group.getId(), group.getBusinessGroup());
        groupList.add(group);
        // 参数坐标系列转换
        if (geoCoordSys != null) {
            if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                Double[] minPosition = Coordtransform.WGS84ToGCJ02(minLongitude, minLatitude);
                minLongitude = minPosition[0];
                minLatitude = minPosition[1];

                Double[] maxPosition = Coordtransform.WGS84ToGCJ02(maxLongitude, maxLatitude);
                maxLongitude = maxPosition[0];
                maxLatitude = maxPosition[1];
            }else if (geoCoordSys.equalsIgnoreCase("BD09")) {
                Double[] gcj02MinPosition = Coordtransform.WGS84ToGCJ02(minLongitude, minLatitude);
                Double[] minPosition = Coordtransform.GCJ02ToBD09(gcj02MinPosition[0], gcj02MinPosition[1]);
                minLongitude = minPosition[0];
                minLatitude = minPosition[1];

                Double[] gcj02MaxPosition = Coordtransform.WGS84ToGCJ02(maxLongitude, maxLatitude);
                Double[] maxPosition = Coordtransform.GCJ02ToBD09(gcj02MaxPosition[0], gcj02MaxPosition[1]);
                maxLongitude = maxPosition[0];
                maxLatitude = maxPosition[1];
            }
        }

        List<CameraChannel> all = channelMapper.queryListInBox(minLongitude, maxLongitude, minLatitude, maxLatitude, level, groupList);
        return addIconPathAndPositionForCameraChannelList(all, geoCoordSys);
    }

    public List<CameraChannel> queryListInCircle(Double centerLongitude, Double centerLatitude, Double radius, Integer level, String groupAlias, String geoCoordSys) {
        // 构建组织结构信息
        CameraGroup group = groupMapper.queryGroupByAlias(groupAlias);
        Assert.notNull(group, "获取组织结构失败");
        // 获取所有子节点
        List<CameraGroup> groupList = queryAllGroupChildren(group.getId(), group.getBusinessGroup());
        groupList.add(group);

        // 参数坐标系列转换
        if (geoCoordSys != null) {
            if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                Double[] position = Coordtransform.WGS84ToGCJ02(centerLongitude, centerLatitude);
                centerLongitude = position[0];
                centerLatitude = position[1];

            }else if (geoCoordSys.equalsIgnoreCase("BD09")) {
                Double[] gcj02Position = Coordtransform.WGS84ToGCJ02(centerLongitude, centerLatitude);
                Double[] position = Coordtransform.GCJ02ToBD09(gcj02Position[0], gcj02Position[1]);
                centerLongitude = position[0];
                centerLatitude = position[1];
            }
        }

        List<CameraChannel> all = channelMapper.queryListInCircle(centerLongitude, centerLatitude, radius, level, groupList);
        return addIconPathAndPositionForCameraChannelList(all, geoCoordSys);
    }

    public List<CameraChannel> queryListInPolygon(List<Point> pointList, String groupAlias, Integer level, String geoCoordSys) {
        // 构建组织结构信息
        CameraGroup group = groupMapper.queryGroupByAlias(groupAlias);
        Assert.notNull(group, "获取组织结构失败");
        // 获取所有子节点
        List<CameraGroup> groupList = queryAllGroupChildren(group.getId(), group.getBusinessGroup());
        groupList.add(group);

        // 参数坐标系列转换
        if (geoCoordSys != null) {
            for (Point point : pointList) {
                if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                    Double[] position = Coordtransform.WGS84ToGCJ02(point.getLng(), point.getLat());
                    point.setLng(position[0]);
                    point.setLat(position[1]);
                }else if (geoCoordSys.equalsIgnoreCase("BD09")) {
                    Double[] gcj02Position = Coordtransform.WGS84ToGCJ02(point.getLng(), point.getLat());
                    Double[] position = Coordtransform.GCJ02ToBD09(gcj02Position[0], gcj02Position[1]);
                    point.setLng(position[0]);
                    point.setLat(position[1]);
                }
            }
        }

        List<CameraChannel> all = channelMapper.queryListInPolygon(pointList, level, groupList);
        return addIconPathAndPositionForCameraChannelList(all, geoCoordSys);
    }
}
