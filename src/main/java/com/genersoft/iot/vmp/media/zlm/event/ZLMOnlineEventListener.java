package com.genersoft.iot.vmp.media.zlm.event;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

/**
 * @description: 在线事件监听器，监听到离线后，修改设备离在线状态。 设备在线有两个来源：
 *               1、设备主动注销，发送注销指令
 *               2、设备未知原因离线，心跳超时
 * @author: swwheihei
 * @date: 2020年5月6日 下午1:51:23
 */
@Component
public class ZLMOnlineEventListener implements ApplicationListener<ZLMOnlineEvent> {
	
	private final static Logger logger = LoggerFactory.getLogger(ZLMOnlineEventListener.class);

	@Autowired
	private IStreamPushService streamPushService;

	@Autowired
	private IStreamProxyService streamProxyService;

	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	public void onApplicationEvent(ZLMOnlineEvent event) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("ZLM上线事件触发，ID：" + event.getMediaServerId());
		}
		streamPushService.zlmServerOnline(event.getMediaServerId());
		streamProxyService.zlmServerOnline(event.getMediaServerId());

	}
}
