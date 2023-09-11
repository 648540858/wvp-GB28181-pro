package com.genersoft.iot.vmp.vmanager.streamPush;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.impl.StreamPushUploadFileHandler;
import com.genersoft.iot.vmp.vmanager.bean.*;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name  = "推流信息管理")
@Controller

@RequestMapping(value = "/api/push")
public class StreamPushController {

    private final static Logger logger = LoggerFactory.getLogger(StreamPushController.class);

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private UserSetting userSetting;

    @GetMapping(value = "/list")
    @ResponseBody
    @Operation(summary = "推流列表查询")
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "pushing", description = "是否正在推流")
    @Parameter(name = "mediaServerId", description = "流媒体ID")
    public PageInfo<StreamPushItem> list(@RequestParam(required = false)Integer page,
                                         @RequestParam(required = false)Integer count,
                                         @RequestParam(required = false)String query,
                                         @RequestParam(required = false)Boolean pushing,
                                         @RequestParam(required = false)String mediaServerId ){

        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        if (ObjectUtils.isEmpty(mediaServerId)) {
            mediaServerId = null;
        }
        PageInfo<StreamPushItem> pushList = streamPushService.getPushList(page, count, query, pushing, mediaServerId);
        return pushList;
    }

    @PostMapping(value = "/save_to_gb")
    @ResponseBody
    @Operation(summary = "将推流添加到国标")
    public void saveToGB(@RequestBody GbStream stream){
        if (!streamPushService.saveToGB(stream)){
           throw new ControllerException(ErrorCode.ERROR100);
        }
    }


    @DeleteMapping(value = "/remove_form_gb")
    @ResponseBody
    @Operation(summary = "将推流移出到国标")
    public void removeFormGB(@RequestBody GbStream stream){
        if (!streamPushService.removeFromGB(stream)){
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }


    @PostMapping(value = "/stop")
    @ResponseBody
    @Operation(summary = "中止一个推流")
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    public void stop(String app, String streamId){
        if (!streamPushService.stop(app, streamId)){
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @DeleteMapping(value = "/batchStop")
    @ResponseBody
    @Operation(summary = "中止多个推流")
    public void batchStop(@RequestBody BatchGBStreamParam batchGBStreamParam){
        if (batchGBStreamParam.getGbStreams().size() == 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
        if (!streamPushService.batchStop(batchGBStreamParam.getGbStreams())){
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @PostMapping(value = "upload")
    @ResponseBody
    public DeferredResult<ResponseEntity<WVPResult<Object>>> uploadChannelFile(@RequestParam(value = "file") MultipartFile file){

        // 最多处理文件一个小时
        DeferredResult<ResponseEntity<WVPResult<Object>>> result = new DeferredResult<>(60*60*1000L);
        // 录像查询以channelId作为deviceId查询
        String key = DeferredResultHolder.UPLOAD_FILE_CHANNEL;
        String uuid = UUID.randomUUID().toString();
        logger.info("通道导入文件类型: {}",file.getContentType() );
        if (file.isEmpty()) {
            logger.warn("通道导入文件为空");
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(-1);
            wvpResult.setMsg("文件为空");
            result.setResult(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wvpResult));
            return result;
        }
        if (file.getContentType() == null) {
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(-1);
            wvpResult.setMsg("无法识别文件类型");
            result.setResult(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wvpResult));
            return result;
        }
        // 同时只处理一个文件
        if (resultHolder.exist(key, null)) {
            logger.warn("已有导入任务正在执行");
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(-1);
            wvpResult.setMsg("已有导入任务正在执行");
            result.setResult(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(wvpResult));
            return result;
        }

        resultHolder.put(key, uuid, result);
        result.onTimeout(()->{
            logger.warn("通道导入超时，可能文件过大");
            RequestMessage msg = new RequestMessage();
            msg.setKey(key);
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(-1);
            wvpResult.setMsg("导入超时，可能文件过大");
            msg.setData(wvpResult);
            resultHolder.invokeAllResult(msg);
        });
        //获取文件流
        InputStream inputStream = null;
        try {
            String name = file.getName();
            inputStream = file.getInputStream();
        } catch (IOException e) {
            logger.error("未处理的异常 ", e);
        }
        try {
            //传入参数
            ExcelReader excelReader = EasyExcel.read(inputStream, StreamPushExcelDto.class,
                    new StreamPushUploadFileHandler(streamPushService, mediaServerService.getDefaultMediaServer().getId(), (errorStreams, errorGBs)->{
                        logger.info("通道导入成功，存在重复App+Stream为{}个，存在国标ID为{}个", errorStreams.size(), errorGBs.size());
                        RequestMessage msg = new RequestMessage();
                        msg.setKey(key);
                        WVPResult<Map<String, List<String>>> wvpResult = new WVPResult<>();
                        if (errorStreams.size() == 0 && errorGBs.size() == 0) {
                            wvpResult.setCode(0);
                            wvpResult.setMsg("成功");
                        }else {
                            wvpResult.setCode(1);
                            wvpResult.setMsg("导入成功。但是存在重复数据");
                            Map<String, List<String>> errorData = new HashMap<>();
                            errorData.put("gbId", errorGBs);
                            errorData.put("stream", errorStreams);
                            wvpResult.setData(errorData);
                        }
                        msg.setData(wvpResult);
                        resultHolder.invokeAllResult(msg);
                    })).build();
            ReadSheet readSheet = EasyExcel.readSheet(0).build();
            excelReader.read(readSheet);
            excelReader.finish();
        }catch (Exception e) {
            logger.warn("通道导入失败：", e);
            RequestMessage msg = new RequestMessage();
            msg.setKey(key);
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(-1);
            wvpResult.setMsg("通道导入失败: " + e.getMessage() );
            msg.setData(wvpResult);
            resultHolder.invokeAllResult(msg);
        }


        return result;
    }

    /**
     * 获取推流播放地址
     * @param app 应用名
     * @param stream 流id
     * @return
     */
    @GetMapping(value = "/getPlayUrl")
    @ResponseBody
    @Operation(summary = "获取推流播放地址")
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    @Parameter(name = "mediaServerId", description = "媒体服务器id")
    public StreamContent getPlayUrl(@RequestParam String app, @RequestParam String stream,
                                    @RequestParam(required = false) String mediaServerId){
        boolean authority = false;
        // 是否登陆用户, 登陆用户返回完整信息
        LoginUser userInfo = SecurityUtils.getUserInfo();
        if (userInfo!= null) {
            authority = true;
        }
        StreamPushItem push = streamPushService.getPush(app, stream);
        if (push != null && !push.isSelf()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "来自其他平台的推流信息");
        }
        StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, authority);
        if (streamInfo == null){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "获取播放地址失败");
        }
        return new StreamContent(streamInfo);
    }

    /**
     * 添加推流信息
     * @param stream 推流信息
     * @return
     */
    @PostMapping(value = "/add")
    @ResponseBody
    @Operation(summary = "添加推流信息")
    public void add(@RequestBody StreamPushItem stream){
        if (ObjectUtils.isEmpty(stream.getGbId())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "国标ID不可为空");
        }
        if (ObjectUtils.isEmpty(stream.getApp()) && ObjectUtils.isEmpty(stream.getStream())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "app或stream不可为空");
        }
        stream.setStatus(false);
        stream.setPushIng(false);
        stream.setAliveSecond(0L);
        stream.setTotalReaderCount("0");
        if (!streamPushService.add(stream)) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }
}
