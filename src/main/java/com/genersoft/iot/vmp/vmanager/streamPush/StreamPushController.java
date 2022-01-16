package com.genersoft.iot.vmp.vmanager.streamPush;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.impl.StreamPushUploadFileHandler;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.sl.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

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

    @ApiOperation("推流列表查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name="page", value = "当前页", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name="count", value = "每页查询数量", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name="query", value = "查询内容", dataTypeClass = String.class),
            @ApiImplicitParam(name="online", value = "是否在线", dataTypeClass = Boolean.class),
    })
    @GetMapping(value = "/list")
    @ResponseBody
    public PageInfo<StreamPushItem> list(@RequestParam(required = false)Integer page,
                                         @RequestParam(required = false)Integer count,
                                         @RequestParam(required = false)String query,
                                         @RequestParam(required = false)Boolean online ){

        PageInfo<StreamPushItem> pushList = streamPushService.getPushList(page, count);
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
    public Object removeFormGB(@RequestParam(required = true)String app, @RequestParam(required = true)String streamId){
        if (streamPushService.stop(app, streamId)){
            return "success";
        }else {
            return "fail";
        }
    }
    @PostMapping(value = "upload")
    @ResponseBody
    public String uploadChannelFile(@RequestParam(value = "file") MultipartFile file){
        if (file.isEmpty()) {
            return "fail";
        }
        //获取文件流
        InputStream inputStream = null;
        try {
            String name = file.getName();
            inputStream = file.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //传入参数
        ExcelReader excelReader = EasyExcel.read(inputStream, StreamPushExcelDto.class,
                new StreamPushUploadFileHandler(streamPushService, mediaServerService.getDefaultMediaServer().getId())).build();
        ReadSheet readSheet = EasyExcel.readSheet(0).build();
        excelReader.read(readSheet);
        excelReader.finish();
        return "success";
    }


}
