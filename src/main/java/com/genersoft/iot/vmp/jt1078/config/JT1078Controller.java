package com.genersoft.iot.vmp.jt1078.config;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * curl http://localhost:18080/api/jt1078/start/live/18864197066/1
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:12
 * @email qingtaij@163.com
 */
@ConditionalOnProperty(value = "jt1078.enable", havingValue = "true")
@RestController
@RequestMapping("/api/jt1078")
public class JT1078Controller {

    private final static Logger logger = LoggerFactory.getLogger(JT1078Controller.class);

    @Resource
    Ijt1078Service service;

    @Autowired
    UserSetting userSetting;

    @Operation(summary = "1078-开始点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/live/start")
    public DeferredResult<WVPResult<StreamContent>> startLive(HttpServletRequest request,
                                                              @Parameter(required = true) String deviceId,
                                                              @Parameter(required = false) String channelId) {
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        String finalChannelId = channelId;
        result.onTimeout(()->{
            logger.info("[1078-点播等待超时] deviceId：{}, channelId：{}, ", deviceId, finalChannelId);
            // 释放rtpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("点播超时");
            result.setResult(wvpResult);
            service.stopPlay(deviceId, finalChannelId);
        });

        service.play(deviceId, channelId, (code, msg, streamInfo) -> {
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
                                                              @Parameter(required = false) String channelId) {
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        service.stopPlay(deviceId, channelId);
    }

    @Operation(summary = "1078-暂停点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/live/pause")
    public void pauseLive(HttpServletRequest request,
                         @Parameter(required = true) String deviceId,
                         @Parameter(required = false) String channelId) {
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        service.pausePlay(deviceId, channelId);
    }

    @Operation(summary = "1078-继续点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @GetMapping("/live/continue")
    public void continueLive(HttpServletRequest request,
                          @Parameter(required = true) String deviceId,
                          @Parameter(required = false) String channelId) {
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        service.continueLivePlay(deviceId, channelId);
    }

    @Operation(summary = "1078-回放-查询资源列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号, 一般为从1开始的数字", required = true)
    @Parameter(name = "startTime", description = "开始时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @Parameter(name = "endTime", description = "结束时间,格式： yyyy-MM-dd HH:mm:ss", required = true)
    @GetMapping("/record/list")
    public void playbackList(HttpServletRequest request,
                             @Parameter(required = true) String deviceId,
                             @Parameter(required = false) String channelId,
                             @Parameter(required = true) String startTime,
                             @Parameter(required = true) String endTime
    ) {
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = "1";
        }
        service.getRecordList(deviceId, channelId, startTime, endTime);
    }

    @Operation(summary = "分页查询部标设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
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



    @Operation(summary = "新增设备", security = @SecurityRequirement(name = JwtUtils.HEADER))
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

}

