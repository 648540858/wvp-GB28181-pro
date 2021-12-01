package com.genersoft.iot.vmp.gb28181.transmit.callback;

/**    
 * @description: 请求信息定义   
 * @author: swwheihei
 * @date:   2020年5月8日 下午1:09:18     
 */
public class RequestMessage {
	
	private String id;

	private String key;

	private Object data;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}
