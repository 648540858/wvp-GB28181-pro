package com.genersoft.iot.vmp.storager.redis;

import java.util.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.PageResult;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
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
						"_" + (channel.getStatus() == 1 ? "on":"off") +
						"_" + (channelMap.get(channelId).size() > 0)+
						"_" + (StringUtils.isEmpty(channel.getParentId())?null:channel.getParentId()),
				channel);
		// 更新device中的通道数量
		Device device = (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceId);
		device.setChannelCount(deviceMap.get(deviceId).size());
		redis.set(VideoManagerConstants.DEVICE_PREFIX+device.getDeviceId(), device);


		// 如果有父设备,更新父设备内子节点数
		String parentId = channel.getParentId();
		if (!StringUtils.isEmpty(parentId) && !parentId.equals(deviceId)) {

			if (channelMap.get(parentId) == null) {
				channelMap.put(parentId, new HashSet<String>());
			}
			channelMap.get(parentId).add(channelId);

			DeviceChannel deviceChannel = queryChannel(deviceId, parentId);
			if (deviceChannel != null) {
				deviceChannel.setSubCount(channelMap.get(parentId).size());
				redis.set(VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
								"_" + deviceChannel.getChannelId() +
								"_" + (deviceChannel.getStatus() == 1 ? "on":"off") +
								"_" + (channelMap.get(deviceChannel.getChannelId()).size() > 0)+
								"_" + (StringUtils.isEmpty(deviceChannel.getParentId())?null:deviceChannel.getParentId()),
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
		// 获取到所有正在播放的流
		Map<String, StreamInfo> stringStreamInfoMap = queryPlayByDeviceId(deviceId);
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
//		List<Object> deviceChannelList = redis.keys(queryStr);
		List<Object> deviceChannelList = redis.scan(queryStr);
		//对查询结果排序，避免出现通道排列顺序乱序的情况
		Collections.sort(deviceChannelList,new Comparator<Object>(){
			@Override
			public int compare(Object o1, Object o2) {
				return o1.toString().compareToIgnoreCase(o2.toString());
			}
		});
		pageResult.setPage(page);
		pageResult.setCount(count);
		pageResult.setTotal(deviceChannelList.size());
		int maxCount = (page + 1 ) * count;
		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
			for (int i = page * count; i < (pageResult.getTotal() > maxCount ? maxCount : pageResult.getTotal() ); i++) {
				DeviceChannel deviceChannel = (DeviceChannel)redis.get((String)deviceChannelList.get(i));
				StreamInfo streamInfo = stringStreamInfoMap.get(deviceId + "_" + deviceChannel.getChannelId());
				deviceChannel.setPlay(streamInfo != null);
				if (streamInfo != null) deviceChannel.setSsrc(streamInfo.getSsrc());
				result.add(deviceChannel);
			}
			pageResult.setData(result);
		}

		return pageResult;
	}



	@Override
	public List<DeviceChannel> queryChannelsByDeviceId(String deviceId) {
		List<DeviceChannel> result = new ArrayList<>();
//		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
		List<Object> deviceChannelList = redis.scan(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");

		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
			for (int i = 0; i < deviceChannelList.size(); i++) {
				result.add((DeviceChannel)redis.get((String) deviceChannelList.get(i)));
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

//		List<Object> deviceChannelList = redis.keys(queryStr);
		List<Object> deviceChannelList = redis.scan(queryStr);

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
//		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
		List<Object> deviceChannelList = redis.scan(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");

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
		DeviceChannel deviceChannel = null;
//		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
		List<Object> deviceChannelList = redis.scan(VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
				"_" + channelId  + "*");
		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
			deviceChannel = (DeviceChannel)redis.get((String)deviceChannelList.get(0));
		}
		return deviceChannel;
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
		Device device = null;

		if (deviceIds == null || deviceIds.length == 0) {

//			List<Object> deviceIdList = redis.keys(VideoManagerConstants.DEVICE_PREFIX+"*");
			List<Object> deviceIdList = redis.scan(VideoManagerConstants.DEVICE_PREFIX+"*");
			pageResult.setTotal(deviceIdList.size());
			int maxCount = (page + 1)* count;
			for (int i = page * count; i < (pageResult.getTotal() > maxCount ? maxCount : pageResult.getTotal() ); i++) {
				// devices.add((Device)redis.get((String)deviceIdList.get(i)));
				device =(Device)redis.get((String)deviceIdList.get(i));
				if (redis.scan(VideoManagerConstants.KEEPLIVEKEY_PREFIX+device.getDeviceId()).size() == 0){
					// outline(device.getDeviceId());
				}
				devices.add(device);
			}
		} else {
			for (int i = 0; i < deviceIds.length; i++) {
				// devices.add((Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceIds[i]));
				device = (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceIds[i]);
				if (redis.scan(VideoManagerConstants.KEEPLIVEKEY_PREFIX+device.getDeviceId()).size() == 0){
					// outline(device.getDeviceId());
				}
				devices.add(device);
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
		Device device = null;

		if (deviceIds == null || deviceIds.length == 0) {
//			List<Object> deviceIdList = redis.keys(VideoManagerConstants.DEVICE_PREFIX+"*");
			List<Object> deviceIdList = redis.scan(VideoManagerConstants.DEVICE_PREFIX+"*");
			for (int i = 0; i < deviceIdList.size(); i++) {
				device =(Device)redis.get((String)deviceIdList.get(i));
				if (redis.scan(VideoManagerConstants.KEEPLIVEKEY_PREFIX+device.getDeviceId()).size() == 0){
					outline(device.getDeviceId());
				}
				devices.add(device);
			}
		} else {
			for (int i = 0; i < deviceIds.length; i++) {
				device = (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceIds[i]);
				if (redis.scan(VideoManagerConstants.KEEPLIVEKEY_PREFIX+device.getDeviceId()).size() == 0){
					outline(device.getDeviceId());
				}
				devices.add(device);
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
		if (streamInfo == null) return false;
		DeviceChannel deviceChannel = queryChannel(streamInfo.getDeviceID(), streamInfo.getCahnnelId());
		if (deviceChannel != null) {
			deviceChannel.setSsrc(null);
			deviceChannel.setPlay(false);
			updateChannel(streamInfo.getDeviceID(), deviceChannel);
		}
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
//		List<Object> playLeys = redis.keys(String.format("%S_%s_*", VideoManagerConstants.PLAYER_PREFIX, ssrc));
		List<Object> playLeys = redis.scan(String.format("%S_%s_*", VideoManagerConstants.PLAYER_PREFIX, ssrc));
		if (playLeys == null || playLeys.size() == 0) return null;
		return (StreamInfo)redis.get(playLeys.get(0).toString());
	}

	@Override
	public StreamInfo queryPlaybackBySSRC(String ssrc) {
//		List<Object> playLeys = redis.keys(String.format("%S_%s_*", VideoManagerConstants.PLAYER_PREFIX, ssrc));
		List<Object> playLeys = redis.scan(String.format("%S_%s_*", VideoManagerConstants.PLAY_BLACK_PREFIX, ssrc));
		if (playLeys == null || playLeys.size() == 0) return null;
		return (StreamInfo)redis.get(playLeys.get(0).toString());
	}

	@Override
	public StreamInfo queryPlayByDevice(String deviceId, String code) {
//		List<Object> playLeys = redis.keys(String.format("%S_*_%s_%s", VideoManagerConstants.PLAYER_PREFIX,
		List<Object> playLeys = redis.scan(String.format("%S_*_%s_%s", VideoManagerConstants.PLAYER_PREFIX,
				deviceId,
				code));
		if (playLeys == null || playLeys.size() == 0) return null;
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
			List<Object> deviceChannelList = redis.scan(VideoManagerConstants.CACHEKEY_PREFIX +
					device.getDeviceId() + "_" + "*");
			if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
				for (int i = 0; i < deviceChannelList.size(); i++) {
					String key = (String)deviceChannelList.get(i);
					String[] s = key.split("_");
					String channelId = s[3];
					HashSet<String> subChannel = channelMap.get(channelId);
					if (subChannel == null) {
						subChannel = new HashSet<>();
					}
					System.out.println(key);
					if (s.length == 6 && !"null".equals(s[5])) {
						subChannel.add(s[5]);
					}
					channelMap.put(channelId, subChannel);
				}
			}
			deviceMap.put(device.getDeviceId(),channelMap);
		}
		System.out.println();
	}

	@Override
	public void cleanChannelsForDevice(String deviceId) {
		List<DeviceChannel> result = new ArrayList<>();
//		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
		List<Object> deviceChannelList = redis.scan(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
			for (int i = 0; i < deviceChannelList.size(); i++) {
				redis.del((String)deviceChannelList.get(i));
			}
		}
	}

	@Override
	public Map<String, StreamInfo> queryPlayByDeviceId(String deviceId) {
		Map<String, StreamInfo> streamInfos = new HashMap<>();
//		List<Object> playLeys = redis.keys(String.format("%S_*_%S_*", VideoManagerConstants.PLAYER_PREFIX, deviceId));
		List<Object> playLeys = redis.scan(String.format("%S_*_%S_*", VideoManagerConstants.PLAYER_PREFIX, deviceId));
		if (playLeys.size() == 0) return streamInfos;
		for (int i = 0; i < playLeys.size(); i++) {
			String key = (String) playLeys.get(i);
			StreamInfo streamInfo = (StreamInfo)redis.get(key);
			streamInfos.put(streamInfo.getDeviceID() + "_" + streamInfo.getCahnnelId(), streamInfo);
		}
		return streamInfos;
	}


	@Override
	public boolean startPlayback(StreamInfo stream) {
		return redis.set(String.format("%S_%s_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX, stream.getSsrc(),stream.getDeviceID(), stream.getCahnnelId()),
				stream);
	}


	@Override
	public boolean stopPlayback(StreamInfo streamInfo) {
		if (streamInfo == null) return false;
		DeviceChannel deviceChannel = queryChannel(streamInfo.getDeviceID(), streamInfo.getCahnnelId());
		if (deviceChannel != null) {
			deviceChannel.setSsrc(null);
			deviceChannel.setPlay(false);
			updateChannel(streamInfo.getDeviceID(), deviceChannel);
		}
		return redis.del(String.format("%S_%s_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
				streamInfo.getSsrc(),
				streamInfo.getDeviceID(),
				streamInfo.getCahnnelId()));
	}

	@Override
	public StreamInfo queryPlaybackByDevice(String deviceId, String code) {
		String format = String.format("%S_*_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
				deviceId,
				code);
		List<Object> playLeys = redis.scan(String.format("%S_*_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
				deviceId,
				code));
		if (playLeys == null || playLeys.size() == 0) {
			playLeys = redis.scan(String.format("%S_*_*_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
				deviceId));
		}
		if (playLeys == null || playLeys.size() == 0) return null;
		return (StreamInfo)redis.get(playLeys.get(0).toString());
	}

	@Override
	public boolean updateParentPlatform(ParentPlatform parentPlatform) {

		// 存储device
		return redis.set(VideoManagerConstants.PLATFORM_PREFIX + parentPlatform.getDeviceGBId(), parentPlatform);
	}

	@Override
	public boolean deleteParentPlatform(ParentPlatform parentPlatform) {
		return redis.del(VideoManagerConstants.PLATFORM_PREFIX + parentPlatform.getDeviceGBId());
	}

	@Override
	public PageResult<ParentPlatform> queryParentPlatformList(int page, int count) {
		PageResult pageResult = new PageResult<Device>();
		pageResult.setPage(page);
		pageResult.setCount(count);
		List<ParentPlatform> resultData = new ArrayList<>();
		List<Object> parentPlatformList = redis.scan(VideoManagerConstants.PLATFORM_PREFIX + "*");
		pageResult.setTotal(parentPlatformList.size());
		int maxCount = (page + 1)* count;
		for (int i = page * count; i < (pageResult.getTotal() > maxCount ? maxCount : pageResult.getTotal() ); i++) {
			ParentPlatform parentPlatform =(ParentPlatform)redis.get((String)parentPlatformList.get(i));
			resultData.add(parentPlatform);

		}
		pageResult.setData(resultData);
		return pageResult;
	}

	@Override
	public ParentPlatform queryParentPlatById(String platformGbId) {
		return (ParentPlatform)redis.get(VideoManagerConstants.PLATFORM_PREFIX + platformGbId);
	}
}
