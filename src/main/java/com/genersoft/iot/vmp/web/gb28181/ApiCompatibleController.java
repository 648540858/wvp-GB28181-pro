package com.genersoft.iot.vmp.web.gb28181;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@CrossOrigin
@RestController
public class ApiCompatibleController {

    private final static Logger logger = LoggerFactory.getLogger(ApiCompatibleController.class);

    @Autowired
    private IMediaService mediaService;

    @GetMapping(value = "/api/v1/stream_info_by_app_and_stream")
    @ResponseBody
    public WVPResult<StreamInfo> getStreamInfoByAppAndStream(HttpServletRequest request, @RequestParam String app, @RequestParam String stream){
        String localAddr = request.getLocalAddr();
        StreamInfo streamINfo = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, localAddr);
        WVPResult<StreamInfo> wvpResult = new WVPResult<>();
        wvpResult.setCode(0);
        wvpResult.setMsg("success");
        wvpResult.setData(streamINfo);
        return wvpResult;
    }
}
