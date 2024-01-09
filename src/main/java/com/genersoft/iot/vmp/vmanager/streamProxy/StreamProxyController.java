package com.genersoft.iot.vmp.vmanager.streamProxy;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxy;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.UUID;

@SuppressWarnings("rawtypes")
/**
 * 拉流代理接口
 */
@Tag(name = "拉流代理", description = "")
@Controller

@RequestMapping(value = "/api/proxy")
public class StreamProxyController {

    private final static Logger logger = LoggerFactory.getLogger(StreamProxyController.class);

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private UserSetting userSetting;


    @Operation(summary = "分页查询流代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @GetMapping(value = "/list")
    @ResponseBody
    public PageInfo<StreamProxy> list(@RequestParam(required = false)Integer page,
                                      @RequestParam(required = false)Integer count,
                                      @RequestParam(required = false)String query,
                                      @RequestParam(required = false)Boolean online ){

        return streamProxyService.getAll(page, count);
    }

    @Operation(summary = "查询流代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名")
    @Parameter(name = "stream", description = "流Id")
    @GetMapping(value = "/one")
    @ResponseBody
    public StreamProxy one(String app, String stream){

        return streamProxyService.getStreamProxyByAppAndStream(app, stream);
    }

    @Operation(summary = "保存代理", security = @SecurityRequirement(name = JwtUtils.HEADER), parameters = {
            @Parameter(name = "param", description = "代理参数", required = true),
    })
    @PostMapping(value = "/save")
    @ResponseBody
    public DeferredResult<Object> save(@RequestBody StreamProxy param){
        logger.info("添加代理： " + JSONObject.toJSONString(param));
        if (ObjectUtils.isEmpty(param.getMediaServerId())) {
            param.setMediaServerId("auto");
        }
        if (ObjectUtils.isEmpty(param.getType())) {
            param.setType("default");
        }
        if (ObjectUtils.isEmpty(param.getRtpType())) {
            param.setRtpType("1");
        }
        if (ObjectUtils.isEmpty(param.getGbId())) {
            param.setGbId(null);
        }
        StreamProxy streamProxyItem = streamProxyService.getStreamProxyByAppAndStream(param.getApp(), param.getStream());
        if (streamProxyItem  != null) {
            streamProxyService.del(param.getApp(), param.getStream());
        }

        DeferredResult<Object> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        // 录像查询以channelId作为deviceId查询
        result.onTimeout(()->{
            WVPResult<StreamInfo> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("超时");
            result.setResult(wvpResult);
        });

        streamProxyService.save(param, (code, msg, streamInfo) -> {
            logger.info("[拉流代理] {}", code == ErrorCode.SUCCESS.getCode()? "成功":"失败： " + msg);
            if (code == ErrorCode.SUCCESS.getCode()) {
                result.setResult(new StreamContent(streamInfo));
            }else {
                result.setResult(WVPResult.fail(code, msg));
            }
        });
        return result;
    }

    @Operation(summary = "添加代理", security = @SecurityRequirement(name = JwtUtils.HEADER), parameters = {
            @Parameter(name = "param", description = "代理参数", required = true),
    })
    @PostMapping(value = "/add")
    @ResponseBody
    public DeferredResult<Object> add(@RequestBody StreamProxy param){
        logger.info("添加代理： " + JSONObject.toJSONString(param));
        if (ObjectUtils.isEmpty(param.getMediaServerId())) {
            param.setMediaServerId("auto");
        }
        if (ObjectUtils.isEmpty(param.getType())) {
            param.setType("default");
        }
        if (ObjectUtils.isEmpty(param.getRtpType())) {
            param.setRtpType("1");
        }
        if (ObjectUtils.isEmpty(param.getGbId())) {
            param.setGbId(null);
        }
        if (ObjectUtils.isEmpty(param.getSrcUrl())) {
            param.setSrcUrl(param.getUrl());
        }else {
            param.setUrl(param.getSrcUrl());
        }

        DeferredResult<Object> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        // 录像查询以channelId作为deviceId查询
        result.onTimeout(()->{
            WVPResult<StreamInfo> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("超时");
            result.setResult(wvpResult);
        });

        streamProxyService.add(param, (code, msg, streamInfo) -> {
            logger.info("[添加拉流代理] {}", code == ErrorCode.SUCCESS.getCode()? "成功":"失败： " + msg);
            if (code == ErrorCode.SUCCESS.getCode()) {
                result.setResult(new StreamContent(streamInfo));
            }else {
                result.setResult(WVPResult.fail(code, msg));
            }
        });
        return result;
    }
    @Operation(summary = "编辑代理", security = @SecurityRequirement(name = JwtUtils.HEADER), parameters = {
            @Parameter(name = "param", description = "代理参数", required = true),
    })
    @PostMapping(value = "/edit")
    @ResponseBody
    public DeferredResult<Object> edit(@RequestBody StreamProxy param){
        logger.info("编辑代理： " + JSONObject.toJSONString(param));
        if (param.getId() <= 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "缺少主键ID");
        }
        if (ObjectUtils.isEmpty(param.getMediaServerId())) {
            param.setMediaServerId("auto");
        }
        if (ObjectUtils.isEmpty(param.getType())) {
            param.setType("default");
        }
        if (ObjectUtils.isEmpty(param.getRtpType())) {
            param.setRtpType("1");
        }
        if (ObjectUtils.isEmpty(param.getGbId())) {
            param.setGbId(null);
        }
        if (ObjectUtils.isEmpty(param.getSrcUrl())) {
            param.setSrcUrl(param.getUrl());
        }else {
            param.setUrl(param.getSrcUrl());
        }

        DeferredResult<Object> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        // 录像查询以channelId作为deviceId查询
        result.onTimeout(()->{
            WVPResult<StreamInfo> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("超时");
            result.setResult(wvpResult);
        });

        streamProxyService.edit(param, (code, msg, streamInfo) -> {
            logger.info("[添加拉流代理] {}", code == ErrorCode.SUCCESS.getCode()? "成功":"失败： " + msg);
            if (code == ErrorCode.SUCCESS.getCode()) {
                result.setResult(new StreamContent(streamInfo));
            }else {
                result.setResult(WVPResult.fail(code, msg));
            }
        });
        return result;
    }

