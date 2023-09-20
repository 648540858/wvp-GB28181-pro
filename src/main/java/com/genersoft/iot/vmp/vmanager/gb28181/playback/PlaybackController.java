package com.genersoft.iot.vmp.vmanager.gb28181.playback;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.ServiceException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.UUID;

/**
 * @author lin
 */
@Tag(name = "视频回放")

@RestController
@RequestMapping("/api/playback")
public class PlaybackController {

	private final static Logger logger = LoggerFactory.getLogger(PlaybackController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private IInviteStreamService inviteStreamService;

	@Autowired
	private IPlayService playService;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private UserSetting userSetting;

	@Operation(summary = "开始视频回放")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "startTime", description = "开始时间", required = true)
	@Parameter(name = "endTime", description = "结束时间", required = true)
	@GetMapping("/start/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<StreamContent>> start(HttpServletRequest request, @PathVariable String deviceId, @PathVariable String channelId,
														  String startTime, String endTime) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备回放 API调用，deviceId：%s ，channelId：%s", deviceId, channelId));
		}

		String uuid = UUID.randomUUID().toString();
		String key = DeferredResultHolder.CALLBACK_CMD_PLAYBACK + deviceId + channelId;
		DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
		resultHolder.put(key, uuid, result);

		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setKey(key);
		requestMessage.setId(uuid);

		playService.playBack(deviceId, channelId, startTime, endTime,
				(code, msg, data)->{

					WVPResult<StreamContent> wvpResult = new WVPResult<>();
					if (code == InviteErrorCode.SUCCESS.getCode()) {
						wvpResult.setCode(ErrorCode.SUCCESS.getCode());
						wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

						if (data != null) {
							StreamInfo streamInfo = (StreamInfo)data;
							if (userSetting.getUseSourceIpAsStreamIp()) {
								streamInfo=streamInfo.clone();//深拷贝
								String host;
								try {
									URL url=new URL(request.getRequestURL().toString());
									host=url.getHost();
								} catch (MalformedURLException e) {
									host=request.getLocalAddr();
								}
								streamInfo.channgeStreamIp(host);
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


	@Operation(summary = "停止视频回放")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "stream", description = "流ID", required = true)
	@GetMapping("/stop/{deviceId}/{channelId}/{stream}")
	public void playStop(
			@PathVariable String deviceId,
			@PathVariable String channelId,
			@PathVariable String stream) {
		if (ObjectUtils.isEmpty(deviceId) || ObjectUtils.isEmpty(channelId) || ObjectUtils.isEmpty(stream)) {
			throw new ControllerException(ErrorCode.ERROR400);
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "设备：" + deviceId + " 未找到");
		}
		try {
			cmder.streamByeCmd(device, channelId, stream, null);
		} catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "发送bye失败： " + e.getMessage());
		}
	}


	@Operation(summary = "回放暂停")
	@Parameter(name = "streamId", description = "回放流ID", required = true)
	@GetMapping("/pause/{streamId}")
	public void playPause(@PathVariable String streamId) {
		logger.info("playPause: "+streamId);

		try {
			playService.pauseRtp(streamId);
		} catch (ServiceException e) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), e.getMessage());
		} catch (InvalidArgumentException | ParseException | SipException e) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
		}
	}


	@Operation(summary = "回放恢复")
	@Parameter(name = "streamId", description = "回放流ID", required = true)
	@GetMapping("/resume/{streamId}")
	public void playResume(@PathVariable String streamId) {
		logger.info("playResume: "+streamId);
		try {
			playService.resumeRtp(streamId);
		} catch (ServiceException e) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), e.getMessage());
		} catch (InvalidArgumentException | ParseException | SipException e) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
		}
	}


	@Operation(summary = "回放拖动播放")
	@Parameter(name = "streamId", description = "回放流ID", required = true)
	@Parameter(name = "seekTime", description = "拖动偏移量，单位s", required = true)
	@GetMapping("/seek/{streamId}/{seekTime}")
	public void playSeek(@PathVariable String streamId, @PathVariable long seekTime) {
		logger.info("playSeek: "+streamId+", "+seekTime);
		InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);

		if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
			logger.warn("streamId不存在!");
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "streamId不存在");
		}
		Device device = storager.queryVideoDevice(inviteInfo.getDeviceId());
		try {
			cmder.playSeekCmd(device, inviteInfo.getStreamInfo(), seekTime);
		} catch (InvalidArgumentException | ParseException | SipException e) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
		}
	}

	@Operation(summary = "回放倍速播放")
	@Parameter(name = "streamId", description = "回放流ID", required = true)
	@Parameter(name = "speed", description = "倍速0.25 0.5 1、2、4", required = true)
	@GetMapping("/speed/{streamId}/{speed}")
	public void playSpeed(@PathVariable String streamId, @PathVariable Double speed) {
		logger.info("playSpeed: "+streamId+", "+speed);
		InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);

		if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
			logger.warn("streamId不存在!");
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "streamId不存在");
		}
		if(speed != 0.25 && speed != 0.5 && speed != 1 && speed != 2.0 && speed != 4.0) {
			logger.warn("不支持的speed： " + speed);
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持的speed（0.25 0.5 1、2、4）");
		}
		Device device = storager.queryVideoDevice(inviteInfo.getDeviceId());
		try {
			cmder.playSpeedCmd(device, inviteInfo.getStreamInfo(), speed);
		} catch (InvalidArgumentException | ParseException | SipException e) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
		}
	}
}
