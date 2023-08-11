package com.genersoft.iot.vmp.vmanager.streamProxy;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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


    @Operation(summary = "分页查询流代理")
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "online", description = "是否在线")
    @GetMapping(value = "/list")
    @ResponseBody
    public PageInfo<StreamProxyItem> list(@RequestParam(required = false)Integer page,
                                          @RequestParam(required = false)Integer count,
                                          @RequestParam(required = false)String query,
                                          @RequestParam(required = false)Boolean online ){

        return streamProxyService.getAll(page, count);
    }

    @Operation(summary = "保存代理", parameters = {
            @Parameter(name = "param", description = "代理参数", required = true),
    })
    @PostMapping(value = "/save")
    @ResponseBody
    public DeferredResult<Object> save(@RequestBody StreamProxyItem param){
        logger.info("添加代理： " + JSONObject.toJSONString(param));
        if (ObjectUtils.isEmpty(param.getMediaServerId())) {
            param.setMediaServerId("auto");
        }
        if (ObjectUtils.isEmpty(param.getType())) {
            param.setType("default");
        }
        if (ObjectUtils.isEmpty(param.getGbId())) {
            param.setGbId(null);
        }

        RequestMessage requestMessage = new RequestMessage();
        String key = DeferredResultHolder.CALLBACK_CMD_PROXY + param.getApp() + param.getStream();
        requestMessage.setKey(key);
        String uuid = UUID.randomUUID().toString();
        requestMessage.setId(uuid);
        DeferredResult<Object> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        // 录像查询以channelId作为deviceId查询
        resultHolder.put(key, uuid, result);
        result.onTimeout(()->{
            WVPResult<StreamInfo> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("超时");
            requestMessage.setData(wvpResult);
            resultHolder.invokeAllResult(requestMessage);
        });

        streamProxyService.save(param, (code, msg, streamInfo) -> {
            logger.info("[拉流代理] {}", code == ErrorCode.SUCCESS.getCode()? "成功":"失败： " + msg);
            if (code == ErrorCode.SUCCESS.getCode()) {
                requestMessage.setData(new StreamContent(streamInfo));
            }else {
                requestMessage.setData(WVPResult.fail(code, msg));
            }
            resultHolder.invokeAllResult(requestMessage);
        });
        return result;
    }

    @GetMapping(value = "/ffmpeg_cmd/list")
    @ResponseBody
    @Operation(summary = "获取ffmpeg.cmd模板")
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
    @Operation(summary = "移除代理")
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

    @GetMapping(value = "/start")
    @ResponseBody
    @Operation(summary = "启用代理")
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
    @Operation(summary = "停用代理")
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
