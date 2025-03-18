package com.genersoft.iot.vmp.streamProxy.controller;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxyParam;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SuppressWarnings("rawtypes")
/**
 * 拉流代理接口
 */
@Tag(name = "拉流代理", description = "")
@RestController
@Slf4j
@RequestMapping(value = "/api/proxy")
public class StreamProxyController {

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private IStreamProxyPlayService streamProxyPlayService;

    @Autowired
    private UserSetting userSetting;


    @Operation(summary = "分页查询流代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "pulling", description = "是否正在拉流")
    @Parameter(name = "mediaServerId", description = "流媒体ID")
    @GetMapping(value = "/list")
    @ResponseBody
    public PageInfo<StreamProxy> list(@RequestParam(required = false)Integer page,
                                      @RequestParam(required = false)Integer count,
                                      @RequestParam(required = false)String query,
                                      @RequestParam(required = false)Boolean pulling,
                                      @RequestParam(required = false)String mediaServerId){

        if (ObjectUtils.isEmpty(mediaServerId)) {
            mediaServerId = null;
        }
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        return streamProxyService.getAll(page, count, query, pulling, mediaServerId);
    }

    @Operation(summary = "查询流代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名")
    @Parameter(name = "stream", description = "流Id")
    @GetMapping(value = "/one")
    @ResponseBody
    public StreamProxy one(String app, String stream){

        return streamProxyService.getStreamProxyByAppAndStream(app, stream);
    }

    @Operation(summary = "保存代理(已存在会覆盖)", security = @SecurityRequirement(name = JwtUtils.HEADER), parameters = {
            @Parameter(name = "param", description = "代理参数", required = true),
    })
    @PostMapping(value = "/save")
    @ResponseBody
    public StreamContent save(@RequestBody StreamProxyParam param){
        log.info("添加代理： " + JSONObject.toJSONString(param));
        if (ObjectUtils.isEmpty(param.getMediaServerId())) {
            param.setMediaServerId("auto");
        }
        if (ObjectUtils.isEmpty(param.getType())) {
            param.setType("default");
        }

        StreamInfo streamInfo =  streamProxyService.save(param);
        if (param.isEnable()) {
            if (streamInfo == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg());
            }else {
                return new StreamContent(streamInfo);
            }
        }else {
            return null;
        }

    }

    @Operation(summary = "新增代理", security = @SecurityRequirement(name = JwtUtils.HEADER), parameters = {
            @Parameter(name = "param", description = "代理参数", required = true),
    })
    @PostMapping(value = "/add")
    @ResponseBody
    public StreamProxy add(@RequestBody StreamProxy param){
        log.info("添加代理： " + JSONObject.toJSONString(param));
        if (ObjectUtils.isEmpty(param.getRelatesMediaServerId())) {
            param.setRelatesMediaServerId(null);
        }
        if (ObjectUtils.isEmpty(param.getType())) {
            param.setType("default");
        }
        if (ObjectUtils.isEmpty(param.getGbId())) {
            param.setGbDeviceId(null);
        }
        param.setServerId(userSetting.getServerId());
        streamProxyService.add(param);
        return param;
    }

    @Operation(summary = "更新代理", security = @SecurityRequirement(name = JwtUtils.HEADER), parameters = {
            @Parameter(name = "param", description = "代理参数", required = true),
    })
    @PostMapping(value = "/update")
    @ResponseBody
    public StreamProxy update(@RequestBody StreamProxy param){
        log.info("更新代理： " + JSONObject.toJSONString(param));
        if (param.getId() == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "缺少代理信息的ID");
        }
        if (ObjectUtils.isEmpty(param.getRelatesMediaServerId())) {
            param.setRelatesMediaServerId(null);
        }
        if (ObjectUtils.isEmpty(param.getGbId())) {
            param.setGbDeviceId(null);
        }
        streamProxyService.update(param);
        return param;
    }

    @GetMapping(value = "/ffmpeg_cmd/list")
    @ResponseBody
    @Operation(summary = "获取ffmpeg.cmd模板", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "mediaServerId", description = "流媒体ID", required = true)
    public Map<String, String> getFFmpegCMDs(@RequestParam String mediaServerId){
        log.debug("获取节点[ {} ]ffmpeg.cmd模板", mediaServerId );

        MediaServer mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "流媒体： " + mediaServerId + "未找到");
        }
        return streamProxyService.getFFmpegCMDs(mediaServerItem);
    }

    @DeleteMapping(value = "/del")
    @ResponseBody
    @Operation(summary = "移除代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    public void del(@RequestParam String app, @RequestParam String stream){
        log.info("移除代理： " + app + "/" + stream);
        if (app == null || stream == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), app == null ?"app不能为null":"stream不能为null");
        }else {
            streamProxyService.delteByAppAndStream(app, stream);
        }
    }

    @DeleteMapping(value = "/delete")
    @ResponseBody
    @Operation(summary = "移除代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "代理ID", required = true)
    public void delte(int id){
        log.info("移除代理： {}", id);
        streamProxyService.delete(id);
    }

    @GetMapping(value = "/start")
    @ResponseBody
    @Operation(summary = "启用代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "代理Id", required = true)
    public StreamContent start(int id){
        log.info("播放代理： {}", id);
        StreamInfo streamInfo = streamProxyPlayService.start(id, null, null);
        if (streamInfo == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg());
        }else {
            return new StreamContent(streamInfo);
        }
    }

    @GetMapping(value = "/stop")
    @ResponseBody
    @Operation(summary = "停用代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "代理Id", required = true)
    public void stop(int id){
        log.info("停用代理： {}", id);
        streamProxyPlayService.stop(id);
    }
}
