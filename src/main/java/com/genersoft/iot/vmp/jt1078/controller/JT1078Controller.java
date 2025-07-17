package com.genersoft.iot.vmp.jt1078.controller;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.ftpServer.FtpSetting;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.controller.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078PlayService;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@ConditionalOnProperty(value = "jt1078.enable", havingValue = "true")
@RestController
@Tag(name  = "部标设备控制")
@RequestMapping("/api/jt1078")
public class JT1078Controller {

    @Resource
    private Ijt1078Service service;

    @Resource
    private Ijt1078PlayService jt1078PlayService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private FtpSetting ftpSetting;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Operation(summary = "JT-开始点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "type", description = "类型：0:音视频,1:视频,3:音频", required = true)
    @GetMapping("/live/start")
    public DeferredResult<WVPResult<StreamContent>> startLive(HttpServletRequest request,
                                                              @Parameter(required = true) String phoneNumber,
                                                              @Parameter(required = true) Integer channelId,
                                                              @Parameter(required = false) Integer type) {
        if (type == null || (type != 0 && type != 1 && type != 3)) {
            type = 0;
        }
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        result.onTimeout(()->{
            log.info("[JT-点播等待超时] phoneNumber：{}, channelId：{}, ", phoneNumber, channelId);
            // 释放rtpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("超时");
            result.setResult(wvpResult);
            jt1078PlayService.stopPlay(phoneNumber, channelId);
        });

        jt1078PlayService.play(phoneNumber, channelId, type, wvpResult -> {
            WVPResult<StreamContent> wvpResultForFinish = new WVPResult<>();
            wvpResultForFinish.setCode(wvpResult.getCode());
            wvpResultForFinish.setMsg(wvpResult.getMsg());
            if (wvpResult.getCode() == InviteErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo  = wvpResult.getData();

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
                    wvpResultForFinish.setData(new StreamContent(streamInfo));
                }
            }
            result.setResult(wvpResultForFinish);
        });

        return result;
    }

    @Operation(summary = "JT-结束点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/live/stop")
    public void stopLive(HttpServletRequest request,
                                                              @Parameter(required = true) String phoneNumber,
                                                              @Parameter(required = true) Integer channelId) {
        jt1078PlayService.stopPlay(phoneNumber, channelId);
    }

    @Operation(summary = "JT-语音对讲", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "app", description = "推流应用名", required = true)
    @Parameter(name = "stream", description = "推流ID", required = true)
    @Parameter(name = "mediaServerId", description = "流媒体ID", required = true)
    @Parameter(name = "onlySend", description = "是否只发送", required = false)
    @GetMapping("/talk/start")
    public DeferredResult<WVPResult<StreamContent>> startTalk(HttpServletRequest request,
                         @Parameter(required = true) String phoneNumber,
                         @Parameter(required = true) Integer channelId,
                         @Parameter(required = true) String app,
                         @Parameter(required = true) String stream,
                         @Parameter(required = true) String mediaServerId,
                         @Parameter(required = false) Boolean onlySend) {
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        result.onTimeout(()->{
            log.info("[JT-语音对讲超时] phoneNumber：{}, channelId：{}, ", phoneNumber, channelId);
            // 释放rtpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("超时");
            result.setResult(wvpResult);
            jt1078PlayService.stopPlay(phoneNumber, channelId);
        });

        jt1078PlayService.startTalk(phoneNumber, channelId, app, stream, mediaServerId, onlySend, wvpResult -> {
            WVPResult<StreamContent> wvpResultForFinish = new WVPResult<>();
            wvpResultForFinish.setCode(wvpResult.getCode());
            wvpResultForFinish.setMsg(wvpResult.getMsg());
            if (wvpResult.getCode() == InviteErrorCode.SUCCESS.getCode()) {

                if (wvpResult.getData() != null) {
                    StreamInfo streamInfo = wvpResult.getData();
                    if (userSetting.getUseSourceIpAsStreamIp()) {
                        streamInfo = wvpResult.getData().clone();//深拷贝
                        String host;
                        try {
                            URL url=new URL(request.getRequestURL().toString());
                            host=url.getHost();
                        } catch (MalformedURLException e) {
                            host=request.getLocalAddr();
                        }
                        streamInfo.changeStreamIp(host);
                    }
                    wvpResultForFinish.setData(new StreamContent(streamInfo));
                }
            }
            result.setResult(wvpResultForFinish);
        });

        return result;
    }

