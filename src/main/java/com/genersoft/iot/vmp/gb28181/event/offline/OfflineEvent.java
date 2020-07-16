package com.genersoft.iot.vmp.gb28181.event.offline;

import org.springframework.context.ApplicationEvent;

/**    
 * @Description:TODO(这里用一句话描述这个类的作用)   
 * @author: songww
 * @date:   2020年5月6日 上午11:33:13     
 */
public class OfflineEvent extends ApplicationEvent {
	
	/**   
	 * @Title:  OutlineEvent   
	 * @Description:    TODO(这里用一句话描述这个方法的作用)   
	 * @param:  @param source  
	 * @throws   
	 */  
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
