package com.genersoft.iot.vmp.storager.redis;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.PageResult;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;

/**    
 * @Description:视频设备数据存储-redis实现  
 * @author: swwheihei
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
		return redis.hasKey(VideoManagerConstants.DEVICE_PREFIX+deviceId);
	}

	/**   
	 * 视频设备创建
	 * 
	 * @param device 设备对象
	 * @return true：创建成功  false：创建失败
	 */ 
	@Override
	public boolean create(Device device) {
		return redis.set(VideoManagerConstants.DEVICE_PREFIX+device.getDeviceId(), device);
	}



	/**   
	 * 视频设备更新
	 * 
	 * @param device 设备对象
	 * @return true：更新成功  false：更新失败
	 */  
	@Override
	public boolean updateDevice(Device device) {
		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + device.getDeviceId() + "_" + "*");
		// 更新device中的通道数量
		device.setChannelCount(deviceChannelList.size());
		// 存储device
		return redis.set(VideoManagerConstants.DEVICE_PREFIX+device.getDeviceId(), device);


	}

	@Override
	public void updateChannel(String deviceId, DeviceChannel channel) {
		// 存储通道
		redis.set(VideoManagerConstants.CACHEKEY_PREFIX+deviceId + "_" + channel.getChannelId(),
				channel);
		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
		// 更新device中的通道数量
		Device device = (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceId);
		device.setChannelCount(deviceChannelList.size());
		redis.set(VideoManagerConstants.DEVICE_PREFIX+device.getDeviceId(), device);
	}

	/**   
	 * 获取设备
	 * 
	 * @param deviceId 设备ID
	 * @return Device 设备对象
	 */  
	@Override
	public Device queryVideoDevice(String deviceId) {
		return (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceId);
	}

	@Override
	public PageResult queryChannelsByDeviceId(String deviceId, int page, int count) {
		List<DeviceChannel> result = new ArrayList<>();
		PageResult pageResult = new PageResult<DeviceChannel>();
		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
		pageResult.setPage(page);
		pageResult.setCount(count);
		pageResult.setTotal(deviceChannelList.size());
		int maxCount = (page + 1 ) * count;
		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
			for (int i = page * count; i < (pageResult.getTotal() > maxCount ? maxCount : pageResult.getTotal() ); i++) {
				result.add((DeviceChannel)redis.get((String)deviceChannelList.get(i)));
			}
			pageResult.setData(result);
		}

		return pageResult;
	}

	@Override
	public List<DeviceChannel> queryChannelsByDeviceId(String deviceId) {
		List<DeviceChannel> result = new ArrayList<>();
		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
			for (int i = 0; i < deviceChannelList.size(); i++) {
				result.add((DeviceChannel)redis.get((String)deviceChannelList.get(i)));
			}
		}
		return result;
	}

	@Override
	public DeviceChannel queryChannel(String deviceId, String channelId) {
		return (DeviceChannel)redis.get(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + channelId);
	}


	/**
	 * 获取多个设备
	 * 
	 * @param deviceIds 设备ID数组
	 * @return List<Device> 设备对象数组
	 */  
	@Override
	public PageResult<Device> queryVideoDeviceList(String[] deviceIds, int page, int count) {
		List<Device> devices = new ArrayList<>();
		PageResult pageResult = new PageResult<Device>();
		pageResult.setPage(page);
		pageResult.setCount(count);

		if (deviceIds == null || deviceIds.length == 0) {

			List<Object> deviceIdList = redis.keys(VideoManagerConstants.DEVICE_PREFIX+"*");
			pageResult.setTotal(deviceIdList.size());
			int maxCount = (page + 1)* count;
			for (int i = page * count; i < (pageResult.getTotal() > maxCount ? maxCount : pageResult.getTotal() ); i++) {
				devices.add((Device)redis.get((String)deviceIdList.get(i)));
			}
		} else {
			for (int i = 0; i < deviceIds.length; i++) {
				devices.add((Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceIds[i]));
			}
		}
		pageResult.setData(devices);
		return pageResult;
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
			List<Object> deviceIdList = redis.keys(VideoManagerConstants.DEVICE_PREFIX+"*");
			for (int i = 0; i < deviceIdList.size(); i++) {
				devices.add((Device)redis.get((String)deviceIdList.get(i)));
			}
		} else {
			for (int i = 0; i < deviceIds.length; i++) {
				devices.add((Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceIds[i]));
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
		return redis.del(VideoManagerConstants.DEVICE_PREFIX+deviceId);
	}

	/**   
	 * 更新设备在线
	 * 
	 * @param deviceId 设备ID
	 * @return true：更新成功  false：更新失败
	 */ 
	@Override
	public boolean online(String deviceId) {
		Device device = (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceId);
		device.setOnline(1);
		return redis.set(VideoManagerConstants.DEVICE_PREFIX+device.getDeviceId(), device);
	}

	/**   
	 * 更新设备离线
	 * 
	 * @param deviceId 设备ID
	 * @return true：更新成功  false：更新失败
	 */ 
	@Override
	public boolean outline(String deviceId) {
		Device device = (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceId);
		if (device == null) return false;
		device.setOnline(0);
		return redis.set(VideoManagerConstants.DEVICE_PREFIX+device.getDeviceId(), device);
	}

	/**
	 * 开始播放时将流存入redis
	 *
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 * @return
	 */
	@Override
	public boolean startPlay(String deviceId, String channelId, StreamInfo stream) {
		return redis.set(String.format("%S_%s_%s", VideoManagerConstants.PLAYER_PREFIX, deviceId, channelId),
				stream);
	}

	/**
	 * 停止播放时从redis删除
	 *
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 * @return
	 */
	@Override
	public boolean stopPlay(String deviceId, String channelId) {
		return redis.del(String.format("%S_%s_%s", VideoManagerConstants.PLAYER_PREFIX, deviceId, channelId));
	}

	/**
	 * 查询播放列表
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 * @return
	 */
	@Override
	public StreamInfo queryPlay(String deviceId, String channelId) {
		return (StreamInfo)redis.get(String.format("%S_%s_%s", VideoManagerConstants.PLAYER_PREFIX, deviceId, channelId));
	}

	/**
	 * 更新流媒体信息
	 * @param mediaServerConfig
	 * @return
	 */
	@Override
	public boolean updateMediaInfo(MediaServerConfig mediaServerConfig) {
		return redis.set(VideoManagerConstants.MEDIA_SERVER_PREFIX,mediaServerConfig);
	}

	/**
	 * 获取流媒体信息
	 * @return
	 */
	@Override
	public MediaServerConfig getMediaInfo() {
		return (MediaServerConfig)redis.get(VideoManagerConstants.MEDIA_SERVER_PREFIX);
	}
}
