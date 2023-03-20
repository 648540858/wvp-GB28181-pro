//package com.genersoft.iot.vmp.vmanager.record;
//
//import com.alibaba.fastjson2.JSONObject;
//import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
//import com.genersoft.iot.vmp.service.IRecordInfoServer;
//import com.genersoft.iot.vmp.storager.dao.dto.RecordInfo;
//import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
//import com.github.pagehelper.PageInfo;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiImplicitParam;
//import io.swagger.annotations.ApiImplicitParams;
//import io.swagger.annotations.ApiOperation;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//@Tag(name  = "云端录像")
//
//@RestController
//@RequestMapping("/api/record")
//public class RecordController {
//
//    @Autowired
//    private IRecordInfoServer recordInfoServer;
//
//     //@ApiOperation("录像列表查询")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name="page", value = "当前页", required = true, dataTypeClass = Integer.class),
//            @ApiImplicitParam(name="count", value = "每页查询数量", required = true, dataTypeClass = Integer.class),
//            @ApiImplicitParam(name="query", value = "查询内容", dataTypeClass = String.class),
//    })
//    @GetMapping(value = "/app/list")
//    @ResponseBody
//    public Object list(@RequestParam(required = false)Integer page,
//                                     @RequestParam(required = false)Integer count ){
//
//        PageInfo<RecordInfo> recordList = recordInfoServer.getRecordList(page - 1, page - 1 + count);
//        return recordList;
//    }
//
//     //@ApiOperation("获取录像详情")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name="recordInfo", value = "录像记录", required = true, dataTypeClass = RecordInfo.class)
//    })
//    @GetMapping(value = "/detail")
//    @ResponseBody
//    public JSONObject list(RecordInfo recordInfo, String time ){
//
//
//        return null;
//    }
//}
