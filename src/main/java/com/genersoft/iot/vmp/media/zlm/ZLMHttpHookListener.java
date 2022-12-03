package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.media.zlm.dto.HookType;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.*;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**    
 * @description:针对 ZLMediaServer的hook事件监听
 * @author: swwheihei
 * @date:   2020年5月8日 上午10:46:48     
 */
@RestController
@RequestMapping("/index/hook")
public class ZLMHttpHookListener {

	private final static Logger logger = LoggerFactory.getLogger(ZLMHttpHookListener.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private SIPCommanderFroPlatform commanderFroPlatform;

	@Autowired
	private IPlayService playService;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private IStreamProxyService streamProxyService;

	@Autowired
	private IStreamPushService streamPushService;

	@Autowired
	private IMediaService mediaService;

	@Autowired
	private EventPublisher eventPublisher;

	 @Autowired
	 private ZLMMediaListManager zlmMediaListManager;

	@Autowired
	private ZlmHttpHookSubscribe subscribe;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private IUserService userService;

	@Autowired
	private VideoStreamSessionManager sessionManager;

	@Autowired
	private AssistRESTfulUtils assistRESTfulUtils;

	@Qualifier("taskExecutor")
	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	/**
	 * 服务器定时上报时间，上报间隔可配置，默认10s上报一次
	 *
	 */
	@ResponseBody
	@PostMapping(value = "/on_server_keepalive", produces = "application/json;charset=UTF-8")
	public JSONObject onServerKeepalive(@RequestBody OnServerKeepaliveHookParam param){

		logger.info("[ZLM HOOK] 收到zlm心跳：" + param.getMediaServerId());

		taskExecutor.execute(()->{
			List<ZlmHttpHookSubscribe.Event> subscribes = this.subscribe.getSubscribes(HookType.on_server_keepalive);
			JSONObject json = (JSONObject) JSON.toJSON(param);
			if (subscribes != null  && subscribes.size() > 0) {
				for (ZlmHttpHookSubscribe.Event subscribe : subscribes) {
					subscribe.response(null, json);
				}
			}
		});
		mediaServerService.updateMediaServerKeepalive(param.getMediaServerId(), param.getData());

		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");

		return ret;
	}
	
	/**
	 * 播放器鉴权事件，rtsp/rtmp/http-flv/ws-flv/hls的播放都将触发此鉴权事件。
	 *  
	 */
	@ResponseBody
	@PostMapping(value = "/on_play", produces = "application/json;charset=UTF-8")
	public JSONObject onPlay(@RequestBody OnPlayHookParam param){
		if (logger.isDebugEnabled()) {
			logger.debug("[ZLM HOOK] 播放鉴权：{}->{}" + param.getMediaServerId(), param);
		}
		String mediaServerId = param.getMediaServerId();

		taskExecutor.execute(()->{
			JSONObject json = (JSONObject) JSON.toJSON(param);
			ZlmHttpHookSubscribe.Event subscribe = this.subscribe.sendNotify(HookType.on_play, json);
			if (subscribe != null ) {
				MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);
				if (mediaInfo != null) {
					subscribe.response(mediaInfo, json);
				}
			}
		});
		JSONObject ret = new JSONObject();
		if (!"rtp".equals(param.getApp())) {
			Map<String, String> paramMap = urlParamToMap(param.getParams());
			StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(param.getApp(), param.getStream());
			if (streamAuthorityInfo != null && streamAuthorityInfo.getCallId() != null && !streamAuthorityInfo.getCallId().equals(paramMap.get("callId"))) {
				ret.put("code", 401);
				ret.put("msg", "Unauthorized");
				return ret;
			}
		}

		ret.put("code", 0);
		ret.put("msg", "success");
		return ret;
	}
	
	/**
	 * rtsp/rtmp/rtp推流鉴权事件。
	 *  
	 */
	@ResponseBody
	@PostMapping(value = "/on_publish", produces = "application/json;charset=UTF-8")
	public JSONObject onPublish(@RequestBody OnPublishHookParam param) {

		JSONObject json = (JSONObject) JSON.toJSON(param);

		logger.info("[ZLM HOOK]推流鉴权：{}->{}",  param.getMediaServerId(), param);
		JSONObject ret = new JSONObject();
		String mediaServerId = json.getString("mediaServerId");
		MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);

