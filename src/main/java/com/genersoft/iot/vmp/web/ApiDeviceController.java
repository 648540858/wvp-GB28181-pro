package com.genersoft.iot.vmp.web;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.DeviceOffLineDetector;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 兼容LiveGBS的API：设备信息
 */
@CrossOrigin
@RestController
@RequestMapping(value = "/api/v1/device")
public class ApiDeviceController {

    private final static Logger logger = LoggerFactory.getLogger(ApiDeviceController.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private DeviceOffLineDetector offLineDetector;

    /**
     * 分页获取设备列表 TODO 现在直接返回，尚未实现分页
     * @param start
     * @param limit
     * @param q
     * @param online
     * @return
     */
    @RequestMapping(value = "/list")
    public JSONObject list( @RequestParam(required = false)Integer start,
                            @RequestParam(required = false)Integer limit,
                            @RequestParam(required = false)String q,
                            @RequestParam(required = false)Boolean online ){

        if (logger.isDebugEnabled()) {
            logger.debug("查询所有视频设备API调用");
        }

        logger.debug("查询所有视频设备API调用");
        JSONObject result = new JSONObject();
        List<Device> devices;
        if (start == null || limit ==null) {
            devices = storager.queryVideoDeviceList();
            result.put("DeviceCount", devices.size());
        }else {
            PageInfo<Device> deviceList = storager.queryVideoDeviceList(start/limit, limit);
            result.put("DeviceCount", deviceList.getTotal());
            devices = deviceList.getList();
        }

        JSONArray deviceJSONList = new JSONArray();
        for (Device device : devices) {
            JSONObject deviceJsonObject = new JSONObject();
            deviceJsonObject.put("ID", device.getDeviceId());
            deviceJsonObject.put("Name", device.getName());
            deviceJsonObject.put("Type", "GB");
            deviceJsonObject.put("ChannelCount", device.getChannelCount());
            deviceJsonObject.put("RecvStreamIP", "");
            deviceJsonObject.put("CatalogInterval", 3600); // 通道目录抓取周期
            deviceJsonObject.put("SubscribeInterval", 0); // 订阅周期(秒), 0 表示后台不周期订阅
            deviceJsonObject.put("Online", device.getOnline() == 1);
            deviceJsonObject.put("Password", "");
            deviceJsonObject.put("MediaTransport", device.getTransport());
            deviceJsonObject.put("RemoteIP", device.getIp());
            deviceJsonObject.put("RemotePort", device.getPort());
            deviceJsonObject.put("LastRegisterAt", "");
            deviceJsonObject.put("LastKeepaliveAt", "");
            deviceJsonObject.put("UpdatedAt", "");
            deviceJsonObject.put("CreatedAt", "");
            deviceJSONList.add(deviceJsonObject);
        }
        result.put("DeviceList",deviceJSONList);
        return result;
    }

    @RequestMapping(value = "/channellist")
    public JSONObject channellist( String serial,
                                   @RequestParam(required = false)String channel_type,
                                   @RequestParam(required = false)String dir_serial ,
                                   @RequestParam(required = false)Integer start,
                                   @RequestParam(required = false)Integer limit,
                                   @RequestParam(required = false)String q,
                                   @RequestParam(required = false)Boolean online ){

        if (logger.isDebugEnabled()) {
            logger.debug("查询所有视频设备API调用");
        }
        JSONObject result = new JSONObject();
        // 查询设备是否存在
        Device device = storager.queryVideoDevice(serial);
        if (device == null) {
            result.put("ChannelCount", 0);
            result.put("ChannelList", "[]");
            return result;
        }
        List<DeviceChannel> deviceChannels;
        if (start == null || limit ==null) {
            deviceChannels = storager.queryChannelsByDeviceId(serial);
            result.put("ChannelCount", deviceChannels.size());
        }else {
            PageInfo<DeviceChannel> pageResult = storager.queryChannelsByDeviceId(serial, null, null, null,start/limit, limit);
            result.put("ChannelCount", pageResult.getTotal());
            deviceChannels = pageResult.getList();
        }

        JSONArray channleJSONList = new JSONArray();
        for (DeviceChannel deviceChannel : deviceChannels) {
            JSONObject deviceJOSNChannel = new JSONObject();
            deviceJOSNChannel.put("ID", deviceChannel.getChannelId());
            deviceJOSNChannel.put("DeviceID", device.getDeviceId());
            deviceJOSNChannel.put("DeviceName", device.getName());
            deviceJOSNChannel.put("DeviceOnline", device.getOnline() == 1);
            deviceJOSNChannel.put("Channel", 0); // TODO 自定义序号
            deviceJOSNChannel.put("Name", deviceChannel.getName());
            deviceJOSNChannel.put("Custom", false);
            deviceJOSNChannel.put("CustomName", "");
            deviceJOSNChannel.put("SubCount", deviceChannel.getSubCount()); // TODO ? 子节点数, SubCount > 0 表示该通道为子目录
            deviceJOSNChannel.put("SnapURL", "");
            deviceJOSNChannel.put("Manufacturer ", deviceChannel.getManufacture());
            deviceJOSNChannel.put("Model", deviceChannel.getModel());
            deviceJOSNChannel.put("Owner", deviceChannel.getOwner());
            deviceJOSNChannel.put("CivilCode", deviceChannel.getCivilCode());
            deviceJOSNChannel.put("Address", deviceChannel.getAddress());
            deviceJOSNChannel.put("Parental", deviceChannel.getParental()); // 当为通道设备时, 是否有通道子设备, 1-有,0-没有
            deviceJOSNChannel.put("ParentID", deviceChannel.getParentId()); // 直接上级编号
            deviceJOSNChannel.put("Secrecy", deviceChannel.getSecrecy());
            deviceJOSNChannel.put("RegisterWay", 1); // 注册方式, 缺省为1, 允许值: 1, 2, 3
                                                     // 1-IETF RFC3261,
                                                     // 2-基于口令的双向认证,
                                                     // 3-基于数字证书的双向认证
            deviceJOSNChannel.put("Status", deviceChannel.getStatus());
            deviceJOSNChannel.put("Longitude", deviceChannel.getLongitude());
            deviceJOSNChannel.put("Latitude", deviceChannel.getLatitude());
            deviceJOSNChannel.put("PTZType ", deviceChannel.getPTZType()); // 云台类型, 0 - 未知, 1 - 球机, 2 - 半球,
                                                                            //   3 - 固定枪机, 4 - 遥控枪机
            deviceJOSNChannel.put("CustomPTZType", "");
            deviceJOSNChannel.put("StreamID", deviceChannel.getStreamId()); // StreamID 直播流ID, 有值表示正在直播
            deviceJOSNChannel.put("NumOutputs ", -1); // 直播在线人数
            channleJSONList.add(deviceJOSNChannel);
        }
        result.put("ChannelList", channleJSONList);
        return result;
    }
}
