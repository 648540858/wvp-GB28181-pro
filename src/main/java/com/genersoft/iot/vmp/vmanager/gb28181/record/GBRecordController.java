package com.genersoft.iot.vmp.vmanager.gb28181.record;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.UUID;

@Tag(name  = "国标录像")

@RestController
@RequestMapping("/api/gb_record")
public class GBRecordController {

	private final static Logger logger = LoggerFactory.getLogger(GBRecordController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IPlayService playService;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private UserSetting userSetting;

	@Operation(summary = "录像查询")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "startTime", description = "开始时间", required = true)
	@Parameter(name = "endTime", description = "结束时间", required = true)
	@GetMapping("/query/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<RecordInfo>> recordinfo(@PathVariable String deviceId, @PathVariable String channelId, String startTime, String endTime){

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("录像信息查询 API调用，deviceId：%s ，startTime：%s， endTime：%s",deviceId, startTime, endTime));
		}
		DeferredResult<WVPResult<RecordInfo>> result = new DeferredResult<>();
		if (!DateUtil.verification(startTime, DateUtil.formatter)){
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "startTime格式为" + DateUtil.PATTERN);
		}
		if (!DateUtil.verification(endTime, DateUtil.formatter)){
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "endTime格式为" + DateUtil.PATTERN);
		}

		Device device = storager.queryVideoDevice(deviceId);
		// 指定超时时间 1分钟30秒
		String uuid = UUID.randomUUID().toString();
		int sn  =  (int)((Math.random()*9+1)*100000);
		String key = DeferredResultHolder.CALLBACK_CMD_RECORDINFO + deviceId + sn;
		RequestMessage msg = new RequestMessage();
		msg.setId(uuid);
		msg.setKey(key);
		try {
			cmder.recordInfoQuery(device, channelId, startTime, endTime, sn, null, null, null, (eventResult -> {
				WVPResult<RecordInfo> wvpResult = new WVPResult<>();
				wvpResult.setCode(ErrorCode.ERROR100.getCode());
				wvpResult.setMsg("查询录像失败, status: " +  eventResult.statusCode + ", message: " + eventResult.msg);
				msg.setData(wvpResult);
				resultHolder.invokeResult(msg);
			}));
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 查询录像: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " +  e.getMessage());
		}

		// 录像查询以channelId作为deviceId查询
		resultHolder.put(key, uuid, result);
		result.onTimeout(()->{
			msg.setData("timeout");
			WVPResult<RecordInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg("timeout");
			msg.setData(wvpResult);
			resultHolder.invokeResult(msg);
		});
        return result;
	}


	@Operation(summary = "开始历史媒体下载")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "startTime", description = "开始时间", required = true)
	@Parameter(name = "endTime", description = "结束时间", required = true)
	@Parameter(name = "downloadSpeed", description = "下载倍速", required = true)
	@GetMapping("/download/start/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<StreamContent>> download(HttpServletRequest request, @PathVariable String deviceId, @PathVariable String channelId,
															 String startTime, String endTime, String downloadSpeed) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("历史媒体下载 API调用，deviceId：%s，channelId：%s，downloadSpeed：%s", deviceId, channelId, downloadSpeed));
		}

		String uuid = UUID.randomUUID().toString();
		String key = DeferredResultHolder.CALLBACK_CMD_DOWNLOAD + deviceId + channelId;
		DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(30000L);
		resultHolder.put(key, uuid, result);
		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setId(uuid);
		requestMessage.setKey(key);


		playService.download(deviceId, channelId, startTime, endTime, Integer.parseInt(downloadSpeed),
		(code, msg, data)->{

			WVPResult<StreamContent> wvpResult = new WVPResult<>();
			if (code == InviteErrorCode.SUCCESS.getCode()) {
				wvpResult.setCode(ErrorCode.SUCCESS.getCode());
				wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

				if (data != null) {
					StreamInfo streamInfo = (StreamInfo)data;
					if (userSetting.getUseSourceIpAsStreamIp()) {
						streamInfo.channgeStreamIp(request.getLocalAddr());
					}
					wvpResult.setData(new StreamContent(streamInfo));
				}
			}else {
				wvpResult.setCode(code);
				wvpResult.setMsg(msg);
			}
			requestMessage.setData(wvpResult);
			resultHolder.invokeResult(requestMessage);
		});

		return result;
	}

	@Operation(summary = "停止历史媒体下载")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "stream", description = "流ID", required = true)
	@GetMapping("/download/stop/{deviceId}/{channelId}/{stream}")
	public void playStop(@PathVariable String deviceId, @PathVariable String channelId, @PathVariable String stream) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备历史媒体下载停止 API调用，deviceId/channelId：%s_%s", deviceId, channelId));
		}

		if (deviceId == null || channelId == null) {
			throw new ControllerException(ErrorCode.ERROR400);
		}

		Device device = deviceService.getDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "设备：" + deviceId + "未找到");
		}

		try {
			cmder.streamByeCmd(device, channelId, stream, null);
		} catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
			logger.error("[停止历史媒体下载]停止历史媒体下载，发送BYE失败 {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
		}
	}

	@Operation(summary = "获取历史媒体下载进度")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "stream", description = "流ID", required = true)
	@GetMapping("/download/progress/{deviceId}/{channelId}/{stream}")
	public StreamContent getProgress(@PathVariable String deviceId, @PathVariable String channelId, @PathVariable String stream) {
		StreamInfo downLoadInfo = playService.getDownLoadInfo(deviceId, channelId, stream);
		if (downLoadInfo == null) {
			throw new ControllerException(ErrorCode.ERROR404);
		}
		return new StreamContent(downLoadInfo);
	}
}
