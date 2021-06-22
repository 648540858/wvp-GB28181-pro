package com.genersoft.iot.vmp.vmanager.onvif;

import be.teletask.onvif.models.OnvifDevice;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.onvif.IONVIFServer;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.UUID;

@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Api(tags = "onvif设备")
@CrossOrigin
@RestController
@RequestMapping("/api/onvif")
public class ONVIFController {


    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private IONVIFServer onvifServer;


    @ApiOperation("搜索")
    @ApiImplicitParams({
            @ApiImplicitParam(name="timeout", value = "超时时间", required = true, dataTypeClass = Integer.class),
    })
    @GetMapping(value = "/search")
    @ResponseBody
    public DeferredResult<ResponseEntity<WVPResult>> search(@RequestParam(required = false)Integer timeout){
        DeferredResult<ResponseEntity<WVPResult>> result = new DeferredResult<>(timeout + 10L);
        UUID uuid = UUID.randomUUID();
        result.onTimeout(()->{
            RequestMessage msg = new RequestMessage();
            msg.setId(DeferredResultHolder.CALLBACK_ONVIF + uuid);
            WVPResult<String> wvpResult = new WVPResult();
            wvpResult.setCode(0);
            wvpResult.setMsg("搜索超时");
            msg.setData(wvpResult);
            resultHolder.invokeResult(msg);
        });
        resultHolder.put(DeferredResultHolder.CALLBACK_ONVIF + uuid, result);

        onvifServer.search(timeout, (errorCode, onvifDevices) ->{
            RequestMessage msg = new RequestMessage();
            msg.setId(DeferredResultHolder.CALLBACK_ONVIF + uuid);
            WVPResult<List<String>> resultData = new WVPResult();
            resultData.setCode(errorCode);
            if (errorCode == 0) {
                resultData.setMsg("success");
                resultData.setData(onvifDevices);
            }else {
                resultData.setMsg("fail");
            }
            msg.setData(resultData);
            msg.setData(resultData);
            resultHolder.invokeResult(msg);
        });

        return result;
    }

    @ApiOperation("获取onvif的rtsp地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name="timeout", value = "超时时间", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name="hostname", value = "onvif地址", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name="username", value = "用户名", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name="password", value = "密码", required = true, dataTypeClass = String.class),
    })
    @GetMapping(value = "/rtsp")
    @ResponseBody
    public DeferredResult<ResponseEntity<WVPResult>> getRTSPUrl(@RequestParam(value="timeout", required=false, defaultValue="3000") Integer timeout,
                                                                @RequestParam(required = true) String hostname,
                                                                @RequestParam(required = false) String username,
                                                                @RequestParam(required = false) String password
                                                                ){

        DeferredResult<ResponseEntity<WVPResult>> result = new DeferredResult<>(timeout + 10L);
        UUID uuid = UUID.randomUUID();
        result.onTimeout(()->{
            RequestMessage msg = new RequestMessage();
            msg.setId(DeferredResultHolder.CALLBACK_ONVIF + uuid);
            WVPResult<String> wvpResult = new WVPResult();
            wvpResult.setCode(0);
            wvpResult.setMsg("获取onvif的rtsp地址超时");
            msg.setData(wvpResult);
            resultHolder.invokeResult(msg);
        });
        resultHolder.put(DeferredResultHolder.CALLBACK_ONVIF + uuid, result);
        OnvifDevice onvifDevice = new OnvifDevice(hostname, username, password);
        onvifServer.getRTSPUrl(timeout, onvifDevice,  (errorCode, url) ->{
            RequestMessage msg = new RequestMessage();
            msg.setId(DeferredResultHolder.CALLBACK_ONVIF + uuid);
            WVPResult<String> resultData = new WVPResult();
            resultData.setCode(errorCode);
            if (errorCode == 0) {
                resultData.setMsg("success");
                resultData.setData(url);
            }else {
                resultData.setMsg(url);
            }
            msg.setData(resultData);

            resultHolder.invokeResult(msg);
        });

        return result;
    }

}
