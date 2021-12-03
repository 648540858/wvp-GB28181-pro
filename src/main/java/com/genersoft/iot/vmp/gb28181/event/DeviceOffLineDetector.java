package com.genersoft.iot.vmp.gb28181.event;

import com.genersoft.iot.vmp.conf.UserSetup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;

/**    
 * @description:设备离在线状态检测器，用于检测设备状态
 * @author: swwheihei
 * @date:   2020年5月13日 下午2:40:29     
 */
@Component
public class DeviceOffLineDetector {

	@Autowired
    private RedisUtil redis;

	@Autowired
    private UserSetup userSetup;
	
	public boolean isOnline(String deviceId) {
		String key = VideoManagerConstants.KEEPLIVEKEY_PREFIX + userSetup.getServerId() + "_" + deviceId;
		return redis.hasKey(key);
	}
}
