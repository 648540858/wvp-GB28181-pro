package com.genersoft.iot.vmp.media.zlm.event;

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

/**
 *
 */
@Component
public class ZLMOfflineEventListener implements ApplicationListener<ZLMOfflineEvent> {

	private final static Logger logger = LoggerFactory.getLogger(ZLMOfflineEventListener.class);

	@Autowired
    private IMediaServerService mediaServerService;

	@Autowired
    private IStreamPushService streamPushService;

	@Autowired
    private IStreamProxyService streamProxyService;

	@Override
	public void onApplicationEvent(ZLMOfflineEvent event) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("ZLM离线事件触发，ID：" + event.getMediaServerId());
		}
		// 处理ZLM离线
		mediaServerService.zlmServerOffline(event.getMediaServerId());
		streamProxyService.zlmServerOffline(event.getMediaServerId());
		streamPushService.zlmServerOffline(event.getMediaServerId());
		// TODO 处理对国标的影响
	}
}
