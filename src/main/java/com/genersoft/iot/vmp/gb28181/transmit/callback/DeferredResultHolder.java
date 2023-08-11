package com.genersoft.iot.vmp.gb28181.transmit.callback;

import com.genersoft.iot.vmp.vmanager.bean.DeferredResultEx;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**    
 * @description: 异步请求处理
 * @author: swwheihei
 * @date:   2020年5月8日 下午7:59:05     
 */
@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Component
public class DeferredResultHolder {
	
	public static final String CALLBACK_CMD_DEVICESTATUS = "CALLBACK_DEVICESTATUS";
	
	public static final String CALLBACK_CMD_DEVICEINFO = "CALLBACK_DEVICEINFO";
	
	public static final String CALLBACK_CMD_DEVICECONTROL = "CALLBACK_DEVICECONTROL";
	
	public static final String CALLBACK_CMD_DEVICECONFIG = "CALLBACK_DEVICECONFIG";

	public static final String CALLBACK_CMD_CONFIGDOWNLOAD = "CALLBACK_CONFIGDOWNLOAD";
	
	public static final String CALLBACK_CMD_CATALOG = "CALLBACK_CATALOG";
	
	public static final String CALLBACK_CMD_RECORDINFO = "CALLBACK_RECORDINFO";

	public static final String CALLBACK_CMD_PLAY = "CALLBACK_PLAY";

	public static final String CALLBACK_CMD_PLAYBACK = "CALLBACK_PLAYBACK";

	public static final String CALLBACK_CMD_DOWNLOAD = "CALLBACK_DOWNLOAD";

	public static final String CALLBACK_CMD_PROXY = "CALLBACK_PROXY";

	public static final String CALLBACK_CMD_STOP = "CALLBACK_STOP";

	public static final String UPLOAD_FILE_CHANNEL = "UPLOAD_FILE_CHANNEL";

	public static final String CALLBACK_CMD_MOBILE_POSITION = "CALLBACK_CMD_MOBILE_POSITION";

	public static final String CALLBACK_CMD_PRESETQUERY = "CALLBACK_PRESETQUERY";

	public static final String CALLBACK_CMD_ALARM = "CALLBACK_ALARM";

	public static final String CALLBACK_CMD_BROADCAST = "CALLBACK_BROADCAST";

	public static final String CALLBACK_CMD_SNAP= "CALLBACK_SNAP";

	private Map<String, Map<String, DeferredResultEx>> map = new ConcurrentHashMap<>();


	public void put(String key, String id, DeferredResultEx result) {
		Map<String, DeferredResultEx> deferredResultMap = map.get(key);
		if (deferredResultMap == null) {
			deferredResultMap = new ConcurrentHashMap<>();
			map.put(key, deferredResultMap);
		}
		deferredResultMap.put(id, result);
	}

	public void put(String key, String id, DeferredResult result) {
		Map<String, DeferredResultEx> deferredResultMap = map.get(key);
		if (deferredResultMap == null) {
			deferredResultMap = new ConcurrentHashMap<>();
			map.put(key, deferredResultMap);
		}
		deferredResultMap.put(id, new DeferredResultEx(result));
	}
	
	public DeferredResultEx get(String key, String id) {
		Map<String, DeferredResultEx> deferredResultMap = map.get(key);
		if (deferredResultMap == null || ObjectUtils.isEmpty(id)) {
			return null;
		}
		return deferredResultMap.get(id);
	}

	public Collection<DeferredResultEx> getAllByKey(String key) {
		Map<String, DeferredResultEx> deferredResultMap = map.get(key);
		if (deferredResultMap == null) {
			return null;
		}
		return deferredResultMap.values();
	}

	public boolean exist(String key, String id){
		if (key == null) {
			return false;
		}
		Map<String, DeferredResultEx> deferredResultMap = map.get(key);
		if (id == null) {
			return deferredResultMap != null;
		}else {
			return deferredResultMap != null && deferredResultMap.get(id) != null;
		}
	}

	/**
	 * 释放单个请求
	 * @param msg
	 */
	public void invokeResult(RequestMessage msg) {
		Map<String, DeferredResultEx> deferredResultMap = map.get(msg.getKey());
		if (deferredResultMap == null) {
			return;
		}
		DeferredResultEx result = deferredResultMap.get(msg.getId());
		if (result == null) {
			return;
		}
		result.getDeferredResult().setResult(msg.getData());
		deferredResultMap.remove(msg.getId());
		if (deferredResultMap.size() == 0) {
			map.remove(msg.getKey());
		}
	}

	/**
	 * 释放所有的请求
	 * @param msg
	 */
	public void invokeAllResult(RequestMessage msg) {
		Map<String, DeferredResultEx> deferredResultMap = map.get(msg.getKey());
		if (deferredResultMap == null) {
			return;
		}
		synchronized (this) {
			deferredResultMap = map.get(msg.getKey());
			if (deferredResultMap == null) {
				return;
			}
			Set<String> ids = deferredResultMap.keySet();
			for (String id : ids) {
				DeferredResultEx result = deferredResultMap.get(id);
				if (result == null) {
					return;
				}
				if (result.getFilter() != null) {
					Object handler = result.getFilter().handler(msg.getData());
					result.getDeferredResult().setResult(handler);
				}else {
					result.getDeferredResult().setResult(msg.getData());
				}

			}
			map.remove(msg.getKey());
		}
	}


}
