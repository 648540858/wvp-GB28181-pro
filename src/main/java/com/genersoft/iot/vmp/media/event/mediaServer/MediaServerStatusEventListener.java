package com.genersoft.iot.vmp.media.event.mediaServer;

import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


/**
 * @description: 在线事件监听器，监听到离线后，修改设备离在线状态。 设备在线有两个来源：
 *               1、设备主动注销，发送注销指令
 *               2、设备未知原因离线，心跳超时
 * @author: swwheihei
 * @date: 2020年5月6日 下午1:51:23
 */
@Component
public class MediaServerStatusEventListener {
	
	private final static Logger logger = LoggerFactory.getLogger(MediaServerStatusEventListener.class);

	@Autowired
	private IStreamPushService streamPushService;

	@Autowired
	private IStreamProxyService streamProxyService;

	@Autowired
	private IPlayService playService;

	@Async("taskExecutor")
	@EventListener
	public void onApplicationEvent(MediaServerOnlineEvent event) {
		logger.info("[媒体节点] 上线 ID：" + event.getMediaServerId());
		streamPushService.zlmServerOnline(event.getMediaServerId());
		streamProxyService.zlmServerOnline(event.getMediaServerId());
		playService.zlmServerOnline(event.getMediaServerId());
	}

	@Async("taskExecutor")
	@EventListener
	public void onApplicationEvent(MediaServerOfflineEvent event) {

		logger.info("[媒体节点] 离线，ID：" + event.getMediaServerId());
		// 处理ZLM离线
		streamProxyService.zlmServerOffline(event.getMediaServerId());
		streamPushService.zlmServerOffline(event.getMediaServerId());
		playService.zlmServerOffline(event.getMediaServerId());
	}
}
