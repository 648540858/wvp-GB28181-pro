package com.genersoft.iot.vmp.media.event.mediaServer;

import com.genersoft.iot.vmp.service.IPlayService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class MediaServerStatusEventListener {
	
	@Autowired
	private IPlayService playService;

	@Async("taskExecutor")
	@EventListener
	public void onApplicationEvent(MediaServerOnlineEvent event) {
		log.info("[媒体节点] 上线 ID：" + event.getMediaServerId());
		playService.zlmServerOnline(event.getMediaServerId());
	}

	@Async("taskExecutor")
	@EventListener
	public void onApplicationEvent(MediaServerOfflineEvent event) {

		log.info("[媒体节点] 离线，ID：" + event.getMediaServerId());
		// 处理ZLM离线
		playService.zlmServerOffline(event.getMediaServerId());
	}
}