    @GetMapping(value = "/ffmpeg_cmd/list")
    @ResponseBody
    @Operation(summary = "获取ffmpeg.cmd模板", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "mediaServerId", description = "流媒体ID", required = true)
    public JSONObject getFFmpegCMDs(@RequestParam String mediaServerId){
        logger.debug("获取节点[ {} ]ffmpeg.cmd模板", mediaServerId );

        MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
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
        logger.info("移除代理： " + app + "/" + stream);
        if (app == null || stream == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), app == null ?"app不能为null":"stream不能为null");
        }else {
            streamProxyService.del(app, stream);
        }
    }

    @DeleteMapping(value = "/delete/id")
    @ResponseBody
    @Operation(summary = "使用ID移除代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    public void del(@RequestBody StreamProxy proxy){
        logger.info("移除代理： " + proxy.getId());
        if (proxy.getId() <= 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "缺少ID");
        }else {
            streamProxyService.delById(proxy.getId());
        }
    }

    @GetMapping(value = "/start")
    @ResponseBody
    @Operation(summary = "启用代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    public void start(String app, String stream){
        logger.info("启用代理： " + app + "/" + stream);
        boolean result = streamProxyService.start(app, stream);
        if (!result) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @GetMapping(value = "/stop")
    @ResponseBody
    @Operation(summary = "停用代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    public void stop(String app, String stream){
        logger.info("停用代理： " + app + "/" + stream);
        boolean result = streamProxyService.stop(app, stream);
        if (!result) {
            logger.info("停用代理失败： " + app + "/" + stream);
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }
}