		if (!"rtp".equals(param.getApp())) {
			if (userSetting.getPushAuthority()) {
				// 推流鉴权
				if (param.getParams() == null) {
					logger.info("推流鉴权失败： 缺少不要参数：sign=md5(user表的pushKey)");
					ret.put("code", 401);
					ret.put("msg", "Unauthorized");
					return ret;
				}
				Map<String, String> paramMap = urlParamToMap(param.getParams());
				String sign = paramMap.get("sign");
				if (sign == null) {
					logger.info("推流鉴权失败： 缺少不要参数：sign=md5(user表的pushKey)");
					ret.put("code", 401);
					ret.put("msg", "Unauthorized");
					return ret;
				}
				// 推流自定义播放鉴权码
				String callId = paramMap.get("callId");
				// 鉴权配置
				boolean hasAuthority = userService.checkPushAuthority(callId, sign);
				if (!hasAuthority) {
					logger.info("推流鉴权失败： sign 无权限: callId={}. sign={}", callId, sign);
					ret.put("code", 401);
					ret.put("msg", "Unauthorized");
					return ret;
				}
				StreamAuthorityInfo streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(param);
				streamAuthorityInfo.setCallId(callId);
				streamAuthorityInfo.setSign(sign);
				// 鉴权通过
				redisCatchStorage.updateStreamAuthorityInfo(param.getApp(), param.getStream(), streamAuthorityInfo);
				// 通知assist新的callId
				if (mediaInfo != null && mediaInfo.getRecordAssistPort() > 0) {
					taskExecutor.execute(()->{
						assistRESTfulUtils.addStreamCallInfo(mediaInfo, param.getApp(), param.getStream(), callId, null);
					});
				}
			}
		}else {
			zlmMediaListManager.sendStreamEvent(param.getApp(),param.getStream(), param.getMediaServerId());
		}

		ret.put("code", 0);
		ret.put("msg", "success");

		if (!"rtp".equals(param.getApp())) {
			ret.put("enable_audio", true);
		}

		taskExecutor.execute(()->{
			ZlmHttpHookSubscribe.Event subscribe = this.subscribe.sendNotify(HookType.on_publish, json);
			if (subscribe != null) {
				if (mediaInfo != null) {
					subscribe.response(mediaInfo, json);
				}else {
					ret.put("code", 1);
					ret.put("msg", "zlm not register");
				}
			}
		});

