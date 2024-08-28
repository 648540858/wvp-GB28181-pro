package com.genersoft.iot.vmp.storager.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.dao.*;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 视频设备数据存储-jdbc实现
 * swwheihei
 * 2020年5月6日 下午2:31:42
 */
@SuppressWarnings("rawtypes")
@Slf4j
@Component
@DS("master")
public class VideoManagerStorageImpl implements IVideoManagerStorage {


	@Autowired
    private DeviceMapper deviceMapper;

	@Autowired
	private DeviceChannelMapper deviceChannelMapper;

	@Autowired
	private DeviceMobilePositionMapper deviceMobilePositionMapper;

	@Autowired
    private PlatformMapper platformMapper;

	@Autowired
    private IRedisCatchStorage redisCatchStorage;

	@Autowired
    private PlatformChannelMapper platformChannelMapper;


	/**
	 * 查询移动位置轨迹
	 * @param deviceId
	 * @param startTime
	 * @param endTime
	 */
	@Override
	public synchronized List<MobilePosition> queryMobilePositions(String deviceId, String channelId, String startTime, String endTime) {
		return deviceMobilePositionMapper.queryPositionByDeviceIdAndTime(deviceId, channelId, startTime, endTime);
	}

	@Override
	public List<Platform> queryEnablePlatformListWithAsMessageChannel() {
		return platformMapper.queryEnablePlatformListWithAsMessageChannel();
	}

	@Override
	public Device queryVideoDeviceByPlatformIdAndChannelId(String platformId, String channelId) {
		List<Device> devices = platformChannelMapper.queryVideoDeviceByPlatformIdAndChannelId(platformId, channelId);
		if (devices.size() > 1) {
			// 出现长度大于0的时候肯定是国标通道的ID重复了
			log.warn("国标ID存在重复：{}", channelId);
		}
		if (devices.size() == 0) {
			return null;
		}else {
			return devices.get(0);
		}


	}

	@Override
	public Device queryDeviceInfoByPlatformIdAndChannelId(String platformId, String channelId) {
		List<Device> devices = platformChannelMapper.queryDeviceInfoByPlatformIdAndChannelId(platformId, channelId);
		if (devices.size() > 1) {
			// 出现长度大于0的时候肯定是国标通道的ID重复了
			log.warn("国标ID存在重复：{}", channelId);
		}
		if (devices.size() == 0) {
			return null;
		}else {
			return devices.get(0);
		}
	}

	/**
	 * 查询最新移动位置
	 * @param deviceId
	 */
	@Override
	public MobilePosition queryLatestPosition(String deviceId) {
		return deviceMobilePositionMapper.queryLatestPositionByDevice(deviceId);
	}


	@Override
	public Device queryVideoDeviceByChannelId( String channelId) {
		Device result = null;
		List<DeviceChannel> channelList = deviceChannelMapper.queryChannelByChannelId(channelId);
		if (channelList.size() == 1) {
			result = deviceMapper.getDeviceByDeviceId(channelList.get(0).getDeviceId());
		}
		return result;
	}

	@Override
	public List<Platform> queryPlatFormListForGBWithGBId(String channelId, List<String> platforms) {
		return platformChannelMapper.queryPlatFormListForGBWithGBId(channelId, platforms);
	}
}
