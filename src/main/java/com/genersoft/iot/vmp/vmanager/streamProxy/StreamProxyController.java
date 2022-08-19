package com.genersoft.iot.vmp.vmanager.streamProxy;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("rawtypes")
/**
 * 拉流代理接口
 */
@Tag(name = "拉流代理", description = "")
@Controller
@CrossOrigin
@RequestMapping(value = "/api/proxy")
public class StreamProxyController {

    private final static Logger logger = LoggerFactory.getLogger(StreamProxyController.class);

    @Autowired
    private IRedisCatchStorage redisCatchStorage;


    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IStreamProxyService streamProxyService;


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
    public WVPResult save(@RequestBody StreamProxyItem param){
        logger.info("添加代理： " + JSONObject.toJSONString(param));
        if (StringUtils.isEmpty(param.getMediaServerId())) {
            param.setMediaServerId("auto");
        }
        if (StringUtils.isEmpty(param.getType())) {
            param.setType("default");
        }
        if (StringUtils.isEmpty(param.getGbId())) {
            param.setGbId(null);
        }
        WVPResult<StreamInfo> result = streamProxyService.save(param);
        return result;
    }

    @GetMapping(value = "/ffmpeg_cmd/list")
    @ResponseBody
    @Operation(summary = "获取ffmpeg.cmd模板")
    @Parameter(name = "mediaServerId", description = "流媒体ID", required = true)
    public WVPResult getFFmpegCMDs(@RequestParam String mediaServerId){
        logger.debug("获取节点[ {} ]ffmpeg.cmd模板", mediaServerId );

        MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
        JSONObject data = streamProxyService.getFFmpegCMDs(mediaServerItem);
        WVPResult<JSONObject> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    @DeleteMapping(value = "/del")
    @ResponseBody
    @Operation(summary = "移除代理")
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    public WVPResult del(@RequestParam String app, @RequestParam String stream){
        logger.info("移除代理： " + app + "/" + stream);
        WVPResult<Object> result = new WVPResult<>();
        if (app == null || stream == null) {
            result.setCode(400);
            result.setMsg(app == null ?"app不能为null":"stream不能为null");
        }else {
            streamProxyService.del(app, stream);
            result.setCode(0);
            result.setMsg("success");
        }
        return result;
    }

    @GetMapping(value = "/start")
    @ResponseBody
    @Operation(summary = "启用代理")
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    public Object start(String app, String stream){
        logger.info("启用代理： " + app + "/" + stream);
        boolean result = streamProxyService.start(app, stream);
        if (!result) {
            logger.info("启用代理失败： " + app + "/" + stream);
        }
        return result?"success":"fail";
    }

    @GetMapping(value = "/stop")
    @ResponseBody
    @Operation(summary = "停用代理")
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    public Object stop(String app, String stream){
        logger.info("停用代理： " + app + "/" + stream);
        boolean result = streamProxyService.stop(app, stream);
        return result?"success":"fail";
    }
}