		if ("rtp".equals(param.getApp())) {
			ret.put("enable_mp4", userSetting.getRecordSip());
		}else {
			ret.put("enable_mp4", userSetting.isRecordPushLive());
		}
		List<SsrcTransaction> ssrcTransactionForAll = sessionManager.getSsrcTransactionForAll(null, null, null, param.getStream());
		if (ssrcTransactionForAll != null && ssrcTransactionForAll.size() == 1) {
			String deviceId = ssrcTransactionForAll.get(0).getDeviceId();
			String channelId = ssrcTransactionForAll.get(0).getChannelId();
			DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
			if (deviceChannel != null) {
				ret.put("enable_audio", deviceChannel.isHasAudio());
			}
			// 如果是录像下载就设置视频间隔十秒
			if (ssrcTransactionForAll.get(0).getType() == VideoStreamSessionManager.SessionType.download) {
				ret.put("mp4_max_second", 10);
				ret.put("enable_mp4", true);
				ret.put("enable_audio", true);
			}
		}
		return ret;
	}
	
	/**
	 * rtsp/rtmp流注册或注销时触发此事件；此事件对回复不敏感。
	 *  
	 */
	@ResponseBody
	@PostMapping(value = "/on_stream_changed", produces = "application/json;charset=UTF-8")
	public JSONObject onStreamChanged(@RequestBody OnStreamChangedHookParam param){

		if (param.isRegist()) {
			logger.info("[ZLM HOOK] 流注册, {}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());
		}else {
			logger.info("[ZLM HOOK] 流注销, {}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());
		}


		JSONObject json = (JSONObject) JSON.toJSON(param);
		taskExecutor.execute(()->{
			ZlmHttpHookSubscribe.Event subscribe = this.subscribe.sendNotify(HookType.on_stream_changed, json);
			if (subscribe != null ) {
				MediaServerItem mediaInfo = mediaServerService.getOne(param.getMediaServerId());
				if (mediaInfo != null) {
					subscribe.response(mediaInfo, json);
				}
			}
			// 流消失移除redis play
			List<OnStreamChangedHookParam.MediaTrack> tracks = param.getTracks();
			if (param.isRegist()) {
				if (param.getOriginType() == OriginType.RTMP_PUSH.ordinal()
						|| param.getOriginType() == OriginType.RTSP_PUSH.ordinal()
						|| param.getOriginType() == OriginType.RTC_PUSH.ordinal()) {

					StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(param.getApp(), param.getStream());
					if (streamAuthorityInfo == null) {
						streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(param);
					}else {
						streamAuthorityInfo.setOriginType(param.getOriginType());
						streamAuthorityInfo.setOriginTypeStr(param.getOriginTypeStr());
					}
					redisCatchStorage.updateStreamAuthorityInfo(param.getApp(), param.getStream(), streamAuthorityInfo);
				}
			}else {
				redisCatchStorage.removeStreamAuthorityInfo(param.getApp(), param.getStream());
			}

			if ("rtsp".equals(param.getSchema())){
				if (param.isRegist()) {
					mediaServerService.addCount(param.getMediaServerId());
				}else {
					mediaServerService.removeCount(param.getMediaServerId());
				}
				if (param.getOriginType() == OriginType.PULL.ordinal()
						|| param.getOriginType() == OriginType.FFMPEG_PULL.ordinal()) {
					// 设置拉流代理上线/离线
					streamProxyService.updateStatus(param.isRegist(), param.getApp(), param.getStream());
				}
				if ("rtp".equals(param.getApp()) && !param.isRegist() ) {
					StreamInfo streamInfo = redisCatchStorage.queryPlayByStreamId(param.getStream());
					if (streamInfo!=null){
						redisCatchStorage.stopPlay(streamInfo);
						storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
					}else{
						streamInfo = redisCatchStorage.queryPlayback(null, null, param.getStream(), null);
						if (streamInfo != null) {
							redisCatchStorage.stopPlayback(streamInfo.getDeviceID(), streamInfo.getChannelId(),
									streamInfo.getStream(), null);
						}
					}
				}else {
					if (!"rtp".equals(param.getApp())){
						String type = OriginType.values()[param.getOriginType()].getType();
						MediaServerItem mediaServerItem = mediaServerService.getOne(param.getMediaServerId());

						if (mediaServerItem != null){
							if (param.isRegist()) {
								StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(param.getApp(), param.getStream());
								String callId = null;
								if (streamAuthorityInfo != null) {
									callId = streamAuthorityInfo.getCallId();
								}
								StreamInfo streamInfoByAppAndStream = mediaService.getStreamInfoByAppAndStream(mediaServerItem,
										param.getApp(), param.getStream(), tracks, callId);
								param.setStreamInfo(streamInfoByAppAndStream);
								redisCatchStorage.addStream(mediaServerItem, type, param.getApp(), param.getStream(), param);
								if (param.getOriginType() == OriginType.RTSP_PUSH.ordinal()
										|| param.getOriginType() == OriginType.RTMP_PUSH.ordinal()
										|| param.getOriginType() == OriginType.RTC_PUSH.ordinal() ) {
									param.setSeverId(userSetting.getServerId());
									zlmMediaListManager.addPush(param);
								}
							}else {
								// 兼容流注销时类型从redis记录获取
								OnStreamChangedHookParam onStreamChangedHookParam = redisCatchStorage.getStreamInfo(param.getApp(), param.getStream(), param.getMediaServerId());
								if (onStreamChangedHookParam != null) {
									type = OriginType.values()[onStreamChangedHookParam.getOriginType()].getType();
									redisCatchStorage.removeStream(mediaServerItem.getId(), type, param.getApp(), param.getStream());
								}
								GbStream gbStream = storager.getGbStream(param.getApp(), param.getStream());
								if (gbStream != null) {
//								eventPublisher.catalogEventPublishForStream(null, gbStream, CatalogEvent.OFF);
								}
								zlmMediaListManager.removeMedia(param.getApp(), param.getStream());
							}
							if (type != null) {
								// 发送流变化redis消息
								JSONObject jsonObject = new JSONObject();
								jsonObject.put("serverId", userSetting.getServerId());
								jsonObject.put("app", param.getApp());
								jsonObject.put("stream", param.getStream());
								jsonObject.put("register", param.isRegist());
								jsonObject.put("mediaServerId", param.getMediaServerId());
								redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
							}
						}
					}
				}
				if (!param.isRegist()) {
					List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByStream(param.getStream());
					if (sendRtpItems.size() > 0) {
						for (SendRtpItem sendRtpItem : sendRtpItems) {
							if (sendRtpItem.getApp().equals(param.getApp())) {
								String platformId = sendRtpItem.getPlatformId();
								ParentPlatform platform = storager.queryParentPlatByServerGBId(platformId);
								Device device = deviceService.getDevice(platformId);

								try {
									if (platform != null) {
										commanderFroPlatform.streamByeCmd(platform, sendRtpItem);
									}else {
										cmder.streamByeCmd(device, sendRtpItem.getChannelId(), param.getStream(), sendRtpItem.getCallId());
									}
								} catch (SipException | InvalidArgumentException | ParseException | SsrcTransactionNotFoundException e) {
									logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
								}
							}
						}
					}
				}
			}
		});

		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");
		return ret;
	}
	
	/**
	 * 流无人观看时事件，用户可以通过此事件选择是否关闭无人看的流。
	 *  
	 */
	@ResponseBody
	@PostMapping(value = "/on_stream_none_reader", produces = "application/json;charset=UTF-8")
	public JSONObject onStreamNoneReader(@RequestBody OnStreamNoneReaderHookParam param){

		logger.info("[ZLM HOOK]流无人观看：{]->{}->{}/{}" + param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());
		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		// 录像下载
		if ("rtp".equals(param.getApp())){
			ret.put("close", userSetting.getStreamOnDemand());
			// 国标流， 点播/录像回放/录像下载
			StreamInfo streamInfoForPlayCatch = redisCatchStorage.queryPlayByStreamId(param.getStream());
			// 点播
			if (streamInfoForPlayCatch != null) {
				// 收到无人观看说明流也没有在往上级推送
				if (redisCatchStorage.isChannelSendingRTP(streamInfoForPlayCatch.getChannelId())) {
					List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByChnnelId(streamInfoForPlayCatch.getChannelId());
					if (sendRtpItems.size() > 0) {
						for (SendRtpItem sendRtpItem : sendRtpItems) {
							ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(sendRtpItem.getPlatformId());
							try {
								commanderFroPlatform.streamByeCmd(parentPlatform, sendRtpItem.getCallId());
							} catch (SipException | InvalidArgumentException | ParseException e) {
								logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
							}
							redisCatchStorage.deleteSendRTPServer(parentPlatform.getServerGBId(), sendRtpItem.getChannelId(),
									sendRtpItem.getCallId(), sendRtpItem.getStreamId());
						}
					}
				}
				Device device = deviceService.getDevice(streamInfoForPlayCatch.getDeviceID());
				if (device != null) {
					try {
						cmder.streamByeCmd(device, streamInfoForPlayCatch.getChannelId(),
								streamInfoForPlayCatch.getStream(), null);
					} catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
						logger.error("[无人观看]点播， 发送BYE失败 {}", e.getMessage());
					}
				}

				redisCatchStorage.stopPlay(streamInfoForPlayCatch);
				storager.stopPlay(streamInfoForPlayCatch.getDeviceID(), streamInfoForPlayCatch.getChannelId());
				return ret;
			}
			// 录像回放
			StreamInfo streamInfoForPlayBackCatch = redisCatchStorage.queryPlayback(null, null, param.getStream(), null);
			if (streamInfoForPlayBackCatch != null ) {
				if (streamInfoForPlayBackCatch.isPause()) {
					ret.put("close", false);
				}else {
					Device device = deviceService.getDevice(streamInfoForPlayBackCatch.getDeviceID());
					if (device != null) {
						try {
							cmder.streamByeCmd(device,streamInfoForPlayBackCatch.getChannelId(),
									streamInfoForPlayBackCatch.getStream(), null);
						} catch (InvalidArgumentException | ParseException | SipException |
								 SsrcTransactionNotFoundException e) {
							logger.error("[无人观看]回放， 发送BYE失败 {}", e.getMessage());
						}
					}
					redisCatchStorage.stopPlayback(streamInfoForPlayBackCatch.getDeviceID(),
							streamInfoForPlayBackCatch.getChannelId(), streamInfoForPlayBackCatch.getStream(), null);
				}
				return ret;
			}
			// 录像下载
			StreamInfo streamInfoForDownload = redisCatchStorage.queryDownload(null, null, param.getStream(), null);
			// 进行录像下载时无人观看不断流
			if (streamInfoForDownload != null) {
				ret.put("close", false);
				return ret;
			}
		}else {
			// 非国标流 推流/拉流代理
			// 拉流代理
			StreamProxyItem streamProxyItem = streamProxyService.getStreamProxyByAppAndStream(param.getApp(), param.getStream());
			if (streamProxyItem != null ) {
				if (streamProxyItem.isEnable_remove_none_reader()) {
					// 无人观看自动移除
					ret.put("close", true);
					streamProxyService.del(param.getApp(), param.getStream());
					String url = streamProxyItem.getUrl() != null?streamProxyItem.getUrl():streamProxyItem.getSrc_url();
					logger.info("[{}/{}]<-[{}] 拉流代理无人观看已经移除",  param.getApp(), param.getStream(), url);
				}else if (streamProxyItem.isEnable_disable_none_reader()) {
					// 无人观看停用
					ret.put("close", true);
					// 修改数据
					streamProxyService.stop(param.getApp(), param.getStream());
				}else {
					// 无人观看不做处理
					ret.put("close", false);
				}
				return ret;
			}
			// 推流具有主动性，暂时不做处理
//			StreamPushItem streamPushItem = streamPushService.getPush(app, streamId);
//			if (streamPushItem != null) {
//				// TODO 发送停止
//
//			}
		}
		return ret;
	}
	
	/**
	 * 流未找到事件，用户可以在此事件触发时，立即去拉流，这样可以实现按需拉流；此事件对回复不敏感。
	 *  
	 */
	@ResponseBody
	@PostMapping(value = "/on_stream_not_found", produces = "application/json;charset=UTF-8")
	public JSONObject onStreamNotFound(@RequestBody OnStreamNotFoundHookParam param){
		logger.info("[ZLM HOOK] 流未找到：{}->{}->{}/{}" + param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());
		taskExecutor.execute(()->{
			MediaServerItem mediaInfo = mediaServerService.getOne(param.getMediaServerId());
			if (userSetting.isAutoApplyPlay() && mediaInfo != null) {
				if ("rtp".equals(param.getApp())) {
					if (mediaInfo.isRtpEnable()) {
						String[] s = param.getStream().split("_");
						if (s.length == 2) {
							String deviceId = s[0];
							String channelId = s[1];
							Device device = redisCatchStorage.getDevice(deviceId);
							if (device != null) {
								playService.play(mediaInfo,deviceId, channelId, null, null, null);
							}
						}
					}
				}else {
					// 拉流代理
					StreamProxyItem streamProxyByAppAndStream = streamProxyService.getStreamProxyByAppAndStream(param.getApp(), param.getStream());
					if (streamProxyByAppAndStream != null && streamProxyByAppAndStream.isEnable_disable_none_reader()) {
						streamProxyService.start(param.getApp(), param.getStream());
					}
				}
			}
		});


		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");
		return ret;
	}
	
	/**
	 * 服务器启动事件，可以用于监听服务器崩溃重启；此事件对回复不敏感。
	 *  
	 */
	@ResponseBody
	@PostMapping(value = "/on_server_started", produces = "application/json;charset=UTF-8")
	public JSONObject onServerStarted(HttpServletRequest request, @RequestBody JSONObject jsonObject){

		jsonObject.put("ip", request.getRemoteAddr());
		ZLMServerConfig zlmServerConfig = JSON.to(ZLMServerConfig.class, jsonObject);
		zlmServerConfig.setIp(request.getRemoteAddr());
		logger.info("[ZLM HOOK] zlm 启动 " + zlmServerConfig.getGeneralMediaServerId());
		taskExecutor.execute(()->{
			List<ZlmHttpHookSubscribe.Event> subscribes = this.subscribe.getSubscribes(HookType.on_server_started);
			if (subscribes != null  && subscribes.size() > 0) {
				for (ZlmHttpHookSubscribe.Event subscribe : subscribes) {
					subscribe.response(null, jsonObject);
				}
			}
			mediaServerService.zlmServerOnline(zlmServerConfig);
		});

		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");
		return ret;
	}

	/**
	 * 发送rtp(startSendRtp)被动关闭时回调
	 */
	@ResponseBody
	@PostMapping(value = "/on_send_rtp_stopped", produces = "application/json;charset=UTF-8")
	public JSONObject onSendRtpStopped(HttpServletRequest request, @RequestBody OnSendRtpStoppedHookParam param){

		logger.info("[ZLM HOOK] 发送rtp被动关闭：{}->{}/{}", param.getMediaServerId(), param.getApp(), param.getStream());

		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");

		// 查找对应的上级推流，发送停止
		if (!"rtp".equals(param.getApp())) {
			return ret;
		}
		taskExecutor.execute(()->{
			List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByStream(param.getStream());
			if (sendRtpItems.size() > 0) {
				for (SendRtpItem sendRtpItem : sendRtpItems) {
					ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(sendRtpItem.getPlatformId());
					try {
						commanderFroPlatform.streamByeCmd(parentPlatform, sendRtpItem.getCallId());
					} catch (SipException | InvalidArgumentException | ParseException e) {
						logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
					}
					redisCatchStorage.deleteSendRTPServer(parentPlatform.getServerGBId(), sendRtpItem.getChannelId(),
							sendRtpItem.getCallId(), sendRtpItem.getStreamId());
				}
			}
		});


		return ret;
	}

	/**
	 * rtpServer收流超时
	 */
	@ResponseBody
	@PostMapping(value = "/on_rtp_server_timeout", produces = "application/json;charset=UTF-8")
	public JSONObject onRtpServerTimeout(HttpServletRequest request, @RequestBody OnRtpServerTimeoutHookParam param){
		logger.info("[ZLM HOOK] rtpServer收流超时：{}->{}({})", param.getMediaServerId(), param.getStream_id(), param.getSsrc());

		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");

		taskExecutor.execute(()->{
			JSONObject json = (JSONObject) JSON.toJSON(param);
			List<ZlmHttpHookSubscribe.Event> subscribes = this.subscribe.getSubscribes(HookType.on_rtp_server_timeout);
			if (subscribes != null  && subscribes.size() > 0) {
				for (ZlmHttpHookSubscribe.Event subscribe : subscribes) {
					subscribe.response(null, json);
				}
			}
		});

		return ret;
	}


	/**
	 * 流量统计事件，播放器或推流器断开时并且耗用流量超过特定阈值时会触发此事件，
	 * 阈值通过配置文件general.flowThreshold配置；此事件对回复不敏感。
	 */
	@ResponseBody
	@PostMapping(value = "/on_flow_report", produces = "application/json;charset=UTF-8")
	public JSONObject onFlowReport(@RequestBody OnFlowReportHookParam param){
		logger.info("[ZLM HOOK] 流量统计：{}/{},时长:{},大小:{}" , param.getMediaServerId(), param.getStream(), param.getDuration(),param.getTotalBytes());

		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");
		return ret;
	}

	/**
	 * 访问http文件服务器上hls之外的文件时触发。
	 *
	 */
	@ResponseBody
	@PostMapping(value = "/on_http_access", produces = "application/json;charset=UTF-8")
	public JSONObject onHttpAccess(@RequestBody JSONObject jsonObject){

		logger.info("[ZLM HOOK] Http访问{},参数是:{}." ,jsonObject.getString("path"),jsonObject.getString("params"));


		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");

		return ret;
	}

	/**
	 * 录制hls完成后通知事件；此事件对回复不敏感。
	 *
	 */
	@ResponseBody
	@PostMapping(value = "/on_record_ts", produces = "application/json;charset=UTF-8")
	public JSONObject onRecordTs(@RequestBody OnRecordTsHookParam param){
		logger.info("[ZLM HOOK] 发送TS切片：{}/{}/{}/{}" , param.getMediaServerId(), param.getApp(), param.getStream(), param.getFile_name());

		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");
		return ret;
	}

	/**
	 * 录制MP4完成后通知事件；此事件对回复不敏感。
	 *
	 */
	@ResponseBody
	@PostMapping(value = "/on_record_mp4", produces = "application/json;charset=UTF-8")
	public JSONObject onRecordMp4(@RequestBody OnRecordMp4HookParam param){
		logger.info("[ZLM HOOK] 发送Mp4：{}/{}/{}/{}" , param.getMediaServerId(), param.getApp(), param.getStream(), param.getFile_name());

		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");
		return ret;
	}

	/**
	 * 该rtsp流是否开启rtsp专用方式的鉴权事件，开启后才会触发on_rtsp_auth事件。
	 *
	 */
	@ResponseBody
	@PostMapping(value = "/on_rtsp_realm", produces = "application/json;charset=UTF-8")
	public JSONObject onRtspRealm(@RequestBody OnRtspRealmHookParam param){
		logger.info("[ZLM HOOK] Rtsp Realm：{}/{}/{}/{}" , param.getMediaServerId(), param.getApp(), param.getStream(), param.getParams());

		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("realm", "");
		return ret;
	}

	/**
	 * rtsp专用的鉴权事件，先触发on_rtsp_realm事件然后才会触发on_rtsp_auth事件。
	 *
	 */
	@ResponseBody
	@PostMapping(value = "/on_rtsp_auth", produces = "application/json;charset=UTF-8")
	public JSONObject onRtspAuth(@RequestBody OnRecordMp4HookParam param){
		logger.info("[ZLM HOOK] Rtsp Auth：{}/{}/{}/{}" , param.getMediaServerId(), param.getApp(), param.getStream(), param.getFile_name());

		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");
		return ret;
	}
	/**
	 * shell登录鉴权，ZLMediaKit提供简单的telnet调试方式。
	 *
	 */
	@ResponseBody
	@PostMapping(value = "/on_shell_login", produces = "application/json;charset=UTF-8")
	public JSONObject onShellLogin(@RequestBody OnShellLoginHookParam param){
		logger.info("[ZLM HOOK] Shell Login：{}/{}/{}/{}" , param.getMediaServerId(), param.getIp(), param.getUser_name(), param.getPort());

		JSONObject ret = new JSONObject();
		ret.put("code", 0);
		ret.put("msg", "success");
		return ret;
	}

	private Map<String, String> urlParamToMap(String params) {
		HashMap<String, String> map = new HashMap<>();
		if (ObjectUtils.isEmpty(params)) {
			return map;
		}
		String[] paramsArray = params.split("&");
		if (paramsArray.length == 0) {
			return map;
		}
		for (String param : paramsArray) {
			String[] paramArray = param.split("=");
			if (paramArray.length == 2){
				map.put(paramArray[0], paramArray[1]);
			}
		}
		return map;
	}
}
