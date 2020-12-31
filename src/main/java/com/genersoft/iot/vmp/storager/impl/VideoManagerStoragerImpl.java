package com.genersoft.iot.vmp.storager.impl;

import java.util.*;

import com.genersoft.iot.vmp.common.PageResult;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.springframework.util.StringUtils;

/**    
 * @Description:视频设备数据存储-jdbc实现
 * @author: swwheihei
 * @date:   2020年5月6日 下午2:31:42
 */
@Component
public class VideoManagerStoragerImpl implements IVideoManagerStorager {

	@Autowired
    private DeviceMapper deviceMapper;

	@Autowired
    private DeviceChannelMapper deviceChannelMapper;


	/**
	 * 根据设备ID判断设备是否存在
	 *
	 * @param deviceId 设备ID
	 * @return true:存在  false：不存在
	 */
	@Override
	public boolean exists(String deviceId) {
		return deviceMapper.getDeviceByDeviceId(deviceId) != null;
	}

	/**
	 * 视频设备创建
	 *
	 * @param device 设备对象
	 * @return true：创建成功  false：创建失败
	 */
	@Override
	public boolean create(Device device) {
		return deviceMapper.add(device) > 0;
	}



	/**
	 * 视频设备更新
	 *
	 * @param device 设备对象
	 * @return true：更新成功  false：更新失败
	 */
	@Override
	public boolean updateDevice(Device device) {
//		if (deviceMap.get(device.getDeviceId()) == null) {
//			deviceMap.put(device.getDeviceId(), new HashMap<String, HashSet<String>>());
//		}
		// 更新device中的通道数量
//		device.setChannelCount(deviceMap.get(device.getDeviceId()).size());
		int result = deviceMapper.update(device);
		// 存储device
		return result > 0;


	}

	@Override
	public void updateChannel(String deviceId, DeviceChannel channel) {
		String channelId = channel.getChannelId();
		channel.setDeviceId(deviceId);
		deviceChannelMapper.update(channel);

//		HashMap<String, HashSet<String>> channelMap = deviceMap.get(deviceId);
//		if (channelMap == null) return;
//		// 作为父设备, 确定自己的子节点数
//		if (channelMap.get(channelId) == null) {
//			channelMap.put(channelId, new HashSet<String>());
//		}else if (channelMap.get(channelId).size() > 0) {
//			channel.setSubCount(channelMap.get(channelId).size());
//		}
//
//		// 存储通道
//		redis.set(VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
//						"_" + channel.getChannelId() +
//						"_" + (channel.getStatus() == 1 ? "on":"off") +
//						"_" + (channelMap.get(channelId).size() > 0)+
//						"_" + (StringUtils.isEmpty(channel.getParentId())?null:channel.getParentId()),
//				channel);
//		// 更新device中的通道数量
//		Device device = (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceId);
//		device.setChannelCount(deviceMap.get(deviceId).size());
//		redis.set(VideoManagerConstants.DEVICE_PREFIX+device.getDeviceId(), device);
//
//
//		// 如果有父设备,更新父设备内子节点数
//		String parentId = channel.getParentId();
//		if (!StringUtils.isEmpty(parentId) && !parentId.equals(deviceId)) {
//
//			if (channelMap.get(parentId) == null) {
//				channelMap.put(parentId, new HashSet<String>());
//			}
//			channelMap.get(parentId).add(channelId);
//
//			DeviceChannel deviceChannel = queryChannel(deviceId, parentId);
//			if (deviceChannel != null) {
//				deviceChannel.setSubCount(channelMap.get(parentId).size());
//				redis.set(VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
//								"_" + deviceChannel.getChannelId() +
//								"_" + (deviceChannel.getStatus() == 1 ? "on":"off") +
//								"_" + (channelMap.get(deviceChannel.getChannelId()).size() > 0)+
//								"_" + (StringUtils.isEmpty(deviceChannel.getParentId())?null:deviceChannel.getParentId()),
//						deviceChannel);
//
//			}
//		}

	}

	/**
	 * 获取设备
	 *
	 * @param deviceId 设备ID
	 * @return Device 设备对象
	 */
	@Override
	public Device queryVideoDevice(String deviceId) {
		return deviceMapper.getDeviceByDeviceId(deviceId);
	}

