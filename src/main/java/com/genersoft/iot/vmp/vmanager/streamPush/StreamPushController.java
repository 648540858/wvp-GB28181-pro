package com.genersoft.iot.vmp.vmanager.streamPush;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.impl.StreamPushUploadFileHandler;
import com.genersoft.iot.vmp.vmanager.bean.BatchGBStreamParam;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.sl.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Api(tags = "推流信息管理")
@Controller
@CrossOrigin
@RequestMapping(value = "/api/push")
public class StreamPushController {

    private final static Logger logger = LoggerFactory.getLogger(StreamPushController.class);

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @ApiOperation("推流列表查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name="page", value = "当前页", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name="count", value = "每页查询数量", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name="query", value = "查询内容", dataTypeClass = String.class),
            @ApiImplicitParam(name="pushing", value = "是否正在推流", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name="mediaServerId", value = "流媒体ID", dataTypeClass = String.class),
    })
    @GetMapping(value = "/list")
    @ResponseBody
    public PageInfo<StreamPushItem> list(@RequestParam(required = false)Integer page,
                                         @RequestParam(required = false)Integer count,
                                         @RequestParam(required = false)String query,
                                         @RequestParam(required = false)Boolean pushing,
                                         @RequestParam(required = false)String mediaServerId ){

        if (StringUtils.isEmpty(query)) {
            query = null;
        }
        if (StringUtils.isEmpty(mediaServerId)) {
            mediaServerId = null;
        }
        PageInfo<StreamPushItem> pushList = streamPushService.getPushList(page, count, query, pushing, mediaServerId);
        return pushList;
    }

    @ApiOperation("将推流添加到国标")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "stream", value = "直播流关联国标平台", dataTypeClass = GbStream.class),
    })
    @PostMapping(value = "/save_to_gb")
    @ResponseBody
    public Object saveToGB(@RequestBody GbStream stream){
        if (streamPushService.saveToGB(stream)){
            return "success";
        }else {
            return "fail";
        }
    }


    @ApiOperation("将推流移出到国标")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "stream", value = "直播流关联国标平台", dataTypeClass = GbStream.class),
    })
    @DeleteMapping(value = "/remove_form_gb")
    @ResponseBody
    public Object removeFormGB(@RequestBody GbStream stream){
        if (streamPushService.removeFromGB(stream)){
            return "success";
        }else {
            return "fail";
        }
    }


    @ApiOperation("中止一个推流")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "app", value = "应用名", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "streamId", value = "流ID", required = true, dataTypeClass = String.class),
    })
    @PostMapping(value = "/stop")
    @ResponseBody
    public Object stop(String app, String streamId){
        if (streamPushService.stop(app, streamId)){
            return "success";
        }else {
            return "fail";
        }
    }

    @ApiOperation("中止多个推流")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "app", value = "应用名", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "streamId", value = "流ID", required = true, dataTypeClass = String.class),
    })
    @DeleteMapping(value = "/batchStop")
    @ResponseBody
    public Object batchStop(@RequestBody BatchGBStreamParam batchGBStreamParam){
        if (batchGBStreamParam.getGbStreams().size() == 0) {
            return "fail";
        }
        if (streamPushService.batchStop(batchGBStreamParam.getGbStreams())){
            return "success";
        }else {
            return "fail";
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
            e.printStackTrace();
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


}
