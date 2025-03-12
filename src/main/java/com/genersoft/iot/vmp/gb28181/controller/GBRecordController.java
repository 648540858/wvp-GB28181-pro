package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name  = "国标录像")
@Slf4j
@RestController
@RequestMapping("/api/gb_record")
public class GBRecordController {

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IPlayService playService;

	@Autowired
	private IDeviceChannelService channelService;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private UserSetting userSetting;

	@Operation(summary = "录像查询", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "startTime", description = "开始时间", required = true)
	@Parameter(name = "endTime", description = "结束时间", required = true)
	@GetMapping("/query/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<RecordInfo>> recordinfo(@PathVariable String deviceId, @PathVariable String channelId, String startTime, String endTime){

		if (log.isDebugEnabled()) {
			log.debug(String.format("录像信息查询 API调用，deviceId：%s ，startTime：%s， endTime：%s",deviceId, startTime, endTime));
		}
		DeferredResult<WVPResult<RecordInfo>> result = new DeferredResult<>(Long.valueOf(userSetting.getRecordInfoTimeout()), TimeUnit.MILLISECONDS);
		if (!DateUtil.verification(startTime, DateUtil.formatter)){
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "startTime格式为" + DateUtil.PATTERN);
		}
		if (!DateUtil.verification(endTime, DateUtil.formatter)){
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "endTime格式为" + DateUtil.PATTERN);
		}

		Device device = deviceService.getDeviceByDeviceId(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + " 不存在");
		}
		DeviceChannel channel = channelService.getOneForSource(device.getId(), channelId);
		if (channel == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), channelId + " 不存在");
		}
		channelService.queryRecordInfo(device, channel, startTime, endTime, (code, msg, data)->{
			WVPResult<RecordInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(code);
			wvpResult.setMsg(msg);
			wvpResult.setData(data);
			result.setResult(wvpResult);
		});
		result.onTimeout(()->{
			WVPResult<RecordInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg("timeout");
			result.setResult(wvpResult);
		});
        return result;
	}


	@Operation(summary = "开始历史媒体下载", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "startTime", description = "开始时间", required = true)
	@Parameter(name = "endTime", description = "结束时间", required = true)
	@Parameter(name = "downloadSpeed", description = "下载倍速", required = true)
	@GetMapping("/download/start/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<StreamContent>> download(HttpServletRequest request, @PathVariable String deviceId, @PathVariable String channelId,
															 String startTime, String endTime, String downloadSpeed) {

		if (log.isDebugEnabled()) {
			log.debug(String.format("历史媒体下载 API调用，deviceId：%s，channelId：%s，downloadSpeed：%s", deviceId, channelId, downloadSpeed));
		}

		String uuid = UUID.randomUUID().toString();
		String key = DeferredResultHolder.CALLBACK_CMD_DOWNLOAD + deviceId + channelId;
		DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(30000L);
		resultHolder.put(key, uuid, result);
		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setId(uuid);
		requestMessage.setKey(key);

		Device device = deviceService.getDeviceByDeviceId(deviceId);
		if (device == null) {
			log.warn("[开始历史媒体下载] 未找到设备 deviceId: {},channelId:{}", deviceId, channelId);
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到设备：" + deviceId);
		}

		DeviceChannel channel = channelService.getOne(deviceId, channelId);
		if (channel == null) {
			log.warn("[开始历史媒体下载] 未找到通道 deviceId: {},channelId:{}", deviceId, channelId);
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到通道：" + channelId);
		}
		playService.download(device, channel, startTime, endTime, Integer.parseInt(downloadSpeed),
		(code, msg, data)->{

			WVPResult<StreamContent> wvpResult = new WVPResult<>();
			if (code == InviteErrorCode.SUCCESS.getCode()) {
				wvpResult.setCode(ErrorCode.SUCCESS.getCode());
				wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

				if (data != null) {
					StreamInfo streamInfo = (StreamInfo)data;
					if (userSetting.getUseSourceIpAsStreamIp()) {
						streamInfo.changeStreamIp(request.getLocalAddr());
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

	@Operation(summary = "停止历史媒体下载", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "stream", description = "流ID", required = true)
	@GetMapping("/download/stop/{deviceId}/{channelId}/{stream}")
	public void downloadStop(@PathVariable String deviceId, @PathVariable String channelId, @PathVariable String stream) {

		if (log.isDebugEnabled()) {
			log.debug(String.format("设备历史媒体下载停止 API调用，deviceId/channelId：%s_%s", deviceId, channelId));
		}

		if (deviceId == null || channelId == null) {
			throw new ControllerException(ErrorCode.ERROR400);
		}

		Device device = deviceService.getDeviceByDeviceId(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "设备：" + deviceId + " 未找到");
		}
		DeviceChannel deviceChannel = channelService.getOneForSource(deviceId, channelId);
		if (deviceChannel == null) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "通道：" + channelId + " 未找到");
		}
		playService.stop(InviteSessionType.DOWNLOAD, device, deviceChannel, stream);
	}

	@Operation(summary = "获取历史媒体下载进度", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "stream", description = "流ID", required = true)
	@GetMapping("/download/progress/{deviceId}/{channelId}/{stream}")
	public StreamContent getProgress(@PathVariable String deviceId, @PathVariable String channelId, @PathVariable String stream) {
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		if (device == null) {
			log.warn("[获取历史媒体下载进度] 未找到设备 deviceId: {},channelId:{}", deviceId, channelId);
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到设备：" + deviceId);
		}

		DeviceChannel channel = channelService.getOne(deviceId, channelId);
		if (channel == null) {
			log.warn("[获取历史媒体下载进度] 未找到通道 deviceId: {},channelId:{}", deviceId, channelId);
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到通道：" + channelId);
		}
		StreamInfo downLoadInfo = playService.getDownLoadInfo(device, channel, stream);
		if (downLoadInfo == null) {
			throw new ControllerException(ErrorCode.ERROR404);
		}
		return new StreamContent(downLoadInfo);
	}
}
