package com.genersoft.iot.vmp.web.gb28181;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Preset;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.web.gb28181.dto.DeviceChannelExtend;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.*;

/**
 * API兼容：设备信息
 */
@SuppressWarnings("unchecked")
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/device")
public class ApiDeviceController {

    @Autowired
    private SIPCommander cmder;
    @Autowired
    private IDeviceChannelService channelService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private IDeviceService deviceService;


    /**
     * 分页获取设备列表 现在直接返回，尚未实现分页
     * @param start
     * @param limit
     * @param q
     * @param online
     * @return
     */
    @GetMapping(value = "/list")
    public JSONObject list( @RequestParam(required = false)Integer start,
                            @RequestParam(required = false)Integer limit,
                            @RequestParam(required = false)String q,
                            @RequestParam(required = false)Boolean online ){

//        if (logger.isDebugEnabled()) {
//            logger.debug("查询所有视频设备API调用");
//        }

        JSONObject result = new JSONObject();
        List<Device> devices;
        if (start == null || limit ==null) {
            devices = deviceService.getAllByStatus(online);
            result.put("DeviceCount", devices.size());
        }else {
            PageInfo<Device> deviceList = deviceService.getAll(start/limit, limit,null, online);
            result.put("DeviceCount", deviceList.getTotal());
            devices = deviceList.getList();
        }

        JSONArray deviceJSONList = new JSONArray();
        devices.stream().forEach(device -> {
            JSONObject deviceJsonObject = new JSONObject();
            deviceJsonObject.put("ID", device.getDeviceId());
            deviceJsonObject.put("Name", device.getName());
            deviceJsonObject.put("Type", "GB");
            deviceJsonObject.put("ChannelCount", device.getChannelCount());
            deviceJsonObject.put("RecvStreamIP", "");
            deviceJsonObject.put("CatalogInterval", 3600); // 通道目录抓取周期
            deviceJsonObject.put("SubscribeInterval", device.getSubscribeCycleForCatalog()); // 订阅周期(秒), 0 表示后台不周期订阅
            deviceJsonObject.put("Online", device.isOnLine());
            deviceJsonObject.put("Password", "");
            deviceJsonObject.put("MediaTransport", device.getTransport());
            deviceJsonObject.put("RemoteIP", device.getIp());
            deviceJsonObject.put("RemotePort", device.getPort());
            deviceJsonObject.put("LastRegisterAt", "");
            deviceJsonObject.put("LastKeepaliveAt", "");
            deviceJsonObject.put("UpdatedAt", "");
            deviceJsonObject.put("CreatedAt", "");
            deviceJSONList.add(deviceJsonObject);
        });
        result.put("DeviceList",deviceJSONList);
        return result;
    }