	@Override
	public PageResult queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, String online, int page, int count) {
		// 获取到所有正在播放的流
		List<DeviceChannel> result = new ArrayList<>();
		PageResult pageResult = new PageResult<DeviceChannel>();

		deviceChannelMapper.queryChannelsByDeviceId(deviceId);
//		String queryContent = "*";
//		if (!StringUtils.isEmpty(query)) queryContent = String.format("*%S*",query);
//		String queryHasSubChannel = "*";
//		if (hasSubChannel != null) queryHasSubChannel = hasSubChannel?"true":"false";
//		String queryOnline = "*";
//		if (!StringUtils.isEmpty(online)) queryOnline = online;
//		String queryStr = VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
//				"_" + queryContent + // 搜索编号和名称
//				"_" + queryOnline + // 搜索是否在线
//				"_" + queryHasSubChannel + // 搜索是否含有子节点
//				"_" + "*";
//		List<Object> deviceChannelList = redis.scan(queryStr);
//		//对查询结果排序，避免出现通道排列顺序乱序的情况
//		Collections.sort(deviceChannelList,new Comparator<Object>(){
//			@Override
//			public int compare(Object o1, Object o2) {
//				return o1.toString().compareToIgnoreCase(o2.toString());
//			}
//		});
//		pageResult.setPage(page);
//		pageResult.setCount(count);
//		pageResult.setTotal(deviceChannelList.size());
//		int maxCount = (page + 1 ) * count;
//		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
//			for (int i = page * count; i < (pageResult.getTotal() > maxCount ? maxCount : pageResult.getTotal() ); i++) {
//				DeviceChannel deviceChannel = (DeviceChannel)redis.get((String)deviceChannelList.get(i));
//				StreamInfo streamInfo = stringStreamInfoMap.get(deviceId + "_" + deviceChannel.getChannelId());
//				deviceChannel.setPlay(streamInfo != null);
//				if (streamInfo != null) deviceChannel.setStreamId(streamInfo.getStreamId());
//				result.add(deviceChannel);
//			}
//			pageResult.setData(result);
//		}

		return pageResult;
	}



	@Override
	public List<DeviceChannel> queryChannelsByDeviceId(String deviceId) {
//		List<DeviceChannel> result = new ArrayList<>();
////		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
//		List<Object> deviceChannelList = redis.scan(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
//
//		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
//			for (int i = 0; i < deviceChannelList.size(); i++) {
//				result.add((DeviceChannel)redis.get((String) deviceChannelList.get(i)));
//			}
//		}
		return deviceChannelMapper.queryChannelsByDeviceId(deviceId);
	}

	@Override
	public PageResult querySubChannels(String deviceId, String parentChannelId, String query, Boolean hasSubChannel, String online, int page, int count) {

		deviceChannelMapper.queryChannelsByDeviceId(deviceId, parentChannelId);

//		List<DeviceChannel> allDeviceChannels = new ArrayList<>();
//		String queryContent = "*";
//		if (!StringUtils.isEmpty(query)) queryContent = String.format("*%S*",query);
//		String queryHasSubChannel = "*";
//		if (hasSubChannel != null) queryHasSubChannel = hasSubChannel?"true":"false";
//		String queryOnline = "*";
//		if (!StringUtils.isEmpty(online)) queryOnline = online;
//		String queryStr = VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
//				"_" + queryContent + // 搜索编号和名称
//				"_" + queryOnline + // 搜索是否在线
//				"_" + queryHasSubChannel + // 搜索是否含有子节点
//				"_" + parentChannelId;
//
////		List<Object> deviceChannelList = redis.keys(queryStr);
//		List<Object> deviceChannelList = redis.scan(queryStr);
//
//		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
//			for (int i = 0; i < deviceChannelList.size(); i++) {
//				DeviceChannel deviceChannel = (DeviceChannel)redis.get((String)deviceChannelList.get(i));
//				if (deviceChannel.getParentId() != null && deviceChannel.getParentId().equals(parentChannelId)) {
//					allDeviceChannels.add(deviceChannel);
//				}
//			}
//		}
//		int maxCount = (page + 1 ) * count;
		PageResult pageResult = new PageResult<DeviceChannel>();
//		pageResult.setPage(page);
//		pageResult.setCount(count);
//		pageResult.setTotal(allDeviceChannels.size());
//
//		if (allDeviceChannels.size() > 0) {
//			pageResult.setData(allDeviceChannels.subList(
//					page * count, pageResult.getTotal() > maxCount ? maxCount : pageResult.getTotal()
//			));
//		}
		return pageResult;
	}

	public List<DeviceChannel> querySubChannels(String deviceId, String parentChannelId) {
		List<DeviceChannel> allDeviceChannels = new ArrayList<>();
//		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
//		List<Object> deviceChannelList = redis.scan(VideoManagerConstants.CACHEKEY_PREFIX + deviceId + "_" + "*");
//
//		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
//			for (int i = 0; i < deviceChannelList.size(); i++) {
//				DeviceChannel deviceChannel = (DeviceChannel)redis.get((String)deviceChannelList.get(i));
//				if (deviceChannel.getParentId() != null && deviceChannel.getParentId().equals(parentChannelId)) {
//					allDeviceChannels.add(deviceChannel);
//				}
//			}
//		}

		return allDeviceChannels;
	}

	@Override
	public DeviceChannel queryChannel(String deviceId, String channelId) {
		DeviceChannel deviceChannel = null;
		return deviceChannelMapper.queryChannel(deviceId, channelId);
////		List<Object> deviceChannelList = redis.keys(VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
//		List<Object> deviceChannelList = redis.scan(VideoManagerConstants.CACHEKEY_PREFIX + deviceId +
//				"_" + channelId  + "*");
//		if (deviceChannelList != null && deviceChannelList.size() > 0 ) {
//			deviceChannel = (DeviceChannel)redis.get((String)deviceChannelList.get(0));
//		}
//		return deviceChannel;
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
//		pageResult.setPage(page);
//		pageResult.setCount(count);
//		Device device = null;
//
//		if (deviceIds == null || deviceIds.length == 0) {
//
////			List<Object> deviceIdList = redis.keys(VideoManagerConstants.DEVICE_PREFIX+"*");
//			List<Object> deviceIdList = redis.scan(VideoManagerConstants.DEVICE_PREFIX+"*");
//			pageResult.setTotal(deviceIdList.size());
//			int maxCount = (page + 1)* count;
//			for (int i = page * count; i < (pageResult.getTotal() > maxCount ? maxCount : pageResult.getTotal() ); i++) {
//				// devices.add((Device)redis.get((String)deviceIdList.get(i)));
//				device =(Device)redis.get((String)deviceIdList.get(i));
//				if (redis.scan(VideoManagerConstants.KEEPLIVEKEY_PREFIX+device.getDeviceId()).size() == 0){
//					// outline(device.getDeviceId());
//				}
//				devices.add(device);
//			}
//		} else {
//			for (int i = 0; i < deviceIds.length; i++) {
//				// devices.add((Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceIds[i]));
//				device = (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceIds[i]);
//				if (redis.scan(VideoManagerConstants.KEEPLIVEKEY_PREFIX+device.getDeviceId()).size() == 0){
//					// outline(device.getDeviceId());
//				}
//				devices.add(device);
//			}
//		}
//		pageResult.setData(devices);
		return pageResult;
	}

	/**
	 * 获取多个设备
	 *
	 * @return List<Device> 设备对象数组
	 */
	@Override
	public List<Device> queryVideoDeviceList() {

//		if (deviceIds == null || deviceIds.length == 0) {
////			List<Object> deviceIdList = redis.keys(VideoManagerConstants.DEVICE_PREFIX+"*");
//			List<Object> deviceIdList = redis.scan(VideoManagerConstants.DEVICE_PREFIX+"*");
//			for (int i = 0; i < deviceIdList.size(); i++) {
//				device =(Device)redis.get((String)deviceIdList.get(i));
//				if (redis.scan(VideoManagerConstants.KEEPLIVEKEY_PREFIX+device.getDeviceId()).size() == 0){
//					outline(device.getDeviceId());
//				}
//				devices.add(device);
//			}
//		} else {
//			for (int i = 0; i < deviceIds.length; i++) {
//				device = (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceIds[i]);
//				if (redis.scan(VideoManagerConstants.KEEPLIVEKEY_PREFIX+device.getDeviceId()).size() == 0){
//					outline(device.getDeviceId());
//				}
//				devices.add(device);
//			}
//		}

		List<Device> deviceList =  deviceMapper.getDevices();
		return deviceList;
	}

	/**
	 * 删除设备
	 *
	 * @param deviceId 设备ID
	 * @return true：删除成功  false：删除失败
	 */
	@Override
	public boolean delete(String deviceId) {
		int result = deviceMapper.del(deviceId);

		return result > 0;
	}

	/**
	 * 更新设备在线
	 *
	 * @param deviceId 设备ID
	 * @return true：更新成功  false：更新失败
	 */
	@Override
	public boolean online(String deviceId) {
		Device device = deviceMapper.getDeviceByDeviceId(deviceId);
		device.setOnline(1);
		return deviceMapper.update(device) > 0;
	}

	/**
	 * 更新设备离线
	 *
	 * @param deviceId 设备ID
	 * @return true：更新成功  false：更新失败
	 */
	@Override
	public boolean outline(String deviceId) {
//		Device device = (Device)redis.get(VideoManagerConstants.DEVICE_PREFIX+deviceId);
//		if (device == null) return false;
//		device.setOnline(0);
//		return redis.set(VideoManagerConstants.DEVICE_PREFIX+device.getDeviceId(), device);

		Device device = deviceMapper.getDeviceByDeviceId(deviceId);
		device.setOnline(0);
		return deviceMapper.update(device) > 0;
	}


	@Override
	public void cleanChannelsForDevice(String deviceId) {
		int result = deviceChannelMapper.cleanChannelsByDeviceId(deviceId);
	}


}
