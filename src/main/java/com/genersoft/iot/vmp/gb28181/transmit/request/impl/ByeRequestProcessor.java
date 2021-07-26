package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.address.SipURI;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;
import javax.sip.message.Response;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**    
 * @Description: BYE请求处理器
 * @author: lawrencehj
 * @date:   2021年3月9日     
 */
public class ByeRequestProcessor extends SIPRequestAbstractProcessor {

	private Logger logger = LoggerFactory.getLogger(ByeRequestProcessor.class);

	private ISIPCommander cmder;

	private IRedisCatchStorage redisCatchStorage;

	private IVideoManagerStorager storager;

	private ZLMRTPServerFactory zlmrtpServerFactory;

	private IMediaServerService mediaServerService;

	/**
	 * 处理BYE请求
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {
		try {
			responseAck(evt);
			Dialog dialog = evt.getDialog();
			if (dialog == null) return;
			if (dialog.getState().equals(DialogState.TERMINATED)) {
				String platformGbId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
				String channelId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
				SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(platformGbId, channelId);
				logger.info("收到bye, [{}/{}]", platformGbId, channelId);
				if (sendRtpItem != null){
					String streamId = sendRtpItem.getStreamId();
					Map<String, Object> param = new HashMap<>();
					param.put("vhost","__defaultVhost__");
					param.put("app",sendRtpItem.getApp());
					param.put("stream",streamId);
					param.put("ssrc",sendRtpItem.getSsrc());
					logger.info("停止向上级推流：" + streamId);
					MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
					zlmrtpServerFactory.stopSendRtpStream(mediaInfo, param);
					redisCatchStorage.deleteSendRTPServer(platformGbId, channelId);
					if (zlmrtpServerFactory.totalReaderCount(mediaInfo, sendRtpItem.getApp(), streamId) == 0) {
						logger.info(streamId + "无其它观看者，通知设备停止推流");
						cmder.streamByeCmd(sendRtpItem.getDeviceId(), channelId);
					}
				}
				// 可能是设备主动停止
				Device device = storager.queryVideoDeviceByChannelId(platformGbId);
				if (device != null) {
					StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(device.getDeviceId(), channelId);
					if (streamInfo != null) {
						redisCatchStorage.stopPlay(streamInfo);
					}
					storager.stopPlay(device.getDeviceId(), channelId);
					mediaServerService.closeRTPServer(device, channelId);
				}
			}
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 回复200 OK
	 * @param evt
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	private void responseAck(RequestEvent evt) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(Response.OK, evt.getRequest());
		getServerTransaction(evt).sendResponse(response);
	}

	public IRedisCatchStorage getRedisCatchStorage() {
		return redisCatchStorage;
	}

	public void setRedisCatchStorage(IRedisCatchStorage redisCatchStorage) {
		this.redisCatchStorage = redisCatchStorage;
	}

	public ZLMRTPServerFactory getZlmrtpServerFactory() {
		return zlmrtpServerFactory;
	}

	public void setZlmrtpServerFactory(ZLMRTPServerFactory zlmrtpServerFactory) {
		this.zlmrtpServerFactory = zlmrtpServerFactory;
	}

	public ISIPCommander getSIPCommander() {
		return cmder;
	}

	public void setSIPCommander(ISIPCommander cmder) {
		this.cmder = cmder;
	}

	public IMediaServerService getMediaServerService() {
		return mediaServerService;
	}

	public void setMediaServerService(IMediaServerService mediaServerService) {
		this.mediaServerService = mediaServerService;
	}

	public IVideoManagerStorager getStorager() {
		return storager;
	}

	public void setStorager(IVideoManagerStorager storager) {
		this.storager = storager;
	}
}