    @GetMapping(value = "/channellist")
    public JSONObject channellist( String serial,
                                   @RequestParam(required = false)String channel_type,
                                   @RequestParam(required = false)String code ,
                                   @RequestParam(required = false)String dir_serial ,
                                   @RequestParam(required = false)Integer start,
                                   @RequestParam(required = false)Integer limit,
                                   @RequestParam(required = false)String q,
                                   @RequestParam(required = false)Boolean online ){

        JSONObject result = new JSONObject();
        List<DeviceChannelExtend> deviceChannels;
        List<String> channelIds = null;
        if (!ObjectUtils.isEmpty(code)) {
            String[] split = code.trim().split(",");
            channelIds = Arrays.asList(split);
        }
        List<DeviceChannelExtend> allDeviceChannelList = channelService.queryChannelExtendsByDeviceId(serial,channelIds,online);
        if (start == null || limit ==null) {
            deviceChannels = allDeviceChannelList;
            result.put("ChannelCount", deviceChannels.size());
        }else {
            if (start > allDeviceChannelList.size()) {
                deviceChannels = new ArrayList<>();
            }else {
                if (start + limit < allDeviceChannelList.size()) {
                    deviceChannels = allDeviceChannelList.subList(start, start + limit);
                }else {
                    deviceChannels = allDeviceChannelList.subList(start, allDeviceChannelList.size());
                }
            }
            result.put("ChannelCount", allDeviceChannelList.size());
        }
        JSONArray channleJSONList = new JSONArray();
        deviceChannels.stream().forEach(deviceChannelExtend -> {
            JSONObject deviceJOSNChannel = new JSONObject();
            deviceJOSNChannel.put("ID", deviceChannelExtend.getChannelId());
            deviceJOSNChannel.put("DeviceID", deviceChannelExtend.getDeviceId());
            deviceJOSNChannel.put("DeviceName", deviceChannelExtend.getDeviceName());
            deviceJOSNChannel.put("DeviceOnline", deviceChannelExtend.isDeviceOnline());
            deviceJOSNChannel.put("Channel", 0); // TODO 自定义序号
            deviceJOSNChannel.put("Name", deviceChannelExtend.getName());
            deviceJOSNChannel.put("Custom", false);
            deviceJOSNChannel.put("CustomName", "");
            deviceJOSNChannel.put("SubCount", deviceChannelExtend.getSubCount()); // TODO ? 子节点数, SubCount > 0 表示该通道为子目录
            deviceJOSNChannel.put("SnapURL", "");
            deviceJOSNChannel.put("Manufacturer ", deviceChannelExtend.getManufacture());
            deviceJOSNChannel.put("Model", deviceChannelExtend.getModel());
            deviceJOSNChannel.put("Owner", deviceChannelExtend.getOwner());
            deviceJOSNChannel.put("CivilCode", deviceChannelExtend.getCivilCode());
            deviceJOSNChannel.put("Address", deviceChannelExtend.getAddress());
            deviceJOSNChannel.put("Parental", deviceChannelExtend.getParental()); // 当为通道设备时, 是否有通道子设备, 1-有,0-没有
            deviceJOSNChannel.put("ParentID", deviceChannelExtend.getParentId()); // 直接上级编号
            deviceJOSNChannel.put("Secrecy", deviceChannelExtend.getSecrecy());
            deviceJOSNChannel.put("RegisterWay", 1); // 注册方式, 缺省为1, 允许值: 1, 2, 3
            // 1-IETF RFC3261,
            // 2-基于口令的双向认证,
            // 3-基于数字证书的双向认证
            deviceJOSNChannel.put("Status", deviceChannelExtend.getStatus());
            deviceJOSNChannel.put("Longitude", deviceChannelExtend.getLongitude());
            deviceJOSNChannel.put("Latitude", deviceChannelExtend.getLatitude());
            deviceJOSNChannel.put("PTZType ", deviceChannelExtend.getPTZType()); // 云台类型, 0 - 未知, 1 - 球机, 2 - 半球,
            //   3 - 固定枪机, 4 - 遥控枪机
            deviceJOSNChannel.put("CustomPTZType", "");
            deviceJOSNChannel.put("StreamID", deviceChannelExtend.getStreamId()); // StreamID 直播流ID, 有值表示正在直播
            deviceJOSNChannel.put("NumOutputs ", -1); // 直播在线人数
            channleJSONList.add(deviceJOSNChannel);
        });
        result.put("ChannelList", channleJSONList);
        return result;
    }

    /**
     * 设备信息 - 获取下级通道预置位
     * @param serial 设备编号
     * @param code 通道编号,通过 /api/v1/device/channellist 获取的 ChannelList.ID, 该参数和 channel 二选一传递即可
     * @param channel 通道序号, 默认值: 1
     * @param fill 是否填充空置预置位，当下级返回预置位，但不够255个时，自动填充空置预置位到255个， 默认值: true， 允许值: true, false
     * @param timeout 超时时间(秒) 默认值: 15
     * @return
     */
    @GetMapping(value = "/fetchpreset")
    private DeferredResult<WVPResult<Object>>  list(String serial,
                      @RequestParam(required = false)Integer channel,
                      @RequestParam(required = false)String code,
                      @RequestParam(required = false)Boolean fill,
                      @RequestParam(required = false)Integer timeout){

        if (log.isDebugEnabled()) {
            log.debug("<模拟接口> 获取下级通道预置位 API调用，deviceId：{} ，channel：{} ，code：{} ，fill：{} ，timeout：{} ",
                    serial, channel, code, fill, timeout);
        }

        Device device = deviceService.getDeviceByDeviceId(serial);
        Assert.notNull(device, "设备不存在");
        DeferredResult<WVPResult<Object>> deferredResult = new DeferredResult<> (timeout * 1000L);
        deviceService.queryPreset(device, code, (resultCode, msg, data) -> {
            if (resultCode == ErrorCode.SUCCESS.getCode()) {
                List<Preset> presetQuerySipReqList = (List<Preset>)data;
                HashMap<String, Object> resultMap = new HashMap<>();
                resultMap.put("DeviceID", code);
                resultMap.put("Result", "OK");
                resultMap.put("SumNum", presetQuerySipReqList.size());
                ArrayList<Map<String, Object>> presetItemList = new ArrayList<>(presetQuerySipReqList.size());
                for (Preset presetQuerySipReq : presetQuerySipReqList) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("PresetID", presetQuerySipReq.getPresetId());
                    item.put("PresetName", presetQuerySipReq.getPresetName());
                    item.put("PresetEnable", true);
                    presetItemList.add(item);
                }
                resultMap.put("PresetItemList",presetItemList );
                deferredResult.setResult(new WVPResult<>(resultCode, msg, resultMap));
            }else {
                deferredResult.setResult(new WVPResult<>(resultCode, msg, null));
            }
        });

        deferredResult.onTimeout(()->{
            log.warn("[获取设备预置位] 超时, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "wait for presetquery timeout["+timeout+"s]"));
        });
        return deferredResult;
    }
}
