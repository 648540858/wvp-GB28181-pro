package com.genersoft.iot.vmp.gb28181.transmit.callback;

/**    
 * @Description: 请求信息定义   
 * @author: swwheihei
 * @date:   2020年5月8日 下午1:09:18     
 */
public class RequestMessage {
	
	private String id;

	private String deviceId;
	
	private String type;
	
	private Object data;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
		this.id = type + deviceId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
		this.id = type + deviceId;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}
