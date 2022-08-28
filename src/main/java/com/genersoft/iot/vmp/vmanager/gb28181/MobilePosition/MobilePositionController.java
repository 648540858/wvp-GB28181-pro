package com.genersoft.iot.vmp.vmanager.gb28181.MobilePosition;

import java.util.List;
import java.util.UUID;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.util.StringUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name  = "位置信息管理")
@CrossOrigin
@RestController
@RequestMapping("/api/position")
public class MobilePositionController {

    private final static Logger logger = LoggerFactory.getLogger(MobilePositionController.class);

    @Autowired
    private IVideoManagerStorage storager;
    
	@Autowired
	private SIPCommander cmder;
	
	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IDeviceService deviceService;

    /**
     * 查询历史轨迹
     * @param deviceId 设备ID
     * @param start 开始时间
     * @param end 结束时间
     * @return
     */
    @Operation(summary = "查询历史轨迹")
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "channelId", description = "通道国标编号")
    @Parameter(name = "start", description = "开始时间")
    @Parameter(name = "end", description = "结束时间")
    @GetMapping("/history/{deviceId}")
    public List<MobilePosition> positions(@PathVariable String deviceId,
                                                                     @RequestParam(required = false) String channelId,
                                                                     @RequestParam(required = false) String start,
                                                                     @RequestParam(required = false) String end) {

        if (StringUtil.isEmpty(start)) {
            start = null;
        }
        if (StringUtil.isEmpty(end)) {
            end = null;
        }
        return storager.queryMobilePositions(deviceId, channelId, start, end);
    }

    /**
     *  查询设备最新位置
     * @param deviceId 设备ID
     * @return
     */
    @Operation(summary = "查询设备最新位置")
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @GetMapping("/latest/{deviceId}")
    public MobilePosition latestPosition(@PathVariable String deviceId) {
        return storager.queryLatestPosition(deviceId);
    }

    /**
     *  获取移动位置信息
     * @param deviceId 设备ID
     * @return
     */
    @Operation(summary = "获取移动位置信息")
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @GetMapping("/realtime/{deviceId}")
    public DeferredResult<MobilePosition> realTimePosition(@PathVariable String deviceId) {
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
        DeferredResult<MobilePosition> result = new DeferredResult<MobilePosition>(5*1000L);
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
    @Operation(summary = "订阅位置信息")
    @Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @Parameter(name = "expires", description = "订阅超时时间", required = true)
    @Parameter(name = "interval", description = "上报时间间隔", required = true)
    @GetMapping("/subscribe/{deviceId}")
    public String positionSubscribe(@PathVariable String deviceId,
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
        device.setSubscribeCycleForMobilePosition(Integer.parseInt(expires));
        device.setMobilePositionSubmissionInterval(Integer.parseInt(interval));
        deviceService.updateDevice(device);
        String result = msg;
        if (deviceService.removeMobilePositionSubscribe(device)) {
            result += "，成功";
        } else {
            result += "，失败";
        }

        return result;
    }
}
