package com.genersoft.iot.vmp.vmanager.streamProxy;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.reponse.ResponseData;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = "拉流代理")
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


    @ApiOperation("分页查询流代理")
    @ApiImplicitParams({
            @ApiImplicitParam(name="pageNo", value = "当前页", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name="pageSize", value = "每页查询数量", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name="query", value = "查询内容", dataTypeClass = String.class),
            @ApiImplicitParam(name="enable", value = "是否启用", dataTypeClass = Boolean.class),
    })
    @GetMapping(value = "/list")
    @ResponseBody
    public ResponseData list(@RequestParam(required = false)Integer pageNo,
                             @RequestParam(required = false)Integer pageSize,
                             @RequestParam(required = false)String query,
                             @RequestParam(required = false)Boolean enable ){

        return ResponseData.success(streamProxyService.getAll(pageNo, pageSize, query, enable));
    }

    @ApiOperation("保存代理")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "param", value = "代理参数", dataTypeClass = StreamProxyItem.class),
    })
    @PostMapping(value = "/save")
    @ResponseBody
    public WVPResult save(@RequestBody StreamProxyItem param){
        logger.info("添加代理： " + JSONObject.toJSONString(param));
        if (StringUtils.isEmpty(param.getMediaServerId())) param.setMediaServerId("auto");
        String msg = streamProxyService.save(param);
        WVPResult<Object> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg(msg);
        return result;
    }

    @ApiOperation("获取ffmpeg.cmd模板")
    @GetMapping(value = "/ffmpeg_cmd/list")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mediaServerId", value = "流媒体ID", dataTypeClass = String.class),
    })
    @ResponseBody
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

    @ApiOperation("移除代理")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "app", value = "应用名", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "stream", value = "流ID", required = true, dataTypeClass = String.class),
    })
    @DeleteMapping(value = "/del")
    @ResponseBody
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

    @ApiOperation("启用代理")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "app", value = "应用名", dataTypeClass = String.class),
            @ApiImplicitParam(name = "stream", value = "流ID", dataTypeClass = String.class),
    })
    @GetMapping(value = "/start")
    @ResponseBody
    public Object start(String app, String stream){
        logger.info("启用代理： " + app + "/" + stream);
        boolean result = streamProxyService.start(app, stream);
        return result?"success":"fail";
    }

    @ApiOperation("停用代理")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "app", value = "应用名", dataTypeClass = String.class),
            @ApiImplicitParam(name = "stream", value = "流ID", dataTypeClass = String.class),
    })
    @GetMapping(value = "/stop")
    @ResponseBody
    public Object stop(String app, String stream){
        logger.info("停用代理： " + app + "/" + stream);
        boolean result = streamProxyService.stop(app, stream);
        return result?"success":"fail";
    }
}
