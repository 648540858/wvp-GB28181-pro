package com.genersoft.iot.vmp.gb28181.transmit.callback;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

/**    
 * @Description:TODO(这里用一句话描述这个类的作用)   
 * @author: swwheihei
 * @date:   2020年5月8日 下午7:59:05     
 */
@Component
public class DeferredResultHolder {
	
	public static final String CALLBACK_CMD_DEVICEINFO = "CALLBACK_DEVICEINFO";
	
	public static final String CALLBACK_CMD_CATALOG = "CALLBACK_CATALOG";
	
	public static final String CALLBACK_CMD_RECORDINFO = "CALLBACK_RECORDINFO";

	private Map<String, DeferredResult> map = new HashMap<String, DeferredResult>();
	
	public void put(String key, DeferredResult result) {
		map.put(key, result);
	}
	
	public DeferredResult get(String key) {
		return map.get(key);
	}
	
	public void invokeResult(RequestMessage msg) {
		DeferredResult result = map.get(msg.getId());
		if (result == null) {
			return;
		}
		result.setResult(new ResponseEntity<>(msg.getData(),HttpStatus.OK));
	}
}
