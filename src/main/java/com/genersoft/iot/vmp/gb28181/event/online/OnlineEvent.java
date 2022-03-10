package com.genersoft.iot.vmp.gb28181.event.online;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import org.springframework.context.ApplicationEvent;

/**    
 * @description: 在线事件类   
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

	private Device device;
	
	private String from;

	private int  expires;

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public int getExpires() {
		return expires;
	}

	public void setExpires(int expires) {
		this.expires = expires;
	}
}
