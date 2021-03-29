package com.genersoft.iot.vmp.gb28181.transmit.callback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

/**    
 * @Description: 异步请求处理
 * @author: swwheihei
 * @date:   2020年5月8日 下午7:59:05     
 */
@Component
public class DeferredResultHolder {
	
	public static final String CALLBACK_CMD_DEVICESTATUS = "CALLBACK_DEVICESTATUS";
	
	public static final String CALLBACK_CMD_DEVICEINFO = "CALLBACK_DEVICEINFO";
	
	public static final String CALLBACK_CMD_DEVICECONTROL = "CALLBACK_DEVICECONTROL";
	
	public static final String CALLBACK_CMD_DEVICECONFIG = "CALLBACK_DEVICECONFIG";

	public static final String CALLBACK_CMD_CONFIGDOWNLOAD = "CALLBACK_CONFIGDOWNLOAD";
	
	public static final String CALLBACK_CMD_CATALOG = "CALLBACK_CATALOG";
	
	public static final String CALLBACK_CMD_RECORDINFO = "CALLBACK_RECORDINFO";

	public static final String CALLBACK_CMD_PlAY = "CALLBACK_PLAY";

	public static final String CALLBACK_CMD_STOP = "CALLBACK_STOP";

	public static final String CALLBACK_CMD_MOBILEPOSITION = "CALLBACK_MOBILEPOSITION";

	public static final String CALLBACK_CMD_PRESETQUERY = "CALLBACK_PRESETQUERY";

	public static final String CALLBACK_CMD_ALARM = "CALLBACK_ALARM";

	public static final String CALLBACK_CMD_BROADCAST = "CALLBACK_BROADCAST";

	private Map<String, DeferredResult> map = new ConcurrentHashMap<String, DeferredResult>();

	public void put(String key, DeferredResult result) {
		map.put(key, result);
	}

	public void invokeResult(RequestMessage msg) {
//		DeferredResult result = map.get(msg.getId());
		// 获取并移除
		DeferredResult result = map.remove(msg.getId());
		if (result == null) {
			return;
		}
		result.setResult(new ResponseEntity<>(msg.getData(),HttpStatus.OK));
	}
}
