package com.genersoft.iot.vmp.web.custom;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.FrontEndControlCodeForPTZ;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.web.custom.bean.CameraChannel;
import com.genersoft.iot.vmp.web.custom.bean.CameraStreamContent;
import com.genersoft.iot.vmp.web.custom.bean.IdsQueryParam;
import com.genersoft.iot.vmp.web.custom.bean.PolygonQueryParam;
import com.genersoft.iot.vmp.web.custom.service.CameraChannelService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Tag(name  = "第三方接口")
@Slf4j
@RestController
@RequestMapping(value = "/api/sy")
public class CameraChannelController {

    @Autowired
    private CameraChannelService channelService;

    @Autowired
    private UserSetting userSetting;


    @GetMapping(value = "/camera/list/group")
    @ResponseBody
    @Operation(summary = "查询摄像机列表, 只查询当前虚拟组织下的", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "sortName", description = "排序字段名")
    @Parameter(name = "order", description = "排序方式（升序 asc 或降序 desc ）")
    @Parameter(name = "groupAlias", description = "分组别名")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    @Parameter(name = "status", description = "摄像头状态")
    @Parameter(name = "containMobileDevice", description = "是否包含移动设备")
    public PageInfo<CameraChannel> queryListInCurrentGroup(@RequestParam(required = false, value = "page", defaultValue = "1" )Integer page,
                                        @RequestParam(required = false, value = "page", defaultValue = "100")Integer count,
                                        @RequestParam(required = false) String query,
                                        @RequestParam(required = false) String sortName,
                                        @RequestParam(required = false) String order,
                                        @RequestParam(required = true) String groupAlias,
                                        @RequestParam(required = false) Boolean status,
                                        @RequestParam(required = false) Boolean containMobileDevice){
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        if (ObjectUtils.isEmpty(sortName)) {
            sortName = null;
        }
        if (ObjectUtils.isEmpty(order)) {
            order = null;
        }
        if (ObjectUtils.isEmpty(groupAlias)) {
            groupAlias = null;
        }

        return channelService.queryList(page, count, query, sortName, order, groupAlias, status, containMobileDevice);
    }

    @GetMapping(value = "/camera/list")
    @ResponseBody
    @Operation(summary = "查询摄像机列表, 查询当前虚拟组织下以及全部子节点", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "sortName", description = "排序字段名")
    @Parameter(name = "order", description = "排序方式（升序 asc 或降序 desc ）")
    @Parameter(name = "groupAlias", description = "分组别名")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    @Parameter(name = "status", description = "摄像头状态")
    @Parameter(name = "containMobileDevice", description = "是否包含移动设备")
    public PageInfo<CameraChannel> list(@RequestParam(required = false, value = "page", defaultValue = "1" )Integer page,
                                        @RequestParam(required = false, value = "page", defaultValue = "100")Integer count,
                                        @RequestParam(required = false) String query,
                                        @RequestParam(required = false) String sortName,
                                        @RequestParam(required = false) String order,
                                        @RequestParam(required = false) String groupAlias,
                                        @RequestParam(required = false) Boolean status,
                                        @RequestParam(required = false) Boolean containMobileDevice){
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        if (ObjectUtils.isEmpty(sortName)) {
            sortName = null;
        }
        if (ObjectUtils.isEmpty(order)) {
            order = null;
        }
        if (ObjectUtils.isEmpty(groupAlias)) {
            groupAlias = null;
        }

        return channelService.queryList(page, count, query, sortName, order, groupAlias, status, containMobileDevice);
    }

