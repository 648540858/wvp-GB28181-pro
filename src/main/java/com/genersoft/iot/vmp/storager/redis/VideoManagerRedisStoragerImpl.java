package com.genersoft.iot.vmp.storager.redis;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;

/**    
 * @Description:视频设备数据存储-redis实现  
 * @author: songww
 * @date:   2020年5月6日 下午2:31:42     
 */
@Component("redisStorager")
public class VideoManagerRedisStoragerImpl implements IVideoManagerStorager {

	@Autowired
    private RedisUtil redis;
	
	/**   
	 * 根据设备ID判断设备是否存在
	 * 
	 * @param deviceId 设备ID
	 * @return true:存在  false：不存在
	 */ 
	@Override
	public boolean exists(String deviceId) {
		return redis.hasKey(VideoManagerConstants.CACHEKEY_PREFIX+deviceId);
	}

	/**   
	 * 视频设备创建
	 * 
	 * @param device 设备对象
	 * @return true：创建成功  false：创建失败
	 */ 
	@Override
	public boolean create(Device device) {
		return redis.set(VideoManagerConstants.CACHEKEY_PREFIX+device.getDeviceId(), device);
	}
	
	/**   
	 * 视频设备更新
	 * 
	 * @param device 设备对象
	 * @return true：更新成功  false：更新失败
	 */  
	@Override
	public boolean update(Device device) {
		return redis.set(VideoManagerConstants.CACHEKEY_PREFIX+device.getDeviceId(), device);
	}

	/**   
	 * 获取设备
	 * 
	 * @param deviceId 设备ID
	 * @return Device 设备对象
	 */  
	@Override
	public Device queryVideoDevice(String deviceId) {
		return (Device)redis.get(VideoManagerConstants.CACHEKEY_PREFIX+deviceId);
	}

	/**   
	 * 获取多个设备
	 * 
	 * @param deviceIds 设备ID数组
	 * @return List<Device> 设备对象数组
	 */  
	@Override
	public List<Device> queryVideoDeviceList(String[] deviceIds) {
		List<Device> devices = new ArrayList<>();
		if (deviceIds == null || deviceIds.length == 0) {
			List<Object> deviceIdList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX+"*");
			for (int i = 0; i < deviceIdList.size(); i++) {
				devices.add((Device)redis.get((String)deviceIdList.get(i)));
			}
		} else {
			for (int i = 0; i < deviceIds.length; i++) {
				devices.add((Device)redis.get(VideoManagerConstants.CACHEKEY_PREFIX+deviceIds[i]));
			}
		}
		return devices;
	}

	/**   
	 * 删除设备
	 * 
	 * @param deviceId 设备ID
	 * @return true：删除成功  false：删除失败
	 */  
	@Override
	public boolean delete(String deviceId) {
		redis.del(VideoManagerConstants.CACHEKEY_PREFIX+deviceId);
		return true;  
	}

	/**   
	 * 更新设备在线
	 * 
	 * @param deviceId 设备ID
	 * @return true：更新成功  false：更新失败
	 */ 
	@Override
	public boolean online(String deviceId) {
		Device device = (Device)redis.get(VideoManagerConstants.CACHEKEY_PREFIX+deviceId);
		device.setOnline(1);
		return redis.set(VideoManagerConstants.CACHEKEY_PREFIX+device.getDeviceId(), device);
	}

	/**   
	 * 更新设备离线
	 * 
	 * @param deviceId 设备ID
	 * @return true：更新成功  false：更新失败
	 */ 
	@Override
	public boolean outline(String deviceId) {
		Device device = (Device)redis.get(VideoManagerConstants.CACHEKEY_PREFIX+deviceId);
		device.setOnline(0);
		return redis.set(VideoManagerConstants.CACHEKEY_PREFIX+device.getDeviceId(), device);
	}
	
}
