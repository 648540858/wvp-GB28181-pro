package com.genersoft.iot.vmp.vmanager.MobilePosition;

import java.util.List;

import javax.sip.message.Response;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.github.pagehelper.util.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class MobilePositionController {

    private final static Logger logger = LoggerFactory.getLogger(MobilePositionController.class);

    @Autowired
    private IVideoManagerStorager storager;
    
	@Autowired
	private SIPCommander cmder;
	
	@Autowired
	private DeferredResultHolder resultHolder;
	
    @GetMapping("/positions/{deviceId}/history")
    public ResponseEntity<List<MobilePosition>> positions(@PathVariable String deviceId,
                                                    @RequestParam(required = false) String start,
                                                    @RequestParam(required = false) String end) {
        if (logger.isDebugEnabled()) {
            logger.debug("查询设备" + deviceId + "的历史轨迹");
        }

        if (StringUtil.isEmpty(start)) {
            start = null;
        }
        if (StringUtil.isEmpty(end)) {
            end = null;
        }

        List<MobilePosition> result = storager.queryMobilePositions(deviceId, start, end);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/positions/{deviceId}/latest")
    public ResponseEntity<MobilePosition> latestPosition(@PathVariable String deviceId) {
        if (logger.isDebugEnabled()) {
            logger.debug("查询设备" + deviceId + "的最新位置");
        }
        MobilePosition result = storager.queryLatestPosition(deviceId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/positions/{deviceId}/realtime")
    public DeferredResult<ResponseEntity<MobilePosition>> realTimePosition(@PathVariable String deviceId) {
        Device device = storager.queryVideoDevice(deviceId);
        cmder.mobilePostitionQuery(device, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_MOBILEPOSITION + deviceId);
			msg.setData(String.format("获取移动位置信息失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<MobilePosition>> result = new DeferredResult<ResponseEntity<MobilePosition>>(5*1000L);
		result.onTimeout(()->{
			logger.warn(String.format("获取移动位置信息超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_CATALOG+deviceId);
			msg.setData("Timeout");
			resultHolder.invokeResult(msg);
		});
        resultHolder.put(DeferredResultHolder.CALLBACK_CMD_CATALOG+deviceId, result);
        return result;
    }

    @GetMapping("/positions/{deviceId}/subscribe")
    public ResponseEntity<String> positionSubscribe(@PathVariable String deviceId,
                                                    @RequestParam String expires,
                                                    @RequestParam String interval) {
        String msg = ((expires.equals("0")) ? "取消" : "") + "订阅设备" + deviceId + "的移动位置";
        if (logger.isDebugEnabled()) {
            logger.debug(msg);
        }

        if (StringUtil.isEmpty(interval)) {
            interval = "5";
        }
        Device device = storager.queryVideoDevice(deviceId);

        String result = msg;
        if (cmder.mobilePositionSubscribe(device, Integer.parseInt(expires), Integer.parseInt(interval))) {
            result += "，成功";
        } else {
            result += "，失败";
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
