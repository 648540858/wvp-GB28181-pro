package com.genersoft.iot.vmp.storager.redis;

import java.util.*;

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
import org.springframework.util.StringUtils;

/**    
 * @Description:视频设备数据存储-redis实现
 * @author: swwheihei
 * @date:   2020年5月6日 下午2:31:42
 */
@Component("redisStorager")
public class VideoManagerRedisStoragerImpl implements IVideoManagerStorager {

	@Autowired
    private RedisUtil redis;

	private HashMap<String, HashMap<String, HashSet<String>>> deviceMap = new HashMap<>();


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
		if (deviceMap.get(device.getDeviceId()) == null) {
			deviceMap.put(device.getDeviceId(), new HashMap<String, HashSet<String>>());
		}
//		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + device.getDeviceId() + "_" + "*");
		// 更新device中的通道数量
		device.setChannelCount(deviceMap.get(device.getDeviceId()).size());
		// 存储device
		return redis.set(VideoManagerConstants.DEVICE_PREFIX+device.getDeviceId(), device);


	}

	@Override
	public void updateChannel(String deviceId, DeviceChannel channel) {
		String channelId = channel.getChannelId();
		HashMap<String, HashSet<String>> channelMap = deviceMap.get(deviceId);
		if (channelMap == null) return;
		// 作为父设备, 确定自己的子节点数
		if (channelMap.get(channelId) == null) {
			channelMap.put(channelId, new HashSet<String>());
		}else if (channelMap.get(channelId).size() > 0) {
			channel.setSubCount(channelMap.get(channelId).size());
		}

		// 存储通道
		redis.set(VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
						"_" + channel.getChannelId() +
						":" + channel.getName() +
						"_" + (channel.getStatus() == 1 ? "on":"off") +
						"_" + (channelMap.get(channelId).size() > 0)+
						"_" + channel.getParentId(),
				channel);
		// 更新device中的通道数量
		Device device = (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceId);
		device.setChannelCount(deviceMap.get(deviceId).size());
		redis.set(VideoManagerConstants.DEVICE_PREFIX+device.getDeviceId(), device);


		// 如果有父设备,更新父设备内子节点数
		String parentId = channel.getParentId();
		if (!StringUtils.isEmpty(parentId)) {

			if (channelMap.get(parentId) == null) {
				channelMap.put(parentId, new HashSet<String>());
			}
			channelMap.get(parentId).add(channelId);

			DeviceChannel deviceChannel = queryChannel(deviceId, parentId);
			if (deviceChannel != null) {
				deviceChannel.setSubCount(channelMap.get(parentId).size());
				redis.set(VideoManagerConstants.CACHEKEY_PREFIX+deviceId + "_" + deviceChannel.getChannelId(),
						deviceChannel);

			}
		}

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
	public PageResult queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, String online, int page, int count) {
		List<DeviceChannel> result = new ArrayList<>();
		PageResult pageResult = new PageResult<DeviceChannel>();
		String queryContent = "*";
		if (!StringUtils.isEmpty(query)) queryContent = String.format("*%S*",query);
		String queryHasSubChannel = "*";
		if (hasSubChannel != null) queryHasSubChannel = hasSubChannel?"true":"false";
		String queryOnline = "*";
		if (!StringUtils.isEmpty(online)) queryOnline = online;
		String queryStr = VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
				"_" + queryContent + // 搜索编号和名称
				"_" + queryOnline + // 搜索是否在线
				"_" + queryHasSubChannel + // 搜索是否含有子节点
				"_" + "*";
		List<Object> deviceChannelList = redis.keys(queryStr);
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
	public PageResult querySubChannels(String deviceId, String parentChannelId, String query, Boolean hasSubChannel, String online, int page, int count) {
		List<DeviceChannel> allDeviceChannels = new ArrayList<>();
		String queryContent = "*";
		if (!StringUtils.isEmpty(query)) queryContent = String.format("*%S*",query);
		String queryHasSubChannel = "*";
		if (hasSubChannel != null) queryHasSubChannel = hasSubChannel?"true":"false";
		String queryOnline = "*";
		if (!StringUtils.isEmpty(online)) queryOnline = online;
		String queryStr = VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
				"_" + queryContent + // 搜索编号和名称
				"_" + queryOnline + // 搜索是否在线
				"_" + queryHasSubChannel + // 搜索是否含有子节点
				"_" + parentChannelId;

		List<Object> deviceChannelList = redis.keys(queryStr);

		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
			for (int i = 0; i < deviceChannelList.size(); i++) {
				DeviceChannel deviceChannel = (DeviceChannel)redis.get((String)deviceChannelList.get(i));
				if (deviceChannel.getParentId() != null && deviceChannel.getParentId().equals(parentChannelId)) {
					allDeviceChannels.add(deviceChannel);
				}
			}
		}
		int maxCount = (page + 1 ) * count;
		PageResult pageResult = new PageResult<DeviceChannel>();
		pageResult.setPage(page);
		pageResult.setCount(count);
		pageResult.setTotal(allDeviceChannels.size());

		if (allDeviceChannels.size() > 0) {
			pageResult.setData(allDeviceChannels.subList(
					page * count, pageResult.getTotal() > maxCount ? maxCount : pageResult.getTotal()
			));
		}
		return pageResult;
	}

	public List<DeviceChannel> querySubChannels(String deviceId, String parentChannelId) {
		List<DeviceChannel> allDeviceChannels = new ArrayList<>();
		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");

		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
			for (int i = 0; i < deviceChannelList.size(); i++) {
				DeviceChannel deviceChannel = (DeviceChannel)redis.get((String)deviceChannelList.get(i));
				if (deviceChannel.getParentId() != null && deviceChannel.getParentId().equals(parentChannelId)) {
					allDeviceChannels.add(deviceChannel);
				}
			}
		}

		return allDeviceChannels;
	}

	@Override
	public DeviceChannel queryChannel(String deviceId, String channelId) {
		return (DeviceChannel)redis.get(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + channelId + "_");
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
	 * @return
	 */
	@Override
	public boolean startPlay(StreamInfo stream) {
		return redis.set(String.format("%S_%s_%s_%s", VideoManagerConstants.PLAYER_PREFIX, stream.getSsrc(),stream.getDeviceID(), stream.getCahnnelId()),
				stream);
	}

	/**
	 * 停止播放时从redis删除
	 *
	 * @return
	 */
	@Override
	public boolean stopPlay(StreamInfo streamInfo) {
		return redis.del(String.format("%S_%s_%s_%s", VideoManagerConstants.PLAYER_PREFIX,
				streamInfo.getSsrc(),
				streamInfo.getDeviceID(),
				streamInfo.getCahnnelId()));
	}

	/**
	 * 查询播放列表
	 * @return
	 */
	@Override
	public StreamInfo queryPlay(StreamInfo streamInfo) {
		return (StreamInfo)redis.get(String.format("%S_%s_%s_%s",
				VideoManagerConstants.PLAYER_PREFIX,
				streamInfo.getSsrc(),
				streamInfo.getDeviceID(),
				streamInfo.getCahnnelId()));
	}
	@Override
	public StreamInfo queryPlayBySSRC(String ssrc) {
		List<Object> playLeys = redis.keys(String.format("%S_%s_*", VideoManagerConstants.PLAYER_PREFIX, ssrc));
		if (playLeys.size() == 0) return null;
		return (StreamInfo)redis.get(playLeys.get(0).toString());
	}

	@Override
	public StreamInfo queryPlayByDevice(String deviceId, String code) {
		List<Object> playLeys = redis.keys(String.format("%S_*_%s_%s", VideoManagerConstants.PLAYER_PREFIX,
				deviceId,
				code));
		return (StreamInfo)redis.get(playLeys.get(0).toString());
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

	@Override
	public void updateCatch() {
		deviceMap = new HashMap<>();
		// 更新设备
		List<Device> devices = queryVideoDeviceList(null);
		if (devices == null && devices.size() == 0) return;
		for (Device device : devices) {
			// 更新设备下的通道
			HashMap<String, HashSet<String>> channelMap = new HashMap<String, HashSet<String>>();
			List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX +
					device.getDeviceId() + "_" + "*");
			if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
				for (int i = 0; i < deviceChannelList.size(); i++) {
					String key = (String)deviceChannelList.get(i);
					String[] s = key.split("_");
					String channelId = s[3].split(":")[0];
					HashSet<String> subChannel = channelMap.get(channelId);
					if (subChannel == null) {
						subChannel = new HashSet<>();
					}
					if (!"null".equals(s[6])) {
						subChannel.add(s[6]);
					}
					channelMap.put(channelId, subChannel);
				}
			}
			deviceMap.put(device.getDeviceId(),channelMap);
		}
	}

	@Override
	public void cleanChannelsForDevice(String deviceId) {
		List<DeviceChannel> result = new ArrayList<>();
		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
			for (int i = 0; i < deviceChannelList.size(); i++) {
				redis.del((String)deviceChannelList.get(i));
			}
		}
	}



}
