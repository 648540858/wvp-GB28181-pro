package com.genersoft.iot.vmp.vmanager.gb28181.play;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;


/**
 * @author lin
 */
@Tag(name  = "国标设备点播")

@RestController
@RequestMapping("/api/play")
public class PlayController {

	private final static Logger logger = LoggerFactory.getLogger(PlayController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private VideoStreamSessionManager streamSession;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private IInviteStreamService inviteStreamService;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IPlayService playService;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private UserSetting userSetting;

	@Operation(summary = "开始点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@GetMapping("/start/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<StreamContent>> play(HttpServletRequest request, @PathVariable String deviceId,
														 @PathVariable String channelId) {

		logger.info("[开始点播] deviceId：{}, channelId：{}, ", deviceId, channelId);
		// 获取可用的zlm
		Device device = storager.queryVideoDevice(deviceId);
		MediaServer newMediaServerItem = playService.getNewMediaServerItem(device);

		RequestMessage requestMessage = new RequestMessage();
		String key = DeferredResultHolder.CALLBACK_CMD_PLAY + deviceId + channelId;
		requestMessage.setKey(key);
		String uuid = UUID.randomUUID().toString();
		requestMessage.setId(uuid);
		DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

		result.onTimeout(()->{
			logger.info("[点播等待超时] deviceId：{}, channelId：{}, ", deviceId, channelId);
			// 释放rtpserver
			WVPResult<StreamInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg("点播超时");
			requestMessage.setData(wvpResult);
			resultHolder.invokeAllResult(requestMessage);
			inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
			storager.stopPlay(deviceId, channelId);
		});

		// 录像查询以channelId作为deviceId查询
		resultHolder.put(key, uuid, result);

		playService.play(newMediaServerItem, deviceId, channelId, null, (code, msg, data) -> {
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
				}else {
					wvpResult.setCode(code);
					wvpResult.setMsg(msg);
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

	@Operation(summary = "停止点播", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "isSubStream", description = "是否子码流（true-子码流，false-主码流），默认为false", required = true)
	@GetMapping("/stop/{deviceId}/{channelId}")
	public JSONObject playStop(@PathVariable String deviceId, @PathVariable String channelId,boolean isSubStream) {

		logger.debug(String.format("设备预览/回放停止API调用，streamId：%s_%s", deviceId, channelId ));

		if (deviceId == null || channelId == null) {
			throw new ControllerException(ErrorCode.ERROR400);
		}

		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备[" + deviceId + "]不存在");
		}

		InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
		if (inviteInfo == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "点播未找到");
		}
		if (InviteSessionStatus.ok == inviteInfo.getStatus()) {
			try {
				logger.info("[停止点播] {}/{}", device.getDeviceId(), channelId);
				cmder.streamByeCmd(device, channelId, inviteInfo.getStream(), null, null);
			} catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
				logger.error("[命令发送失败] 停止点播， 发送BYE: {}", e.getMessage());
				throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
			}
		}
		inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
		storager.stopPlay(deviceId, channelId);

		JSONObject json = new JSONObject();
		json.put("deviceId", deviceId);
		json.put("channelId", channelId);
		json.put("isSubStream", isSubStream);
		return json;
	}
	/**
	 * 结束转码
	 */
	@Operation(summary = "结束转码", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "key", description = "视频流key", required = true)
	@Parameter(name = "mediaServerId", description = "流媒体服务ID", required = true)
	@PostMapping("/convertStop/{key}")
	public void playConvertStop(@PathVariable String key, String mediaServerId) {
		if (mediaServerId == null) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "流媒体：" + mediaServerId + "不存在" );
		}
		MediaServer mediaInfo = mediaServerService.getOne(mediaServerId);
		if (mediaInfo == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "使用的流媒体已经停止运行" );
		}else {
			Boolean deleted = mediaServerService.delFFmpegSource(mediaInfo, key);
			if (!deleted) {
				throw new ControllerException(ErrorCode.ERROR100 );
			}
		}
	}

	@Operation(summary = "语音广播命令", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "deviceId", description = "通道国标编号", required = true)
	@Parameter(name = "timeout", description = "推流超时时间(秒)", required = true)
	@GetMapping("/broadcast/{deviceId}/{channelId}")
	@PostMapping("/broadcast/{deviceId}/{channelId}")
    public AudioBroadcastResult broadcastApi(@PathVariable String deviceId, @PathVariable String channelId, Integer timeout, Boolean broadcastMode) {
		if (logger.isDebugEnabled()) {
			logger.debug("语音广播API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "未找到设备： " + deviceId);
		}
		if (channelId == null) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "未找到通道： " + channelId);
		}

		return playService.audioBroadcast(device, channelId, broadcastMode);

	}

	@Operation(summary = "停止语音广播")
	@Parameter(name = "deviceId", description = "设备Id", required = true)
	@Parameter(name = "channelId", description = "通道Id", required = true)
	@GetMapping("/broadcast/stop/{deviceId}/{channelId}")
	@PostMapping("/broadcast/stop/{deviceId}/{channelId}")
	public void stopBroadcast(@PathVariable String deviceId, @PathVariable String channelId) {
		if (logger.isDebugEnabled()) {
			logger.debug("停止语音广播API调用");
		}
//		try {
//			playService.stopAudioBroadcast(deviceId, channelId);
//		} catch (InvalidArgumentException | ParseException  | SipException e) {
//			logger.error("[命令发送失败] 停止语音: {}", e.getMessage());
//			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " +  e.getMessage());
//		}
		playService.stopAudioBroadcast(deviceId, channelId);
	}

	@Operation(summary = "获取所有的ssrc", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@GetMapping("/ssrc")
	public JSONObject getSSRC() {
		if (logger.isDebugEnabled()) {
			logger.debug("获取所有的ssrc");
		}
		JSONArray objects = new JSONArray();
		List<SsrcTransaction> allSsrc = streamSession.getAllSsrc();
		for (SsrcTransaction transaction : allSsrc) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("deviceId", transaction.getDeviceId());
			jsonObject.put("channelId", transaction.getChannelId());
			jsonObject.put("ssrc", transaction.getSsrc());
			jsonObject.put("streamId", transaction.getStream());
			objects.add(jsonObject);
		}

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("data", objects);
		jsonObject.put("count", objects.size());
		return jsonObject;
	}

	@Operation(summary = "获取截图", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "isSubStream", description = "是否子码流（true-子码流，false-主码流），默认为false", required = true)
	@GetMapping("/snap")
	public DeferredResult<String> getSnap(String deviceId, String channelId,boolean isSubStream) {
		if (logger.isDebugEnabled()) {
			logger.debug("获取截图: {}/{}", deviceId, channelId);
		}

		DeferredResult<String> result = new DeferredResult<>(3 * 1000L);
		String key  = DeferredResultHolder.CALLBACK_CMD_SNAP + deviceId;
		String uuid  = UUID.randomUUID().toString();
		resultHolder.put(key, uuid,  result);

		RequestMessage message = new RequestMessage();
		message.setKey(key);
		message.setId(uuid);

		String fileName = deviceId + "_" + channelId + "_" + DateUtil.getNowForUrl() + ".jpg";
		playService.getSnap(deviceId, channelId, fileName, (code, msg, data) -> {
			if (code == InviteErrorCode.SUCCESS.getCode()) {
				message.setData(data);
			}else {
				message.setData(WVPResult.fail(code, msg));
			}
			resultHolder.invokeResult(message);
		});
		return result;
	}

}

