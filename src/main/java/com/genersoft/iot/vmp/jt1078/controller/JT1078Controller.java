package com.genersoft.iot.vmp.jt1078.controller;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.controller.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ConditionalOnProperty(value = "jt1078.enable", havingValue = "true")
@RestController
@Tag(name  = "部标设备")
@RequestMapping("/api/jt1078")
public class JT1078Controller {

    private final static Logger logger = LoggerFactory.getLogger(JT1078Controller.class);

    @Resource
    Ijt1078Service service;

    @Autowired
    UserSetting userSetting;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Operation(summary = "1078-开始点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "type", description = "类型：0:音视频,1:视频,3:音频", required = true)
    @GetMapping("/live/start")
    public DeferredResult<WVPResult<StreamContent>> startLive(HttpServletRequest request,
                                                              @Parameter(required = true) String deviceId,
                                                              @Parameter(required = true) String channelId,
                                                              @Parameter(required = false) Integer type) {
        if (type == null || (type != 0 && type != 1 && type != 3)) {
            type = 0;
        }
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        result.onTimeout(()->{
            logger.info("[1078-点播等待超时] deviceId：{}, channelId：{}, ", deviceId, channelId);
            // 释放rtpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("超时");
            result.setResult(wvpResult);
            service.stopPlay(deviceId, channelId);
        });

        service.play(deviceId, channelId, type, (code, msg, streamInfo) -> {
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                wvpResult.setCode(ErrorCode.SUCCESS.getCode());
                wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

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
                        streamInfo.channgeStreamIp(host);
                    }
                    wvpResult.setData(new StreamContent(streamInfo));
                }else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }
            }else {
                wvpResult.setCode(code);
                wvpResult.setMsg(msg);
            }
            result.setResult(wvpResult);
        });

        return result;
    }

    @Operation(summary = "1078-结束点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/live/stop")
    public void stopLive(HttpServletRequest request,
                                                              @Parameter(required = true) String deviceId,
                                                              @Parameter(required = true) String channelId) {
        service.stopPlay(deviceId, channelId);
    }

    @Operation(summary = "1078-语音对讲", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "app", description = "推流应用名", required = true)
    @Parameter(name = "stream", description = "推流ID", required = true)
    @Parameter(name = "mediaServerId", description = "流媒体ID", required = true)
    @Parameter(name = "onlySend", description = "是否只发送", required = false)
    @GetMapping("/talk/start")
    public DeferredResult<WVPResult<StreamContent>> startTalk(HttpServletRequest request,
                         @Parameter(required = true) String deviceId,
                         @Parameter(required = true) String channelId,
                         @Parameter(required = true) String app,
                         @Parameter(required = true) String stream,
                         @Parameter(required = true) String mediaServerId,
                         @Parameter(required = false) Boolean onlySend) {
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        String finalChannelId = channelId;
        result.onTimeout(()->{
            logger.info("[1078-语音对讲超时] deviceId：{}, channelId：{}, ", deviceId, finalChannelId);
            // 释放rtpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("超时");
            result.setResult(wvpResult);
            service.stopPlay(deviceId, finalChannelId);
        });

        service.startTalk(deviceId, channelId, app, stream, mediaServerId, onlySend, (code, msg, streamInfo) -> {
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                wvpResult.setCode(ErrorCode.SUCCESS.getCode());
                wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

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
                        streamInfo.channgeStreamIp(host);
                    }
                    wvpResult.setData(new StreamContent(streamInfo));
                }else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }
            }else {
                wvpResult.setCode(code);
                wvpResult.setMsg(msg);
            }
            result.setResult(wvpResult);
        });

        return result;
    }

    @Operation(summary = "1078-结束对讲", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/talk/stop")
    public void stopTalk(HttpServletRequest request,
                         @Parameter(required = true) String deviceId,
                         @Parameter(required = true) String channelId) {
        service.stopTalk(deviceId, channelId);
    }


    @Operation(summary = "1078-暂停点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/live/pause")
    public void pauseLive(HttpServletRequest request,
                         @Parameter(required = true) String deviceId,
                         @Parameter(required = true) String channelId) {
        service.pausePlay(deviceId, channelId);
    }

    @Operation(summary = "1078-继续点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/live/continue")
    public void continueLive(HttpServletRequest request,
                          @Parameter(required = true) String deviceId,
                          @Parameter(required = true) String channelId) {

        service.continueLivePlay(deviceId, channelId);
    }

    @Operation(summary = "1078-切换码流类型", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "streamType", description = "0:主码流; 1:子码流", required = true)
    @GetMapping("/live/continue")
    public void changeStreamType(HttpServletRequest request,
                             @Parameter(required = true) String deviceId,
                             @Parameter(required = true) String channelId,
                             @Parameter(required = true) Integer streamType) {
        service.changeStreamType(deviceId, channelId, streamType);
    }

    @Operation(summary = "1078-录像-查询资源列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "startTime", description = "开始时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "endTime", description = "结束时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @GetMapping("/record/list")
    public WVPResult<List<J1205.JRecordItem>> playbackList(HttpServletRequest request,
                                                                     @Parameter(required = true) String deviceId,
                                                                     @Parameter(required = false) String channelId,
                                                                     @Parameter(required = true) String startTime,
                                                                     @Parameter(required = true) String endTime
    ) {
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        List<J1205.JRecordItem> recordList = service.getRecordList(deviceId, channelId, startTime, endTime);
        if (recordList == null) {
            return WVPResult.fail(ErrorCode.ERROR100);
        }else {
            return WVPResult.success(recordList);
        }
    }
    @Operation(summary = "1078-开始回放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "startTime", description = "开始时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "endTime", description = "结束时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @GetMapping("/playback/start")
    public DeferredResult<WVPResult<StreamContent>> recordLive(HttpServletRequest request,
                                                              @Parameter(required = true) String deviceId,
                                                              @Parameter(required = false) String channelId,
                                                              @Parameter(required = true) String startTime,
                                                              @Parameter(required = true) String endTime) {
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        String finalChannelId = channelId;
        result.onTimeout(()->{
            logger.info("[1078-回放-等待超时] deviceId：{}, channelId：{}, ", deviceId, finalChannelId);
            // 释放rtpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("回放超时");
            result.setResult(wvpResult);
            service.stopPlay(deviceId, finalChannelId);
        });

        service.playback(deviceId, channelId, startTime, endTime, (code, msg, streamInfo) -> {
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                wvpResult.setCode(ErrorCode.SUCCESS.getCode());
                wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

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
                        streamInfo.channgeStreamIp(host);
                    }
                    wvpResult.setData(new StreamContent(streamInfo));
                }else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }
            }else {
                wvpResult.setCode(code);
                wvpResult.setMsg(msg);
            }
            result.setResult(wvpResult);
        });

        return result;
    }

    @Operation(summary = "1078-结束回放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/playback/stop")
    public void stopPlayback(HttpServletRequest request,
                         @Parameter(required = true) String deviceId,
                         @Parameter(required = false) String channelId) {
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        service.stopPlayback(deviceId, channelId);
    }

    @Operation(summary = "1078-分页查询部标设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @GetMapping("/device/list")
    public PageInfo<JTDevice> getDevices(int page, int count,
                                         @RequestParam(required = false) String query,
                                         @RequestParam(required = false) Boolean online) {
        return service.getDeviceList(page, count, query, online);
    }

    @Operation(summary = "更新设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "device", description = "设备", required = true)
    @PostMapping("/device/update")
    public void updateDevice(JTDevice device){
        assert device.getId() > 0;
        assert device.getDeviceId() != null;
        service.updateDevice(device);
    }



    @Operation(summary = "1078-新增设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "device", description = "设备", required = true)
    @PostMapping("/device/add")
    public void addDevice(JTDevice device){
        assert device.getDeviceId() != null;
        service.addDevice(device);
    }
    @Operation(summary = "删除设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备ID", required = true)
    @DeleteMapping("/device/delete")
    public void addDevice(String deviceId){
        assert deviceId != null;
        service.deleteDeviceByDeviceId(deviceId);
    }

    @Operation(summary = "1078-云台控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "command", description = "控制指令,允许值: left, right, up, down, zoomin, zoomout, irisin, irisout, focusnear, focusfar, stop", required = true)
    @Parameter(name = "speed", description = "速度(0-255)， command,值 left, right, up, down时有效", required = true)
    @PostMapping("/ptz")
    public void ptz(String deviceId, String channelId, String command, int speed){

        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        logger.info("[1078-云台控制] deviceId：{}, channelId：{}, command: {}, speed: {}", deviceId, channelId, command, speed);
        service.ptzControl(deviceId, channelId, command, speed);
    }

    @Operation(summary = "1078-补光灯开关", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "command", description = "控制指令,允许值: on off", required = true)
    @PostMapping("/fill-light")
    public void fillLight(String deviceId, String channelId, String command){

        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        logger.info("[1078-补光灯开关] deviceId：{}, channelId：{}, command: {}", deviceId, channelId, command);
        service.supplementaryLight(deviceId, channelId, command);
    }

    @Operation(summary = "1078-雨刷开关", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "command", description = "控制指令,允许值: on off", required = true)
    @PostMapping("/wiper")
    public void wiper(String deviceId, String channelId, String command){

        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        logger.info("[1078-雨刷开关] deviceId：{}, channelId：{}, command: {}", deviceId, channelId, command);
        service.wiper(deviceId, channelId, command);
    }

    @Operation(summary = "1078-查询终端参数", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @GetMapping("/config")
    public JTDeviceConfig config(String deviceId, String[] params){

        logger.info("[1078-查询终端参数] deviceId：{}", deviceId);
        return service.queryConfig(deviceId, params, null);
    }

    @Operation(summary = "1078-设置终端参数", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @Parameter(name = "config", description = "终端参数", required = true)
    @PostMapping("/set-config")
    public void setConfig(@RequestBody SetConfigParam config){

        logger.info("[1078-设置终端参数] 参数: {}", config.toString());
        service.setConfig(config.getDeviceId(), config.getConfig());
    }

    @Operation(summary = "终端控制-连接", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "control", description = "终端控制参数", required = true)
    @PostMapping("/control/connection")
    public void connectionControl(@RequestBody ConnectionControlParam control){

        logger.info("[1078-终端控制] 参数: {}", control.toString());
        service.connectionControl(control.getDeviceId(), control.getControl());
    }

    @Operation(summary = "1078-终端控制-复位", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @PostMapping("/control/reset")
    public void resetControl(String deviceId){

        logger.info("[1078-复位] deviceId: {}", deviceId);
        service.resetControl(deviceId);
    }

    @Operation(summary = "1078-终端控制-恢复出厂设置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @PostMapping("/control/factory-reset")
    public void factoryResetControl(String deviceId){

        logger.info("[1078-恢复出厂设置] deviceId: {}", deviceId);
        service.factoryResetControl(deviceId);
    }

    @Operation(summary = "1078-查询终端属性", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @GetMapping("/attribute")
    public JTDeviceAttribute attribute(String deviceId){

        logger.info("[1078-查询终端属性] deviceId: {}", deviceId);
        return service.attribute(deviceId);
    }

    @Operation(summary = "1078-查询位置信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @GetMapping("/position-info")
    public JTPositionBaseInfo queryPositionInfo(String deviceId){

        logger.info("[1078-查询位置信息] deviceId: {}", deviceId);
        return service.queryPositionInfo(deviceId);
    }

    @Operation(summary = "1078-临时位置跟踪控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @Parameter(name = "timeInterval", description = "时间间隔,单位为秒,时间间隔为0 时停止跟踪,停止跟踪无需带后继字段", required = true)
    @Parameter(name = "validityPeriod", description = "位置跟踪有效期, 单位为秒,终端在接收到位置跟踪控制消息后,在有效期截止时间之前依据消息中的时间间隔发送位置汇报", required = true)
    @GetMapping("/control/temp-position-tracking")
    public void tempPositionTrackingControl(String deviceId, Integer timeInterval, Long validityPeriod){

        logger.info("[1078-临时位置跟踪控制] deviceId: {}, 时间间隔 {}秒, 位置跟踪有效期 {}秒", deviceId, timeInterval, validityPeriod);
        service.tempPositionTrackingControl(deviceId, timeInterval, validityPeriod);
    }

    @Operation(summary = "1078-人工确认报警消息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @Parameter(name = "timeInterval", description = "时间间隔,单位为秒,时间间隔为0 时停止跟踪,停止跟踪无需带后继字段", required = true)
    @Parameter(name = "validityPeriod", description = "位置跟踪有效期, 单位为秒,终端在接收到位置跟踪控制消息后,在有效期截止时间之前依据消息中的时间间隔发送位置汇报", required = true)
    @PostMapping("/confirmation-alarm-message")
    public void confirmationAlarmMessage(@RequestBody ConfirmationAlarmMessageParam param){

        logger.info("[1078-人工确认报警消息] 参数: {}", param);
        service.confirmationAlarmMessage(param.getDeviceId(), param.getAlarmPackageNo(), param.getAlarmMessageType());
    }

    @Operation(summary = "1078-链路检测", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @GetMapping("/link-detection")
    public WVPResult<Integer> linkDetection(String deviceId){

        logger.info("[1078-链路检测] deviceId: {}", deviceId);
        int result = service.linkDetection(deviceId);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-文本信息下发", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "textMessageParam", description = "文本信息下发参数", required = true)
    @PostMapping("/text-msg")
    public WVPResult<Integer> textMessage(@RequestBody TextMessageParam textMessageParam){

        logger.info("[1078-文本信息下发] textMessageParam: {}", textMessageParam);
        int result = service.textMessage(textMessageParam.getDeviceId(), textMessageParam.getSign(), textMessageParam.getTextType(), textMessageParam.getContent());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-电话回拨", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @Parameter(name = "sign", description = "标志: 0:普通通话,1:监听", required = true)
    @Parameter(name = "phoneNumber", description = "电话号码", required = true)
    @GetMapping("/telephone-callback")
    public WVPResult<Integer> telephoneCallback(String deviceId, Integer sign, String phoneNumber){

        logger.info("[1078-电话回拨] deviceId: {}, sign: {}, phoneNumber: {},", deviceId, sign, phoneNumber);
        int result = service.telephoneCallback(deviceId, sign, phoneNumber);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-设置电话本", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "setPhoneBookParam", description = "设置电话本参数", required = true)
    @PostMapping("/set-phone-book")
    public WVPResult<Integer> setPhoneBook(@RequestBody SetPhoneBookParam setPhoneBookParam){

        logger.info("[1078-设置电话本] setPhoneBookParam: {}", setPhoneBookParam);
        int result = service.setPhoneBook(setPhoneBookParam.getDeviceId(), setPhoneBookParam.getType(), setPhoneBookParam.getPhoneBookContactList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-车门控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @Parameter(name = "open", description = "开启车门", required = true)
    @GetMapping("/control/door")
    public WVPResult<Integer> controlDoor(String deviceId, Boolean open){

        logger.info("[1078-车门控制] deviceId: {}, open: {},", deviceId, open);
        JTPositionBaseInfo positionBaseInfo = service.controlDoor(deviceId, open);

        if (open == !positionBaseInfo.getStatus().isDoorLocking()) {
            return WVPResult.success(null);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "1078-更新圆形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/circle/update")
    public WVPResult<Integer> updateAreaForCircle(@RequestBody SetAreaParam areaParam){

        logger.info("[1078-更新圆形区域] areaParam: {},", areaParam);
        int result = service.setAreaForCircle(0, areaParam.getDeviceId(), areaParam.getCircleAreaList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-追加圆形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/circle/add")
    public WVPResult<Integer> addAreaForCircle(@RequestBody SetAreaParam areaParam){

        logger.info("[1078-追加圆形区域] areaParam: {},", areaParam);
        int result = service.setAreaForCircle(1, areaParam.getDeviceId(), areaParam.getCircleAreaList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-修改圆形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/circle/edit")
    public WVPResult<Integer> editAreaForCircle(@RequestBody SetAreaParam areaParam){

        logger.info("[1078-修改圆形区域] areaParam: {},", areaParam);
        int result = service.setAreaForCircle(2, areaParam.getDeviceId(), areaParam.getCircleAreaList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-删除圆形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @Parameter(name = "ids", description = "待删除圆形区域的id，例如1,2,3", required = true)
    @GetMapping("/area/circle/delete")
    public WVPResult<Integer> deleteAreaForCircle(String deviceId, @RequestParam(value = "ids", required = false) List<Long> ids){

        logger.info("[1078-删除圆形区域] deviceId: {}, ids:{}", deviceId, ids);
        int result = service.deleteAreaForCircle(deviceId, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-查询圆形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @GetMapping("/area/circle/query")
    public WVPResult<List<JTAreaOrRoute>> queryAreaForCircle(String deviceId, @RequestParam(value = "ids", required = false) List<Long> ids){

        logger.info("[1078-查询圆形区域] deviceId: {}, ids:{}", deviceId, ids);
        List<JTAreaOrRoute> result = service.queryAreaForCircle(deviceId, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }


    @Operation(summary = "1078-更新矩形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/rectangle/update")
    public WVPResult<Integer> updateAreaForRectangle(@RequestBody SetAreaParam areaParam){

        logger.info("[1078-更新矩形区域] areaParam: {},", areaParam);
        int result = service.setAreaForRectangle(0, areaParam.getDeviceId(), areaParam.getRectangleAreas());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-追加矩形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/rectangle/add")
    public WVPResult<Integer> addAreaForRectangle(@RequestBody SetAreaParam areaParam){

        logger.info("[1078-追加矩形区域] areaParam: {},", areaParam);
        int result = service.setAreaForRectangle(1, areaParam.getDeviceId(), areaParam.getRectangleAreas());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-修改矩形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/rectangle/edit")
    public WVPResult<Integer> editAreaForRectangle(@RequestBody SetAreaParam areaParam){

        logger.info("[1078-修改矩形区域] areaParam: {},", areaParam);
        int result = service.setAreaForRectangle(2, areaParam.getDeviceId(), areaParam.getRectangleAreas());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-删除矩形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @Parameter(name = "ids", description = "待删除圆形区域的id，例如1,2,3", required = true)
    @GetMapping("/area/rectangle/delete")
    public WVPResult<Integer> deleteAreaForRectangle(String deviceId, @RequestParam(value = "ids", required = false) List<Long> ids){

        logger.info("[1078-删除矩形区域] deviceId: {}, ids:{}", deviceId, ids);
        int result = service.deleteAreaForRectangle(deviceId, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-查询矩形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @GetMapping("/area/rectangle/query")
    public WVPResult<List<JTAreaOrRoute>> queryAreaForRectangle(String deviceId, @RequestParam(value = "ids", required = false) List<Long> ids){

        logger.info("[1078-查询矩形区域] deviceId: {}, ids:{}", deviceId, ids);
        List<JTAreaOrRoute> result = service.queryAreaForRectangle(deviceId, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "1078-设置多边形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/polygon/set")
    public WVPResult<Integer> setAreaForPolygon(@RequestBody SetAreaParam areaParam){

        logger.info("[1078-设置多边形区域] areaParam: {},", areaParam);
        int result = service.setAreaForPolygon(areaParam.getDeviceId(), areaParam.getPolygonArea());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-删除多边形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @Parameter(name = "ids", description = "待删除圆形区域的id，例如1,2,3", required = true)
    @GetMapping("/area/polygon/delete")
    public WVPResult<Integer> deleteAreaForPolygon(String deviceId, @RequestParam(value = "ids", required = false) List<Long> ids){

        logger.info("[1078-删除多边形区域] deviceId: {}, ids:{}", deviceId, ids);
        int result = service.deleteAreaForPolygon(deviceId, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-查询多边形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @GetMapping("/area/polygon/query")
    public WVPResult<List<JTAreaOrRoute>> queryAreaForPolygon(String deviceId, @RequestParam(value = "ids", required = false) List<Long> ids){

        logger.info("[1078-查询多边形区域] deviceId: {}, ids:{}", deviceId, ids);
        List<JTAreaOrRoute> result = service.queryAreaForPolygon(deviceId, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "1078-设置路线", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/route/set")
    public WVPResult<Integer> setRoute(@RequestBody SetAreaParam areaParam){

        logger.info("[1078-设置路线] areaParam: {},", areaParam);
        int result = service.setRoute(areaParam.getDeviceId(), areaParam.getRoute());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-删除路线", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @Parameter(name = "ids", description = "待删除圆形区域的id，例如1,2,3", required = true)
    @GetMapping("/route/delete")
    public WVPResult<Integer> deleteRoute(String deviceId, @RequestParam(value = "ids", required = false) List<Long> ids){

        logger.info("[1078-删除路线] deviceId: {}, ids:{}", deviceId, ids);
        int result = service.deleteRoute(deviceId, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "1078-查询路线", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @GetMapping("/route/query")
    public WVPResult<List<JTAreaOrRoute>> queryRoute(String deviceId, @RequestParam(value = "ids", required = false) List<Long> ids){

        logger.info("[1078-查询路线] deviceId: {}, ids:{}", deviceId, ids);
        List<JTAreaOrRoute> result = service.queryRoute(deviceId, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    // TODO 待实现 行驶记录数据采集命令 行驶记录数据上传 行驶记录参数下传命令 电子运单上报 CAN总线数据上传

    @Operation(summary = "1078-上报驾驶员身份信息请求", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @GetMapping("/driver-information")
    public WVPResult<JTDriverInformation> queryDriverInformation(String deviceId){

        logger.info("[1078-上报驾驶员身份信息请求] deviceId: {}", deviceId);
        JTDriverInformation jtDriverInformation = service.queryDriverInformation(deviceId);
        if (jtDriverInformation != null) {
            return WVPResult.success(jtDriverInformation);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "1078-摄像头立即拍摄命令", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备编号", required = true)
    @PostMapping("/shooting")
    public WVPResult<List<Long>> shooting(@RequestBody ShootingParam param){

        logger.info("[1078-摄像头立即拍摄命令] param: {}", param );
        List<Long> ids = service.shooting(param.getDeviceId(), param.getShootingCommand());
        if (ids != null) {
            return WVPResult.success(ids);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "1078-存储多媒体数据检索", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "param", description = "存储多媒体数据参数", required = true)
    @PostMapping("/media-data-info")
    public WVPResult<List<JTMediaDataInfo>> queryMediaData(@RequestBody QueryMediaDataParam param){

        logger.info("[1078-存储多媒体数据检索] param: {}", param );
        List<JTMediaDataInfo> ids = service.queryMediaData(param.getDeviceId(), param.getQueryMediaDataCommand());
        if (ids != null) {
            return WVPResult.success(ids);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "1078-存储多媒体数据上传命令", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "param", description = "存储多媒体数据参数", required = true)
    @PostMapping("/media-data-upload")
    public DeferredResult<WVPResult<List<String>>> updateMediaData(@RequestBody QueryMediaDataParam param){

        logger.info("[1078-存储多媒体数据上传命令] param: {}", param );
        DeferredResult<WVPResult<List<String>>> deferredResult = new DeferredResult<>(30000L);
        List<String> resultList = new ArrayList<>();

        deferredResult.onTimeout(()->{
            logger.info("[1078-存储多媒体数据上传命令超时] param: {}", param );
            WVPResult<List<String>> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setMsg("超时");
            fail.setData(resultList);
            deferredResult.setResult(fail);
        });
        List<JTMediaDataInfo> ids;
        if (param.getMediaId() != null) {
            ids = new ArrayList<>();
            JTMediaDataInfo mediaDataInfo = new JTMediaDataInfo();
            mediaDataInfo.setId(param.getMediaId());
            ids.add(mediaDataInfo);
        }else {
            ids = service.queryMediaData(param.getDeviceId(), param.getQueryMediaDataCommand());
        }
        if (ids.isEmpty()) {
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100));
            return deferredResult;
        }
        Map<String, JTMediaDataInfo> idMap= new HashMap<>();
        for (JTMediaDataInfo mediaDataInfo : ids) {
            idMap.put(mediaDataInfo.getId() + ".jpg", mediaDataInfo);
        }
        // 开启文件监听
        FileAlterationObserver observer = new FileAlterationObserver(new File("mediaEvent"));
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
               if (idMap.containsKey(file.getName())) {
                   idMap.remove(file.getName());
                   resultList.add("mediaEvent" + File.separator + file.getName());
                   if (idMap.isEmpty()) {
                       deferredResult.setResult(WVPResult.success(resultList));
                   }
               }
            }
        });
        FileAlterationMonitor monitor = new FileAlterationMonitor(5, observer);
        try {
            monitor.start();
        } catch (Exception e) {
            logger.info("[1078-存储多媒体数据上传命令监听文件失败] param: {}", param );
            deferredResult.setResult(null);
            return deferredResult;
        }
        taskExecutor.execute(()->{
            if (param.getMediaId() != null) {
                service.uploadMediaDataForSingle(param.getDeviceId(), param.getMediaId(), param.getDelete());
            }else {
                service.uploadMediaData(param.getDeviceId(), param.getQueryMediaDataCommand());
            }

        });
        return deferredResult;
    }

    @Operation(summary = "1078-开始录音", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "time", description = "录音时间，单位为秒(s) ,0 表示一直录音", required = false)
    @Parameter(name = "save", description = "0:实时上传；1:保存", required = false)
    @Parameter(name = "samplingRate", description = "音频采样率， 0:8K；1:11K；2:23K；3:32K", required = false)
    @GetMapping("/record/start")
    public void startRecord(HttpServletRequest request,
                         @Parameter(required = true) String deviceId,
                         @Parameter(required = false) Integer time,
                         @Parameter(required = false) Integer save,
                         @Parameter(required = false) Integer samplingRate
                            ) {
        if (ObjectUtils.isEmpty(time)) {
            time = 0;
        }
        if (ObjectUtils.isEmpty(save)) {
            save = 0;
        }
        if (ObjectUtils.isEmpty(samplingRate)) {
            samplingRate = 0;
        }
        service.record(deviceId, 1, time, save, samplingRate);
    }

    @Operation(summary = "1078-停止录音", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "time", description = "录音时间，单位为秒(s) ,0 表示一直录音", required = false)
    @Parameter(name = "save", description = "0:实时上传；1:保存", required = false)
    @Parameter(name = "samplingRate", description = "音频采样率， 0:8K；1:11K；2:23K；3:32K", required = false)
    @GetMapping("/record/stop")
    public void stopRecord(HttpServletRequest request,
                            @Parameter(required = true) String deviceId,
                            @Parameter(required = false) Integer time,
                            @Parameter(required = false) Integer save,
                            @Parameter(required = false) Integer samplingRate
    ) {
        if (ObjectUtils.isEmpty(time)) {
            time = 0;
        }
        if (ObjectUtils.isEmpty(save)) {
            save = 0;
        }
        if (ObjectUtils.isEmpty(samplingRate)) {
            samplingRate = 0;
        }
        service.record(deviceId, 0, time, save, samplingRate);
    }

    @Operation(summary = "1078-查询终端音视频属性", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @GetMapping("/media/attribute")
    public JTMediaAttribute queryMediaAttribute(HttpServletRequest request,
                           @Parameter(required = true) String deviceId
    ) {
        return service.queryMediaAttribute(deviceId);
    }

    // TODO 视频报警上报


}

