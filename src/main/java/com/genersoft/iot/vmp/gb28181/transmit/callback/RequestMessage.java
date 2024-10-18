package com.genersoft.iot.vmp.gb28181.transmit.callback;

import lombok.Data;

/**
 * @description: 请求信息定义   
 * @author: swwheihei
 * @date:   2020年5月8日 下午1:09:18     
 */
@Data
public class RequestMessage {
	
	private String id;

	private String key;

	private Object data;
}
