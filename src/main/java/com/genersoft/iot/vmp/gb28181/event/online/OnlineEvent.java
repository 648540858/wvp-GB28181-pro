package com.genersoft.iot.vmp.gb28181.event.online;

import org.springframework.context.ApplicationEvent;

/**    
 * @Description: 在线事件类   
 * @author: swwheihei
 * @date:   2020年5月6日 上午11:32:56     
 */
public class OnlineEvent extends ApplicationEvent {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public OnlineEvent(Object source) {
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
