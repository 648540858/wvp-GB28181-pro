package com.genersoft.iot.vmp.gb28181.event.online;

import org.springframework.context.ApplicationEvent;

/**    
 * @Description:TODO(这里用一句话描述这个类的作用)   
 * @author: swwheihei
 * @date:   2020年5月6日 上午11:32:56     
 */
public class OnlineEvent extends ApplicationEvent {

	/**   
	 * @Title:  OnlineEvent   
	 * @Description:    TODO(这里用一句话描述这个方法的作用)   
	 * @param:  @param source  
	 * @throws   
	 */  
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
