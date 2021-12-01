package com.genersoft.iot.vmp.vmanager.gb28181.MobilePosition;

import java.util.List;
import java.util.UUID;

import javax.sip.message.Response;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.github.pagehelper.util.StringUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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

/**
 *  位置信息管理
 */
@Api(tags = "位置信息管理")
@CrossOrigin
@RestController
@RequestMapping("/api/position")
public class MobilePositionController {

    private final static Logger logger = LoggerFactory.getLogger(MobilePositionController.class);

    @Autowired
    private IVideoManagerStorager storager;
    
	@Autowired
	private SIPCommander cmder;
	
	@Autowired
	private DeferredResultHolder resultHolder;

    /**
     *  查询历史轨迹
     * @param deviceId 设备ID
     * @param start 开始时间
     * @param end 结束时间
     * @return
     */
    @ApiOperation("查询历史轨迹")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "start", value = "开始时间", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "end", value = "结束时间", required = true, dataTypeClass = String.class),
    })
    @GetMapping("/history/{deviceId}")
    public ResponseEntity<List<MobilePosition>> positions(@PathVariable String deviceId,
                                                    @RequestParam(required = false) String start,
                                                    @RequestParam(required = false) String end) {
//        if (logger.isDebugEnabled()) {
//            logger.debug("查询设备" + deviceId + "的历史轨迹");
//        }

        if (StringUtil.isEmpty(start)) {
            start = null;
        }
        if (StringUtil.isEmpty(end)) {
            end = null;
        }

        List<MobilePosition> result = storager.queryMobilePositions(deviceId, start, end);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     *  查询设备最新位置
     * @param deviceId 设备ID
     * @return
     */
    @ApiOperation("查询设备最新位置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
    })
    @GetMapping("/latest/{deviceId}")
    public ResponseEntity<MobilePosition> latestPosition(@PathVariable String deviceId) {
//        if (logger.isDebugEnabled()) {
//            logger.debug("查询设备" + deviceId + "的最新位置");
//        }
        MobilePosition result = storager.queryLatestPosition(deviceId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     *  获取移动位置信息
     * @param deviceId 设备ID
     * @return
     */
    @ApiOperation("获取移动位置信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
    })
    @GetMapping("/realtime/{deviceId}")
    public DeferredResult<ResponseEntity<MobilePosition>> realTimePosition(@PathVariable String deviceId) {
        Device device = storager.queryVideoDevice(deviceId);
        String uuid = UUID.randomUUID().toString();
        String key = DeferredResultHolder.CALLBACK_CMD_MOBILEPOSITION + deviceId;
        cmder.mobilePostitionQuery(device, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
            msg.setKey(key);
			msg.setData(String.format("获取移动位置信息失败，错误码： %s, %s", event.statusCode, event.msg));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<MobilePosition>> result = new DeferredResult<ResponseEntity<MobilePosition>>(5*1000L);
		result.onTimeout(()->{
			logger.warn(String.format("获取移动位置信息超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
            msg.setId(uuid);
            msg.setKey(key);
			msg.setData("Timeout");
			resultHolder.invokeResult(msg);
		});
        resultHolder.put(key, uuid, result);
        return result;
    }

    /**
     * 订阅位置信息
     * @param deviceId 设备ID
     * @param expires 订阅超时时间
     * @param interval 上报时间间隔
     * @return true = 命令发送成功
     */
    @ApiOperation("订阅位置信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "expires", value = "订阅超时时间", dataTypeClass = String.class),
            @ApiImplicitParam(name = "interval", value = "上报时间间隔", dataTypeClass = String.class),
    })
    @GetMapping("/subscribe/{deviceId}")
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
