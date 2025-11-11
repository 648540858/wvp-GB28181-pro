package com.genersoft.iot.vmp.web.custom;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.ICloudRecordService;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushPlayService;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.HttpUtils;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.vmanager.cloudRecord.bean.CloudRecordUrl;
import com.genersoft.iot.vmp.web.custom.bean.*;
import com.genersoft.iot.vmp.web.custom.service.CameraChannelService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Tag(name  = "第三方接口")
@Slf4j
@RestController
@RequestMapping(value = "/api/sy")
@ConditionalOnProperty(value = "sy.enable", havingValue = "true")
@Hidden
public class CameraChannelController {

    @Autowired
    private CameraChannelService channelService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ICloudRecordService cloudRecordService;

    @Autowired
    private IStreamPushPlayService streamPushPlayService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private IStreamPushService streamPushService;

    @Value("${sy.ptz-control-time-interval}")
    private int ptzControlTimeInterval = 300;

    @GetMapping(value = "/camera/list")
    @ResponseBody
    @Operation(summary = "查询摄像机列表, 只查询当前虚拟组织下的", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @Parameter(name = "groupAlias", description = "分组别名")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    @Parameter(name = "status", description = "摄像头状态")
    public PageInfo<CameraChannel> queryList(@RequestParam(required = false, value = "page", defaultValue = "1" )Integer page,
                                        @RequestParam(required = false, value = "count", defaultValue = "100")Integer count,
                                        String groupAlias,
                                        @RequestParam(required = false) String geoCoordSys,
                                        @RequestParam(required = false) Boolean status){


        return channelService.queryList(page, count, groupAlias, status, geoCoordSys);
    }

    @GetMapping(value = "/camera/list-with-child")
    @ResponseBody
    @Operation(summary = "查询摄像机列表, 查询当前虚拟组织下以及全部子节点", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "sortName", description = "排序字段名")
    @Parameter(name = "order", description = "排序方式（true: 升序 或 false: 降序 ）")
    @Parameter(name = "groupAlias", description = "分组别名")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    @Parameter(name = "status", description = "摄像头状态")
    public PageInfo<CameraChannel> queryListWithChild(@RequestParam(required = false, value = "page", defaultValue = "1" )Integer page,

                                        @RequestParam(required = false, value = "count", defaultValue = "100")Integer count,
                                        @RequestParam(required = false) String query,
                                        @RequestParam(required = false) String sortName,
                                        @RequestParam(required = false) Boolean order,
                                        @RequestParam(required = false) String groupAlias,
                                        @RequestParam(required = false) String geoCoordSys,
                                        @RequestParam(required = false) Boolean status){
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

        return channelService.queryListWithChild(page, count, query, sortName, order, groupAlias, status, geoCoordSys);
    }

    @GetMapping(value = "/camera/cont-with-child")
    @ResponseBody
    @Operation(summary = "查询摄像机列表的总数和在线数", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "groupAlias", description = "分组别名")
    public List<CameraCount> queryCountWithChild(String groupAlias){
        return channelService.queryCountWithChild(groupAlias);
    }

    @GetMapping(value = "/camera/one")
    @ResponseBody
    @Operation(summary = "查询单个摄像头信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "通道编号")
    @Parameter(name = "deviceCode", description = "摄像头设备国标编号, 对于非国标摄像头可以不设置此参数")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    public CameraChannel getOne(String deviceId, @RequestParam(required = false) String deviceCode,
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
    public void updateCamera(String deviceId,
                                      @RequestParam(required = false) String deviceCode,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) Double longitude,
                                      @RequestParam(required = false) Double latitude,
                                      @RequestParam(required = false) String geoCoordSys) {
        channelService.updateCamera(deviceId, deviceCode, name, longitude, latitude, geoCoordSys);
    }

    @PostMapping(value = "/camera/list/ids")
    @ResponseBody
    @Operation(summary = "根据编号查询多个摄像头信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<CameraChannel> queryListByDeviceIds(@RequestBody IdsQueryParam param) {
        return channelService.queryListByDeviceIds(param.getDeviceIds(), param.getGeoCoordSys());
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
        return channelService.queryListInBox(minLongitude, maxLongitude, minLatitude, maxLatitude, level, groupAlias, geoCoordSys);
    }

    @PostMapping(value = "/camera/list/polygon")
    @ResponseBody
    @Operation(summary = "根据多边形查询摄像头", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<CameraChannel> queryListInPolygon(@RequestBody PolygonQueryParam param) {
        return channelService.queryListInPolygon(param.getPosition(), param.getGroupAlias(), param.getLevel(), param.getGeoCoordSys());
    }

    @GetMapping(value = "/camera/list/circle")
    @ResponseBody
    @Operation(summary = "根据圆范围查询摄像头", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "centerLongitude", description = "圆心经度")
    @Parameter(name = "centerLatitude", description = "圆心纬度")
    @Parameter(name = "radius", description = "查询范围的半径，单位米")
    @Parameter(name = "level", description = "地图级别")
    @Parameter(name = "groupAlias", description = "分组别名")
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    public List<CameraChannel> queryListInCircle(Double centerLongitude, Double centerLatitude, Double radius, String groupAlias,
                                                 @RequestParam(required = false) String geoCoordSys, @RequestParam(required = false) Integer level) {
        return channelService.queryListInCircle(centerLongitude, centerLatitude, radius, level, groupAlias, geoCoordSys);
    }

    @GetMapping(value = "/camera/list/address")
    @ResponseBody
    @Operation(summary = "根据安装地址和监视方位获取摄像头", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "address", description = "安装地址")
    @Parameter(name = "directionType", description = "监视方位", required = false)
    @Parameter(name = "geoCoordSys", description = "坐标系类型：WGS84,GCJ02、BD09")
    public List<CameraChannel> queryListByAddressAndDirectionType(String address, @RequestParam(required = false) Integer directionType, @RequestParam(required = false) String geoCoordSys) {
        return channelService.queryListByAddressAndDirectionType(address, directionType, geoCoordSys);
    }

    @GetMapping(value = "/camera/control/play")
    @ResponseBody
    @Operation(summary = "播放摄像头", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "通道编号")
    @Parameter(name = "deviceCode", description = "摄像头设备国标编号, 对于非国标摄像头可以不设置此参数")
    public DeferredResult<WVPResult<CameraStreamContent>> play(HttpServletRequest request, String deviceId, @RequestParam(required = false) String deviceCode) {

        log.info("[SY-播放摄像头] API调用，deviceId：{} ，deviceCode：{} ",deviceId, deviceCode);
        DeferredResult<WVPResult<CameraStreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

        ErrorCallback<CameraStreamInfo> callback = (code, msg, cameraStreamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo = cameraStreamInfo.getStreamInfo();
                CommonGBChannel channel = cameraStreamInfo.getChannel();
                WVPResult<CameraStreamContent> wvpResult = WVPResult.success();
                if (cameraStreamInfo.getStreamInfo() != null) {
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
                    CameraStreamContent cameraStreamContent = new CameraStreamContent(streamInfo);
                    cameraStreamContent.setName(channel.getGbName());
                    if (channel.getGbPtzType() != null) {
                        cameraStreamContent.setControlType(
                                (channel.getGbPtzType() == 1 || channel.getGbPtzType() == 4 || channel.getGbPtzType() == 5) ? 1 : 0);
                    }else {
                        cameraStreamContent.setControlType(0);
                    }

                    wvpResult.setData(cameraStreamContent);
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
        // 设置时间间隔后自动发送停止
        if (!command.equalsIgnoreCase("stop")) {
            dynamicTask.startDelay(UUID.randomUUID().toString(), () -> {
                channelService.ptz(deviceId, deviceCode, "stop", speed, (code, msg, data) -> {});
            }, ptzControlTimeInterval);
        }
        return result;
    }

    @GetMapping(value = "/camera/list-for-mobile")
    @ResponseBody
    @Operation(summary = "查询移动设备摄像机列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @Parameter(name = "topGroupAlias", description = "分组别名")
    public PageInfo<CameraChannel> queryListForMobile(@RequestParam(required = false, value = "page", defaultValue = "1" )Integer page,
                                                      @RequestParam(required = false, value = "count", defaultValue = "100")Integer count,
                                                      @RequestParam(required = false) String topGroupAlias){

        return channelService.queryListForMobile(page, count, topGroupAlias);
    }


    @Operation(summary = "获取推流播放地址", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    @Parameter(name = "callId", description = "推流时携带的自定义鉴权ID", required = true)
    @GetMapping(value = "/push/play")
    @ResponseBody
    public DeferredResult<WVPResult<StreamContent>> getStreamInfoByAppAndStream(HttpServletRequest request,
                                                                                String app,
                                                                                String stream,
                                                                                String callId){
        StreamPush streamPush = streamPushService.getPush(app, stream);
        Assert.notNull(streamPush, "地址不存在");

        // 权限校验
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
        if (streamAuthorityInfo == null
                || streamAuthorityInfo.getCallId() == null
                || !streamAuthorityInfo.getCallId().equals(callId)) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "播放地址鉴权失败");
        }

        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        result.onTimeout(()->{
            WVPResult<StreamContent> fail = WVPResult.fail(ErrorCode.ERROR100.getCode(), "等待推流超时");
            result.setResult(fail);
        });

        streamPushPlayService.start(streamPush.getId(), (code, msg, streamInfo) -> {
            if (code == 0 && streamInfo != null) {
                streamInfo=streamInfo.clone();//深拷贝
                String host;
                try {
                    URL url=new URL(request.getRequestURL().toString());
                    host=url.getHost();
                } catch (MalformedURLException e) {
                    host=request.getLocalAddr();
                }
                streamInfo.changeStreamIp(host);
                WVPResult<StreamContent> success = WVPResult.success(new StreamContent(streamInfo));
                result.setResult(success);
            }
        }, null, null);
        return result;
    }

    @Operation(summary = "获取推流播放地址（不做检查）", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    @Parameter(name = "callId", description = "推流时携带的自定义鉴权ID", required = true)
    @GetMapping(value = "/push/play-without-check")
    @ResponseBody
    public StreamContent getStreamInfoByAppAndStreamWithoutCheck(HttpServletRequest request,
                                                                                String app,
                                                                                String stream,
                                                                                String callId){

        MediaServer mediaServer = mediaServerService.getDefaultMediaServer();
        Assert.notNull(mediaServer, "流媒体服务器不存在");
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServer, app, stream, null, callId);
        streamInfo=streamInfo.clone();//深拷贝
        String host;
        try {
            URL url=new URL(request.getRequestURL().toString());
            host=url.getHost();
        } catch (MalformedURLException e) {
            host=request.getLocalAddr();
        }
        streamInfo.changeStreamIp(host);
        return new StreamContent(streamInfo);
    }

    @ResponseBody
    @GetMapping("/record/collect/add")
    @Operation(summary = "添加收藏")
    @Parameter(name = "app", description = "应用名", required = false)
    @Parameter(name = "stream", description = "流ID", required = false)
    @Parameter(name = "mediaServerId", description = "流媒体ID", required = false)
    @Parameter(name = "startTime", description = "鉴权ID", required = false)
    @Parameter(name = "endTime", description = "鉴权ID", required = false)
    @Parameter(name = "callId", description = "鉴权ID", required = false)
    @Parameter(name = "recordId", description = "录像记录的ID，用于精准收藏一个视频文件", required = false)
    public int addCollect(@RequestParam(required = false) String app, @RequestParam(required = false) String stream, @RequestParam(required = false) String mediaServerId, @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime, @RequestParam(required = false) String callId, @RequestParam(required = false) Integer recordId) {
        log.info("[云端录像] 添加收藏，app={}，stream={},mediaServerId={},startTime={},endTime={},callId={},recordId={}", app, stream, mediaServerId, startTime, endTime, callId, recordId);
        if (recordId != null) {
            return cloudRecordService.changeCollectById(recordId, true);
        } else {
            return cloudRecordService.changeCollect(true, app, stream, mediaServerId, startTime, endTime, callId);
        }
    }

    @ResponseBody
    @GetMapping("/record/collect/delete")
    @Operation(summary = "移除收藏")
    @Parameter(name = "app", description = "应用名", required = false)
    @Parameter(name = "stream", description = "流ID", required = false)
    @Parameter(name = "mediaServerId", description = "流媒体ID", required = false)
    @Parameter(name = "startTime", description = "鉴权ID", required = false)
    @Parameter(name = "endTime", description = "鉴权ID", required = false)
    @Parameter(name = "callId", description = "鉴权ID", required = false)
    @Parameter(name = "recordId", description = "录像记录的ID，用于精准精准移除一个视频文件的收藏", required = false)
    public int deleteCollect(@RequestParam(required = false) String app, @RequestParam(required = false) String stream, @RequestParam(required = false) String mediaServerId, @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime, @RequestParam(required = false) String callId, @RequestParam(required = false) Integer recordId) {
        log.info("[云端录像] 移除收藏，app={}，stream={},mediaServerId={},startTime={},endTime={},callId={},recordId={}", app, stream, mediaServerId, startTime, endTime, callId, recordId);
        if (recordId != null) {
            return cloudRecordService.changeCollectById(recordId, false);
        } else {
            return cloudRecordService.changeCollect(false, app, stream, mediaServerId, startTime, endTime, callId);
        }
    }

    /************************* 以下这些接口只适合wvp和zlm部署在同一台服务器的情况，且wvp只有一个zlm节点的情况 ***************************************/

    /**
     * 下载指定录像文件的压缩包
     * @param app 应用名
     * @param stream 流ID
     * @param callId 每次录像的唯一标识，置空则查询全部流媒体
     */
    @ResponseBody
    @GetMapping("/record/zip")
    public void downloadZipFile(HttpServletResponse response,
                                @RequestParam(required = false) String app,
                                @RequestParam(required = false) String stream,
                                @RequestParam(required = false) String callId

    ) {
        log.info("[下载指定录像文件的压缩包] 查询 app->{}, stream->{}, callId->{}", app, stream, callId);

        if (app != null && ObjectUtils.isEmpty(app.trim())) {
            app = null;
        }
        if (stream != null && ObjectUtils.isEmpty(stream.trim())) {
            stream = null;
        }
        if (callId != null && ObjectUtils.isEmpty(callId.trim())) {
            callId = null;
        }
        // 设置响应头
        response.setContentType("application/zip");
        response.setCharacterEncoding("UTF-8");
        if (stream != null && callId != null) {
            response.addHeader("Content-Disposition", "attachment;filename=" + stream + "_" + callId + ".zip");
        }
        List<CloudRecordUrl> cloudRecordItemList = cloudRecordService.getUrlList(app, stream, callId);
        if (ObjectUtils.isEmpty(cloudRecordItemList)) {
            log.warn("[下载指定录像文件的压缩包] 未找到录像文件，app->{}, stream->{}, callId->{}", app, stream, callId);
            return;
        }

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (CloudRecordUrl recordUrl : cloudRecordItemList) {
                try {
                    zos.putNextEntry(new ZipEntry(recordUrl.getFileName()));
                    boolean downloadSuccess = HttpUtils.downLoadFile(recordUrl.getDownloadUrl(), zos);
                    if (!downloadSuccess) {
                        log.warn("[下载指定录像文件的压缩包] 下载文件失败: {}", recordUrl.getDownloadUrl());
                        zos.closeEntry();
                        continue;
                    }
                    zos.closeEntry();
                } catch (Exception e) {
                    log.error("[下载指定录像文件的压缩包] 处理文件失败: {}, 错误: {}", recordUrl.getFileName(), e.getMessage());
                    // 继续处理下一个文件
                }
            }
        } catch (IOException e) {
            log.error("[下载指定录像文件的压缩包] 创建压缩包失败，查询 app->{}, stream->{}, callId->{}", app, stream, callId, e);
        }
    }

    /**
     *
     * @param query 检索内容
     * @param app 应用名
     * @param stream 流ID
     * @param startTime 开始时间(yyyy-MM-dd HH:mm:ss)
     * @param endTime 结束时间(yyyy-MM-dd HH:mm:ss)
     * @param mediaServerId 流媒体ID，置空则查询全部流媒体
     * @param callId 每次录像的唯一标识，置空则查询全部流媒体
     * @param remoteHost 拼接播放地址时使用的远程地址
     */
    @ResponseBody
    @GetMapping("/record/list-url")
    @Operation(summary = "分页查询云端录像", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "query", description = "检索内容", required = false)
    @Parameter(name = "app", description = "应用名", required = false)
    @Parameter(name = "stream", description = "流ID", required = false)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @Parameter(name = "startTime", description = "开始时间(yyyy-MM-dd HH:mm:ss)", required = false)
    @Parameter(name = "endTime", description = "结束时间(yyyy-MM-dd HH:mm:ss)", required = false)
    @Parameter(name = "mediaServerId", description = "流媒体ID，置空则查询全部流媒体", required = false)
    @Parameter(name = "callId", description = "每次录像的唯一标识，置空则查询全部流媒体", required = false)
    public PageInfo<CloudRecordUrl> getListWithUrl(HttpServletRequest request, @RequestParam(required = false) String query, @RequestParam(required = false) String app, @RequestParam(required = false) String stream, @RequestParam int page, @RequestParam int count, @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime, @RequestParam(required = false) String mediaServerId, @RequestParam(required = false) String callId, @RequestParam(required = false) String remoteHost

    ) {
        log.info("[云端录像] 查询URL app->{}, stream->{}, mediaServerId->{}, page->{}, count->{}, startTime->{}, endTime->{}, callId->{}", app, stream, mediaServerId, page, count, startTime, endTime, callId);

        List<MediaServer> mediaServers;
        if (!ObjectUtils.isEmpty(mediaServerId)) {
            mediaServers = new ArrayList<>();
            MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
            if (mediaServer == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到流媒体: " + mediaServerId);
            }
            mediaServers.add(mediaServer);
        } else {
            mediaServers = null;
        }
        if (query != null && ObjectUtils.isEmpty(query.trim())) {
            query = null;
        }
        if (app != null && ObjectUtils.isEmpty(app.trim())) {
            app = null;
        }
        if (stream != null && ObjectUtils.isEmpty(stream.trim())) {
            stream = null;
        }
        if (startTime != null && ObjectUtils.isEmpty(startTime.trim())) {
            startTime = null;
        }
        if (endTime != null && ObjectUtils.isEmpty(endTime.trim())) {
            endTime = null;
        }
        if (callId != null && ObjectUtils.isEmpty(callId.trim())) {
            callId = null;
        }
        MediaServer mediaServer = mediaServerService.getDefaultMediaServer();
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到流媒体节点");
        }
        if (remoteHost == null) {
            remoteHost = request.getScheme() + "://" + request.getLocalAddr() + ":" + (request.getScheme().equals("https") ? mediaServer.getHttpSSlPort() : mediaServer.getHttpPort());
        }
        PageInfo<CloudRecordItem> cloudRecordItemPageInfo = cloudRecordService.getList(page, count, query, app, stream, startTime, endTime, mediaServers, callId, null);
        PageInfo<CloudRecordUrl> cloudRecordUrlPageInfo = new PageInfo<>();
        if (!ObjectUtils.isEmpty(cloudRecordItemPageInfo)) {
            cloudRecordUrlPageInfo.setPageNum(cloudRecordItemPageInfo.getPageNum());
            cloudRecordUrlPageInfo.setPageSize(cloudRecordItemPageInfo.getPageSize());
            cloudRecordUrlPageInfo.setSize(cloudRecordItemPageInfo.getSize());
            cloudRecordUrlPageInfo.setEndRow(cloudRecordItemPageInfo.getEndRow());
            cloudRecordUrlPageInfo.setStartRow(cloudRecordItemPageInfo.getStartRow());
            cloudRecordUrlPageInfo.setPages(cloudRecordItemPageInfo.getPages());
            cloudRecordUrlPageInfo.setPrePage(cloudRecordItemPageInfo.getPrePage());
            cloudRecordUrlPageInfo.setNextPage(cloudRecordItemPageInfo.getNextPage());
            cloudRecordUrlPageInfo.setIsFirstPage(cloudRecordItemPageInfo.isIsFirstPage());
            cloudRecordUrlPageInfo.setIsLastPage(cloudRecordItemPageInfo.isIsLastPage());
            cloudRecordUrlPageInfo.setHasPreviousPage(cloudRecordItemPageInfo.isHasPreviousPage());
            cloudRecordUrlPageInfo.setHasNextPage(cloudRecordItemPageInfo.isHasNextPage());
            cloudRecordUrlPageInfo.setNavigatePages(cloudRecordItemPageInfo.getNavigatePages());
            cloudRecordUrlPageInfo.setNavigateFirstPage(cloudRecordItemPageInfo.getNavigateFirstPage());
            cloudRecordUrlPageInfo.setNavigateLastPage(cloudRecordItemPageInfo.getNavigateLastPage());
            cloudRecordUrlPageInfo.setNavigatepageNums(cloudRecordItemPageInfo.getNavigatepageNums());
            cloudRecordUrlPageInfo.setTotal(cloudRecordItemPageInfo.getTotal());
            List<CloudRecordUrl> cloudRecordUrlList = new ArrayList<>(cloudRecordItemPageInfo.getList().size());
            List<CloudRecordItem> cloudRecordItemList = cloudRecordItemPageInfo.getList();
            for (CloudRecordItem cloudRecordItem : cloudRecordItemList) {
                CloudRecordUrl cloudRecordUrl = new CloudRecordUrl();
                cloudRecordUrl.setId(cloudRecordItem.getId());
                cloudRecordUrl.setDownloadUrl(remoteHost + "/index/api/downloadFile?file_path=" + cloudRecordItem.getFilePath() + "&save_name=" + cloudRecordItem.getStream() + "_" + cloudRecordItem.getCallId() + "_" + DateUtil.timestampMsToUrlToyyyy_MM_dd_HH_mm_ss((long)cloudRecordItem.getStartTime()));
                cloudRecordUrl.setPlayUrl(remoteHost + "/index/api/downloadFile?file_path=" + cloudRecordItem.getFilePath());
                cloudRecordUrlList.add(cloudRecordUrl);
            }
            cloudRecordUrlPageInfo.setList(cloudRecordUrlList);
        }
        return cloudRecordUrlPageInfo;
    }

    @GetMapping(value = "/forceClose")
    @ResponseBody
    @Operation(summary = "强制停止推流", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public void stop(String app, String stream){
        streamPushPlayService.stop(app, stream);
    }

    @GetMapping(value = "/camera/meeting/list")
    @ResponseBody
    @Operation(summary = "查询会议设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "topGroupAlias", description = "分组别名")
    public List<CameraChannel> queryMeetingChannelList(String topGroupAlias){
        return channelService.queryMeetingChannelList(topGroupAlias);
    }


}