    @GetMapping(value = "/camera/one")
    @ResponseBody
    @Operation(summary = "查询单个摄像头信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "通道编号")
    @Parameter(name = "deviceCode", description = "摄像头设备国标编号, 对于非国标摄像头可以不设置此参数")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    public CameraChannel getOne(@RequestParam(required = true) String deviceId, @RequestParam(required = true) String deviceCode,
                                  @RequestParam(required = false) String geoCoordSys) {
        return channelService.queryOne(deviceId, deviceCode, geoCoordSys);
    }

    @GetMapping(value = "/camera/update")
    @ResponseBody
    @Operation(summary = "更新摄像头信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "通道编号")
    @Parameter(name = "deviceCode", description = "摄像头设备国标编号, 对于非国标摄像头可以不设置此参数")
    @Parameter(name = "name", description = "通道名称")
    @Parameter(name = "longitude", description = "经度")
    @Parameter(name = "latitude", description = "纬度")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    public CameraChannel updateCamera(String deviceId,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) Double longitude,
                                      @RequestParam(required = false) Double latitude) {
        return null;
    }

    @PostMapping(value = "/camera/list/ids")
    @ResponseBody
    @Operation(summary = "根据编号查询多个摄像头信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<CameraChannel> queryListByNos(@RequestBody IdsQueryParam param) {
        return null;
    }

    @GetMapping(value = "/camera/list/box")
    @ResponseBody
    @Operation(summary = "根据矩形查询摄像头", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "minLongitude", description = "最小经度")
    @Parameter(name = "maxLongitude", description = "最大经度")
    @Parameter(name = "minLatitude", description = "最小纬度")
    @Parameter(name = "maxLatitude", description = "最大纬度")
    @Parameter(name = "level", description = "地图级别")
    @Parameter(name = "groupAlias", description = "分组别名")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    public List<CameraChannel> queryListInBox(Double minLongitude, Double maxLongitude,
                                              Double minLatitude, Double maxLatitude,
                                              @RequestParam(required = false) Integer level,
                                              String groupAlias,
                                              @RequestParam(required = false) String geoCoordSys) {
        return null;
    }

    @GetMapping(value = "/camera/list/polygon")
    @ResponseBody
    @Operation(summary = "根据多边形查询摄像头", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "position", description = "多边形位置，格式： [{'lng':116.32, 'lat': 39: 39.2}, {'lng':115.32, 'lat': 39: 38.2}, {'lng':125.32, 'lat': 39: 38.2}]")
    @Parameter(name = "level", description = "地图级别")
    @Parameter(name = "groupAlias", description = "分组别名")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    public List<CameraChannel> queryListInPolygon(@RequestBody PolygonQueryParam param) {
        return null;
    }

    @GetMapping(value = "/camera/list/circle")
    @ResponseBody
    @Operation(summary = "根据圆范围查询摄像头", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "centerLongitude", description = "圆心经度")
    @Parameter(name = "centerLatitude", description = "圆心纬度")
    @Parameter(name = "radius", description = "查询范围的半径，单位米")
    @Parameter(name = "groupAlias", description = "分组别名")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    public List<CameraChannel> queryListInCircle(Double centerLongitude, Double centerLatitude, Double radius, String groupAlias,
                                                 String geoCoordSys) {
        return null;
    }

    @GetMapping(value = "/camera/list/address")
    @ResponseBody
    @Operation(summary = "根据安装地址和监视方位获取摄像头", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "address", description = "安装地址")
    @Parameter(name = "directionType", description = "监视方位")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    public List<CameraChannel> queryListByAddressAndDirectionType(String address, Integer directionType, String geoCoordSys) {
        return null;
    }

    @GetMapping(value = "/camera/control/play")
    @ResponseBody
    @Operation(summary = "播放摄像头", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "通道编号")
    @Parameter(name = "deviceCode", description = "摄像头设备国标编号, 对于非国标摄像头可以不设置此参数")
    public DeferredResult<WVPResult<CameraStreamContent>> play(HttpServletRequest request, String deviceId, @RequestParam(required = false) String deviceCode) {

        log.info("[SY-播放摄像头] API调用，deviceId：{} ，deviceCode：{} ",deviceId, deviceCode);
        DeferredResult<WVPResult<CameraStreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

        ErrorCallback<StreamInfo> callback = (code, msg, streamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                WVPResult<CameraStreamContent> wvpResult = WVPResult.success();
                if (streamInfo != null) {
                    if (userSetting.getUseSourceIpAsStreamIp()) {
                        streamInfo=streamInfo.clone();//深拷贝
                        String host;
                        try {
                            URL url=new URL(request.getRequestURL().toString());
                            host=url.getHost();
                        } catch (MalformedURLException e) {
                            host=request.getLocalAddr();
                        }
                        streamInfo.changeStreamIp(host);
                    }
                    if (!ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix())
                            && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }
                    wvpResult.setData(new CameraStreamContent(streamInfo));
                }else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }
                result.setResult(wvpResult);
            }else {
                result.setResult(WVPResult.fail(code, msg));
            }
        };
        channelService.play(deviceId, deviceCode, callback);
        return result;
    }

    @GetMapping(value = "/camera/control/stop")
    @ResponseBody
    @Operation(summary = "停止播放摄像头", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "通道编号")
    @Parameter(name = "deviceCode", description = "摄像头设备国标编号, 对于非国标摄像头可以不设置此参数")
    public void stopPlay(String deviceId, @RequestParam(required = false) String deviceCode) {
        log.info("[SY-停止播放摄像头] API调用，deviceId：{} ，deviceCode：{} ",deviceId, deviceCode);
        channelService.stopPlay(deviceId, deviceCode);
    }

    @Operation(summary = "云台控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "通道编号")
    @Parameter(name = "deviceCode", description = "摄像头设备国标编号, 对于非国标摄像头可以不设置此参数")
    @Parameter(name = "command", description = "控制指令,允许值: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop", required = true)
    @Parameter(name = "speed", description = "速度(0-100)", required = true)
    @GetMapping("/camera/control/ptz")
    public DeferredResult<WVPResult<String>> ptz(String deviceId, @RequestParam(required = false) String deviceCode, String command, Integer speed){

        log.info("[SY-云台控制] API调用，deviceId：{} ，deviceCode：{} ，command：{} ，speed：{} ",deviceId, deviceCode, command, speed);

        DeferredResult<WVPResult<String>> result = new DeferredResult<>();

        result.onTimeout(()->{
            WVPResult<String> wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "请求超时");
            result.setResult(wvpResult);
        });

        channelService.ptz(deviceId, deviceCode, command, speed, (code, msg, data) -> {
            WVPResult<String> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(msg);
            wvpResult.setData(data);
            result.setResult(wvpResult);
        });
        return result;

    }




}