    @Operation(summary = "JT-结束对讲", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/talk/stop")
    public void stopTalk(HttpServletRequest request,
                         @Parameter(required = true) String phoneNumber,
                         @Parameter(required = true) Integer channelId) {
        jt1078PlayService.stopTalk(phoneNumber, channelId);
    }


    @Operation(summary = "JT-暂停点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/live/pause")
    public void pauseLive(HttpServletRequest request,
                         @Parameter(required = true) String phoneNumber,
                         @Parameter(required = true) Integer channelId) {
        jt1078PlayService.pausePlay(phoneNumber, channelId);
    }

    @Operation(summary = "JT-继续点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/live/continue")
    public void continueLive(HttpServletRequest request,
                          @Parameter(required = true) String phoneNumber,
                          @Parameter(required = true) Integer channelId) {

        jt1078PlayService.continueLivePlay(phoneNumber, channelId);
    }

    @Operation(summary = "JT-切换码流类型", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "streamType", description = "0:主码流; 1:子码流", required = true)
    @GetMapping("/live/switch")
    public void changeStreamType(HttpServletRequest request,
                             @Parameter(required = true) String phoneNumber,
                             @Parameter(required = true) Integer channelId,
                             @Parameter(required = true) Integer streamType) {
        service.changeStreamType(phoneNumber, channelId, streamType);
    }

    @Operation(summary = "JT-录像-查询资源列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "startTime", description = "开始时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "endTime", description = "结束时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @GetMapping("/record/list")
    public WVPResult<List<J1205.JRecordItem>> playbackList(HttpServletRequest request,
                                                                     @Parameter(required = true) String phoneNumber,
                                                                     @Parameter(required = true) Integer channelId,
                                                                     @Parameter(required = true) String startTime,
                                                                     @Parameter(required = true) String endTime
    ) {
        List<J1205.JRecordItem> recordList = jt1078PlayService.getRecordList(phoneNumber, channelId, startTime, endTime);
        if (recordList == null) {
            return WVPResult.fail(ErrorCode.ERROR100);
        }else {
            return WVPResult.success(recordList);
        }
    }
    @Operation(summary = "JT-录像-开始回放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "startTime", description = "开始时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "endTime", description = "结束时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "type", description = "0.音视频 1.音频 2.视频 3.视频或音视频", required = true)
    @Parameter(name = "rate", description = "0.所有码流 1.主码流 2.子码流(如果此通道只传输音频,此字段置0)", required = true)
    @Parameter(name = "playbackType", description = "0.正常回放 1.快进回放 2.关键帧快退回放 3.关键帧播放 4.单帧上传", required = true)
    @Parameter(name = "playbackSpeed", description = "0.无效 1.1倍 2.2倍 3.4倍 4.8倍 5.16倍 (回放控制为1和2时,此字段内容有效,否则置0)", required = true)
    @GetMapping("/playback/start")
    public DeferredResult<WVPResult<StreamContent>> recordLive(HttpServletRequest request,
                                                              @Parameter(required = true) String phoneNumber,
                                                              @Parameter(required = true) Integer channelId,
                                                              @Parameter(required = true) String startTime,
                                                              @Parameter(required = true) String endTime,
                                                              @Parameter(required = false) Integer type,
                                                              @Parameter(required = false) Integer rate,
                                                              @Parameter(required = false) Integer playbackType,
                                                              @Parameter(required = false) Integer playbackSpeed

    ) {
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        result.onTimeout(()->{
            log.info("[JT-回放-等待超时] phoneNumber：{}, channelId：{}, ", phoneNumber, channelId);
            // 释放rtpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("回放超时");
            result.setResult(wvpResult);
            jt1078PlayService.stopPlay(phoneNumber, channelId);
        });

        jt1078PlayService.playback(phoneNumber, channelId, startTime, endTime,type, rate, playbackType, playbackSpeed,  wvpResult -> {
            WVPResult<StreamContent> wvpResultForFinish = new WVPResult<>();
            wvpResultForFinish.setCode(wvpResult.getCode());
            wvpResultForFinish.setMsg(wvpResult.getMsg());
            if (wvpResult.getCode() == InviteErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo  = wvpResult.getData();
                if (streamInfo != null) {
                    if (userSetting.getUseSourceIpAsStreamIp()) {
                        String host;
                        try {
                            URL url=new URL(request.getRequestURL().toString());
                            host=url.getHost();
                        } catch (MalformedURLException e) {
                            host=request.getLocalAddr();
                        }
                        streamInfo.changeStreamIp(host);
                    }
                    wvpResultForFinish.setData(new StreamContent(streamInfo));
                }
            }
            result.setResult(wvpResultForFinish);
        });

        return result;
    }

    @Operation(summary = "JT-录像-回放控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "command", description = "0:开始回放; 1:暂停回放; 2:结束回放; 3:快进回放; 4:关键帧快退回放; 5:拖动回放; 6:关键帧播放", required = true)
    @Parameter(name = "playbackSpeed", description = "0.无效 1.1倍 2.2倍 3.4倍 4.8倍 5.16倍 (回放控制为3和4时,此字段内容有效,否则置0)", required = false)
    @Parameter(name = "time", description = "拖动回放位置(时间)", required = false)
    @GetMapping("/playback/control")
    public void recordControl(@Parameter(required = true) String phoneNumber,
                              @Parameter(required = true) Integer channelId,
                              @Parameter(required = false) Integer command,
                              @Parameter(required = false) String time,
                              @Parameter(required = false) Integer playbackSpeed

    ) {
        jt1078PlayService.playbackControl(phoneNumber, channelId, command, playbackSpeed,time);
    }

    @Operation(summary = "JT-录像-结束回放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/playback/stop")
    public void stopPlayback(HttpServletRequest request,
                         @Parameter(required = true) String phoneNumber,
                         @Parameter(required = true) Integer channelId) {
        jt1078PlayService.stopPlayback(phoneNumber, channelId);
    }

    @Operation(summary = "JT-录像-获取下载地址", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "startTime", description = "开始时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "endTime", description = "结束时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "alarmSign", description = "报警标志", required = true)
    @Parameter(name = "mediaType", description = "音视频资源类型： 0.音视频 1.音频 2.视频 3.视频或音视频", required = true)
    @Parameter(name = "streamType", description = "码流类型：0.所有码流 1.主码流 2.子码流(如果此通道只传输音频,此字段置0)", required = true)
    @Parameter(name = "storageType", description = "存储器类型", required = true)
    @GetMapping("/playback/downloadUrl")
    public String getRecordTempUrl(HttpServletRequest request,
                                   @Parameter(required = true) String phoneNumber,
                                   @Parameter(required = true) Integer channelId,
                                   @Parameter(required = true) String startTime,
                                   @Parameter(required = true) String endTime,
                                   @Parameter(required = false) Integer alarmSign,
                                   @Parameter(required = false) Integer mediaType,
                                   @Parameter(required = false) Integer streamType,
                                   @Parameter(required = false) Integer storageType

    ){
        log.info("[JT-录像] 下载，设备:{}， 通道： {}， 开始时间： {}， 结束时间： {}，报警标志: {}, 音视频类型： {}， 码流类型： {}，存储器类型： {}， ",
                phoneNumber, channelId, startTime, endTime, alarmSign, mediaType, streamType, storageType);
        if (!ftpSetting.getEnable()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未启用ftp服务，无法下载录像");
        }
        return service.getRecordTempUrl(phoneNumber, channelId, startTime, endTime, alarmSign, mediaType, streamType, storageType);
    }

    @Operation(summary = "JT-录像-下载", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "path", description = "临时下载路径", required = true)
    @GetMapping("/playback/download")
    public void download(HttpServletRequest request, HttpServletResponse response, @Parameter(required = true) String path) throws IOException {
        if (!ftpSetting.getEnable()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未启用ftp服务，无法下载录像");
        }
        DeferredResult<String> result = new DeferredResult<>();
        ServletOutputStream outputStream = response.getOutputStream();
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(path + ".mp4", "UTF-8"));
//        response.setContentLength(394983300);
        response.setStatus(HttpServletResponse.SC_OK);
        service.recordDownload(path, outputStream);
    }

    @Operation(summary = "JT-云台控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "command", description = "控制指令,允许值: left, right, up, down, zoomin, zoomout, irisin, irisout, focusnear, focusfar, stop", required = true)
    @Parameter(name = "speed", description = "速度(0-255)， command,值 left, right, up, down时有效", required = true)
    @GetMapping("/ptz")
    public void ptz(String phoneNumber, Integer channelId, String command, int speed){

        log.info("[JT-云台控制] phoneNumber：{}, channelId：{}, command: {}, speed: {}", phoneNumber, channelId, command, speed);
        service.ptzControl(phoneNumber, channelId, command, speed);
    }

    @Operation(summary = "JT-补光灯开关", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "command", description = "控制指令,允许值: on off", required = true)
    @GetMapping("/fill-light")
    public void fillLight(String phoneNumber, Integer channelId, String command){

        log.info("[JT-补光灯开关] phoneNumber：{}, channelId：{}, command: {}", phoneNumber, channelId, command);
        service.supplementaryLight(phoneNumber, channelId, command);
    }

    @Operation(summary = "JT-雨刷开关", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "command", description = "控制指令,允许值: on off", required = true)
    @GetMapping("/wiper")
    public void wiper(String phoneNumber, Integer channelId, String command){

        log.info("[JT-雨刷开关] phoneNumber：{}, channelId：{}, command: {}", phoneNumber, channelId, command);
        service.wiper(phoneNumber, channelId, command);
    }

    @Operation(summary = "JT-查询终端参数", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @GetMapping("/config")
    public JTDeviceConfig config(String phoneNumber, String[] params){

        log.info("[JT-查询终端参数] phoneNumber：{}", phoneNumber);
        return service.queryConfig(phoneNumber, params, null);
    }

    @Operation(summary = "JT-设置终端参数", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @Parameter(name = "config", description = "终端参数", required = true)
    @PostMapping("/set-config")
    public void setConfig(@RequestBody SetConfigParam config){

        log.info("[JT-设置终端参数] 参数: {}", config.toString());
        service.setConfig(config.getPhoneNumber(), config.getConfig());
    }

    @Operation(summary = "终端控制-连接指定的服务器", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "control", description = "终端控制参数", required = true)
    @PostMapping("/control/connection")
    public void connectionControl(@RequestBody ConnectionControlParam control){

        log.info("[JT-终端控制] 参数: {}", control.toString());
        service.connectionControl(control.getPhoneNumber(), control.getControl());
    }

    @Operation(summary = "JT-终端控制-复位", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @PostMapping("/control/reset")
    public void resetControl(String phoneNumber){

        log.info("[JT-复位] phoneNumber: {}", phoneNumber);
        service.resetControl(phoneNumber);
    }

    @Operation(summary = "JT-终端控制-恢复出厂设置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @PostMapping("/control/factory-reset")
    public void factoryResetControl(String phoneNumber){

        log.info("[JT-恢复出厂设置] phoneNumber: {}", phoneNumber);
        service.factoryResetControl(phoneNumber);
    }

    @Operation(summary = "JT-查询终端属性", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @GetMapping("/attribute")
    public JTDeviceAttribute attribute(String phoneNumber){

        log.info("[JT-查询终端属性] phoneNumber: {}", phoneNumber);
        return service.attribute(phoneNumber);
    }

    @Operation(summary = "JT-查询位置信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @GetMapping("/position-info")
    public JTPositionBaseInfo queryPositionInfo(String phoneNumber){

        log.info("[JT-查询位置信息] phoneNumber: {}", phoneNumber);
        return service.queryPositionInfo(phoneNumber);
    }

    @Operation(summary = "JT-临时位置跟踪控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @Parameter(name = "timeInterval", description = "时间间隔,单位为秒,时间间隔为0 时停止跟踪,停止跟踪无需带后继字段", required = true)
    @Parameter(name = "validityPeriod", description = "位置跟踪有效期, 单位为秒,终端在接收到位置跟踪控制消息后,在有效期截止时间之前依据消息中的时间间隔发送位置汇报", required = true)
    @GetMapping("/control/temp-position-tracking")
    public void tempPositionTrackingControl(String phoneNumber, Integer timeInterval, Long validityPeriod){

        log.info("[JT-临时位置跟踪控制] phoneNumber: {}, 时间间隔 {}秒, 位置跟踪有效期 {}秒", phoneNumber, timeInterval, validityPeriod);
        service.tempPositionTrackingControl(phoneNumber, timeInterval, validityPeriod);
    }

    @Operation(summary = "JT-人工确认报警消息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @Parameter(name = "timeInterval", description = "时间间隔,单位为秒,时间间隔为0 时停止跟踪,停止跟踪无需带后继字段", required = true)
    @Parameter(name = "validityPeriod", description = "位置跟踪有效期, 单位为秒,终端在接收到位置跟踪控制消息后,在有效期截止时间之前依据消息中的时间间隔发送位置汇报", required = true)
    @PostMapping("/confirmation-alarm-message")
    public void confirmationAlarmMessage(@RequestBody ConfirmationAlarmMessageParam param){

        log.info("[JT-人工确认报警消息] 参数: {}", param);
        service.confirmationAlarmMessage(param.getPhoneNumber(), param.getAlarmPackageNo(), param.getAlarmMessageType());
    }

    @Operation(summary = "JT-链路检测", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @GetMapping("/link-detection")
    public Integer linkDetection(String phoneNumber){

        log.info("[JT-链路检测] phoneNumber: {}", phoneNumber);
        return service.linkDetection(phoneNumber);
    }

    @Operation(summary = "JT-文本信息下发", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "textMessageParam", description = "文本信息下发参数", required = true)
    @PostMapping("/text-msg")
    public WVPResult<Integer> textMessage(@RequestBody TextMessageParam textMessageParam){

        log.info("[JT-文本信息下发] textMessageParam: {}", textMessageParam);
        int result = service.textMessage(textMessageParam.getPhoneNumber(), textMessageParam.getSign(), textMessageParam.getTextType(), textMessageParam.getContent());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-电话回拨", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @Parameter(name = "sign", description = "标志: 0:普通通话,1:监听", required = true)
    @Parameter(name = "destPhoneNumber", description = "回拨电话号码", required = true)
    @GetMapping("/telephone-callback")
    public WVPResult<Integer> telephoneCallback(String phoneNumber, Integer sign, String destPhoneNumber){

        log.info("[JT-电话回拨] phoneNumber: {}, sign: {}, phoneNumber: {},", phoneNumber, sign, phoneNumber);
        int result = service.telephoneCallback(phoneNumber, sign, destPhoneNumber);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-设置电话本", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "setPhoneBookParam", description = "设置电话本参数", required = true)
    @PostMapping("/set-phone-book")
    public WVPResult<Integer> setPhoneBook(@RequestBody SetPhoneBookParam setPhoneBookParam){

        log.info("[JT-设置电话本] setPhoneBookParam: {}", setPhoneBookParam);
        int result = service.setPhoneBook(setPhoneBookParam.getPhoneNumber(), setPhoneBookParam.getType(), setPhoneBookParam.getPhoneBookContactList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-车门控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @Parameter(name = "open", description = "开启车门", required = true)
    @GetMapping("/control/door")
    public WVPResult<Integer> controlDoor(String phoneNumber, Boolean open){

        log.info("[JT-车门控制] phoneNumber: {}, open: {},", phoneNumber, open);
        JTPositionBaseInfo positionBaseInfo = service.controlDoor(phoneNumber, open);
        if (positionBaseInfo == null || positionBaseInfo.getStatus() == null) {
            return WVPResult.fail(ErrorCode.ERROR100.getCode(), "控制失败");
        }
        if (open == !positionBaseInfo.getStatus().isDoorLocking()) {
            return WVPResult.success(null);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100.getCode(), "控制失败");
        }
    }

    @Operation(summary = "JT-更新圆形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/circle/update")
    public WVPResult<Integer> updateAreaForCircle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-更新圆形区域] areaParam: {},", areaParam);
        int result = service.setAreaForCircle(0, areaParam.getPhoneNumber(), areaParam.getCircleAreaList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-追加圆形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/circle/add")
    public WVPResult<Integer> addAreaForCircle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-追加圆形区域] areaParam: {},", areaParam);
        int result = service.setAreaForCircle(1, areaParam.getPhoneNumber(), areaParam.getCircleAreaList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-修改圆形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/circle/edit")
    public WVPResult<Integer> editAreaForCircle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-修改圆形区域] areaParam: {},", areaParam);
        int result = service.setAreaForCircle(2, areaParam.getPhoneNumber(), areaParam.getCircleAreaList());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-删除圆形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @Parameter(name = "ids", description = "待删除圆形区域的id，例如1,2,3", required = true)
    @GetMapping("/area/circle/delete")
    public WVPResult<Integer> deleteAreaForCircle(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-删除圆形区域] phoneNumber: {}, ids:{}", phoneNumber, ids);
        int result = service.deleteAreaForCircle(phoneNumber, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-查询圆形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @GetMapping("/area/circle/query")
    public WVPResult<List<JTAreaOrRoute>> queryAreaForCircle(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-查询圆形区域] phoneNumber: {}, ids:{}", phoneNumber, ids);
        List<JTAreaOrRoute> result = service.queryAreaForCircle(phoneNumber, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }


    @Operation(summary = "JT-更新矩形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/rectangle/update")
    public WVPResult<Integer> updateAreaForRectangle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-更新矩形区域] areaParam: {},", areaParam);
        int result = service.setAreaForRectangle(0, areaParam.getPhoneNumber(), areaParam.getRectangleAreas());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-追加矩形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/rectangle/add")
    public WVPResult<Integer> addAreaForRectangle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-追加矩形区域] areaParam: {},", areaParam);
        int result = service.setAreaForRectangle(1, areaParam.getPhoneNumber(), areaParam.getRectangleAreas());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-修改矩形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/rectangle/edit")
    public WVPResult<Integer> editAreaForRectangle(@RequestBody SetAreaParam areaParam){

        log.info("[JT-修改矩形区域] areaParam: {},", areaParam);
        int result = service.setAreaForRectangle(2, areaParam.getPhoneNumber(), areaParam.getRectangleAreas());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-删除矩形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @Parameter(name = "ids", description = "待删除圆形区域的id，例如1,2,3", required = true)
    @GetMapping("/area/rectangle/delete")
    public WVPResult<Integer> deleteAreaForRectangle(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-删除矩形区域] phoneNumber: {}, ids:{}", phoneNumber, ids);
        int result = service.deleteAreaForRectangle(phoneNumber, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-查询矩形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @GetMapping("/area/rectangle/query")
    public WVPResult<List<JTAreaOrRoute>> queryAreaForRectangle(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-查询矩形区域] phoneNumber: {}, ids:{}", phoneNumber, ids);
        List<JTAreaOrRoute> result = service.queryAreaForRectangle(phoneNumber, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "JT-设置多边形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/area/polygon/set")
    public WVPResult<Integer> setAreaForPolygon(@RequestBody SetAreaParam areaParam){

        log.info("[JT-设置多边形区域] areaParam: {},", areaParam);
        int result = service.setAreaForPolygon(areaParam.getPhoneNumber(), areaParam.getPolygonArea());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-删除多边形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @Parameter(name = "ids", description = "待删除圆形区域的id，例如1,2,3", required = true)
    @GetMapping("/area/polygon/delete")
    public WVPResult<Integer> deleteAreaForPolygon(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-删除多边形区域] phoneNumber: {}, ids:{}", phoneNumber, ids);
        int result = service.deleteAreaForPolygon(phoneNumber, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-查询多边形区域", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @GetMapping("/area/polygon/query")
    public WVPResult<List<JTAreaOrRoute>> queryAreaForPolygon(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-查询多边形区域] phoneNumber: {}, ids:{}", phoneNumber, ids);
        List<JTAreaOrRoute> result = service.queryAreaForPolygon(phoneNumber, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "JT-设置路线", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "areaParam", description = "设置区域参数", required = true)
    @PostMapping("/route/set")
    public WVPResult<Integer> setRoute(@RequestBody SetAreaParam areaParam){

        log.info("[JT-设置路线] areaParam: {},", areaParam);
        int result = service.setRoute(areaParam.getPhoneNumber(), areaParam.getRoute());
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-删除路线", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @Parameter(name = "ids", description = "待删除圆形区域的id，例如1,2,3", required = true)
    @GetMapping("/route/delete")
    public WVPResult<Integer> deleteRoute(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-删除路线] phoneNumber: {}, ids:{}", phoneNumber, ids);
        int result = service.deleteRoute(phoneNumber, ids);
        if (result == 0) {
            return WVPResult.success(result);
        }else {
            WVPResult<Integer> fail = WVPResult.fail(ErrorCode.ERROR100);
            fail.setData(result);
            return fail;
        }
    }

    @Operation(summary = "JT-查询路线", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @GetMapping("/route/query")
    public WVPResult<List<JTAreaOrRoute>> queryRoute(String phoneNumber, @RequestParam(value = "ids", required = false) List<Long> ids){

        log.info("[JT-查询路线] phoneNumber: {}, ids:{}", phoneNumber, ids);
        List<JTAreaOrRoute> result = service.queryRoute(phoneNumber, ids);
        if (result != null) {
            return WVPResult.success(result);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    // TODO 待实现 行驶记录数据采集命令 行驶记录数据上传 行驶记录参数下传命令 电子运单上报 CAN总线数据上传

    @Operation(summary = "JT-上报驾驶员身份信息请求", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @GetMapping("/driver-information")
    public WVPResult<JTDriverInformation> queryDriverInformation(String phoneNumber){

        log.info("[JT-上报驾驶员身份信息请求] phoneNumber: {}", phoneNumber);
        JTDriverInformation jtDriverInformation = service.queryDriverInformation(phoneNumber);
        if (jtDriverInformation != null) {
            return WVPResult.success(jtDriverInformation);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "JT-摄像头立即拍摄命令", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @PostMapping("/shooting")
    public WVPResult<List<Long>> shooting(@RequestBody ShootingParam param){

        log.info("[JT-摄像头立即拍摄命令] param: {}", param );
        List<Long> ids = service.shooting(param.getPhoneNumber(), param.getShootingCommand());
        if (ids != null) {
            return WVPResult.success(ids);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "JT-抓图", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @Parameter(name = "channelId", description = "通道编号", required = true)
    @GetMapping("/snap")
    public void snap(HttpServletResponse response, String phoneNumber, Integer channelId){

        log.info("[JT-抓图] 设备编号: {}, 通道编号: {}", phoneNumber, channelId );
        Assert.notNull(channelId, "缺少通道编号");
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
//            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(phoneNumber + "_" + channelId + ".jpg", "UTF-8"));
            service.snap(phoneNumber, channelId, outputStream);
        }catch (Exception e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "JT-存储多媒体数据检索", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "param", description = "存储多媒体数据参数", required = true)
    @PostMapping("/media/list")
    public WVPResult<List<JTMediaDataInfo>> queryMediaData(@RequestBody QueryMediaDataParam param){

        log.info("[JT-存储多媒体数据检索] param: {}", param );
        List<JTMediaDataInfo> ids = service.queryMediaData(param.getPhoneNumber(), param.getQueryMediaDataCommand());
        if (ids != null) {
            return WVPResult.success(ids);
        }else {
            return WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "JT-单条存储多媒体数据上传", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备编号", required = true)
    @Parameter(name = "mediaId", description = "多媒体ID", required = true)
    @GetMapping("/media/upload/one")
    public void uploadOneMedia(HttpServletResponse response, String phoneNumber, Long mediaId){

        log.info("[JT-单条存储多媒体数据上传] 设备编号: {}, 多媒体ID: {}", phoneNumber, mediaId );
        Assert.notNull(mediaId, "缺少通道编号");
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            service.uploadOneMedia(phoneNumber, mediaId, outputStream);
        }catch (Exception e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
        }
    }

//    @Operation(summary = "JT-存储多媒体数据上传命令", security = @SecurityRequirement(name = JwtUtils.HEADER))
//    @Parameter(name = "param", description = "存储多媒体数据参数", required = true)
//    @PostMapping("/media-data-upload")
//    public DeferredResult<WVPResult<List<String>>> updateMediaData(@RequestBody QueryMediaDataParam param){
//
//        log.info("[JT-存储多媒体数据上传命令] param: {}", param );
//        DeferredResult<WVPResult<List<String>>> deferredResult = new DeferredResult<>(30000L);
//        List<String> resultList = new ArrayList<>();
//
//        deferredResult.onTimeout(()->{
//            log.info("[JT-存储多媒体数据上传命令超时] param: {}", param );
//            WVPResult<List<String>> fail = WVPResult.fail(ErrorCode.ERROR100);
//            fail.setMsg("超时");
//            fail.setData(resultList);
//            deferredResult.setResult(fail);
//        });
//        List<JTMediaDataInfo> ids;
//        if (param.getMediaId() != null) {
//            ids = new ArrayList<>();
//            JTMediaDataInfo mediaDataInfo = new JTMediaDataInfo();
//            mediaDataInfo.setId(param.getMediaId());
//            ids.add(mediaDataInfo);
//        }else {
//            ids = service.queryMediaData(param.getPhoneNumber(), param.getQueryMediaDataCommand());
//        }
//        if (ids.isEmpty()) {
//            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100));
//            return deferredResult;
//        }
//        Map<String, JTMediaDataInfo> idMap= new HashMap<>();
//        for (JTMediaDataInfo mediaDataInfo : ids) {
//            idMap.put(mediaDataInfo.getId() + ".jpg", mediaDataInfo);
//        }
//        // 开启文件监听
//        FileAlterationObserver observer = new FileAlterationObserver(new File("mediaEvent"));
//        observer.addListener(new FileAlterationListenerAdaptor() {
//            @Override
//            public void onFileCreate(File file) {
//               if (idMap.containsKey(file.getName())) {
//                   idMap.remove(file.getName());
//                   resultList.add("mediaEvent" + File.separator + file.getName());
//                   if (idMap.isEmpty()) {
//                       deferredResult.setResult(WVPResult.success(resultList));
//                   }
//               }
//            }
//        });
//        FileAlterationMonitor monitor = new FileAlterationMonitor(5, observer);
//        try {
//            monitor.start();
//        } catch (Exception e) {
//            log.info("[JT-存储多媒体数据上传命令监听文件失败] param: {}", param );
//            deferredResult.setResult(null);
//            return deferredResult;
//        }
//        taskExecutor.execute(()->{
//            if (param.getMediaId() != null) {
//                service.uploadMediaDataForSingle(param.getPhoneNumber(), param.getMediaId(), param.getDelete());
//            }else {
//                service.uploadMediaData(param.getPhoneNumber(), param.getQueryMediaDataCommand());
//            }
//
//        });
//        return deferredResult;
//    }

    @Operation(summary = "JT-开始录音", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "time", description = "录音时间，单位为秒(s) ,0 表示一直录音", required = false)
    @Parameter(name = "save", description = "0:实时上传；1:保存", required = false)
    @Parameter(name = "samplingRate", description = "音频采样率， 0:8K；1:11K；2:23K；3:32K", required = false)
    @GetMapping("/record/start")
    public void startRecord(HttpServletRequest request,
                         @Parameter(required = true) String phoneNumber,
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
        service.record(phoneNumber, 1, time, save, samplingRate);
    }

    @Operation(summary = "JT-停止录音", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @Parameter(name = "time", description = "录音时间，单位为秒(s) ,0 表示一直录音", required = false)
    @Parameter(name = "save", description = "0:实时上传；1:保存", required = false)
    @Parameter(name = "samplingRate", description = "音频采样率， 0:8K；1:11K；2:23K；3:32K", required = false)
    @GetMapping("/record/stop")
    public void stopRecord(HttpServletRequest request,
                            @Parameter(required = true) String phoneNumber,
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
        service.record(phoneNumber, 0, time, save, samplingRate);
    }

    @Operation(summary = "JT-查询终端音视频属性", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "phoneNumber", description = "设备手机号", required = true)
    @GetMapping("/media/attribute")
    public JTMediaAttribute queryMediaAttribute(HttpServletRequest request,
                           @Parameter(required = true) String phoneNumber
    ) {
        return service.queryMediaAttribute(phoneNumber);
    }

    // TODO 视频报警上报


}

