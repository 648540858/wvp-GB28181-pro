package com.genersoft.iot.vmp.gb28181.event.offline;

import org.springframework.context.ApplicationEvent;

/**    
 * @description: 离线事件类   
 * @author: swwheihei
 * @date:   2020年5月6日 上午11:33:13     
 */
public class OfflineEvent extends ApplicationEvent {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public OfflineEvent(Object source) {
		super(source);
	}

	private String deviceId;
	
	private String from;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}
}
