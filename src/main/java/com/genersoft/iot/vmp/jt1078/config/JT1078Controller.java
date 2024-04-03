package com.genersoft.iot.vmp.jt1078.config;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.proc.response.*;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.vmanager.gb28181.play.PlayController;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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

    /**
     * jt1078Template 调用示例
     */
    @GetMapping("/start/live/{deviceId}/{channelId}")
    public DeferredResult<WVPResult<StreamContent>> startLive(HttpServletRequest request,  @PathVariable String deviceId, @PathVariable String channelId) {
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

        result.onTimeout(()->{
            logger.info("[1078-点播等待超时] deviceId：{}, channelId：{}, ", deviceId, channelId);
            // 释放rtpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("点播超时");
            result.setResult(wvpResult);
            service.stopPlay(deviceId, channelId);
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

