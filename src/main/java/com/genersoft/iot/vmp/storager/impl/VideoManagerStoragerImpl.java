package com.genersoft.iot.vmp.storager.impl;

import java.util.*;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
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
	public synchronized boolean create(Device device) {
		return deviceMapper.add(device) > 0;
	}



	/**
	 * 视频设备更新
	 *
	 * @param device 设备对象
	 * @return true：更新成功  false：更新失败
	 */
	@Override
	public synchronized boolean updateDevice(Device device) {
		Device deviceByDeviceId = deviceMapper.getDeviceByDeviceId(device.getDeviceId());
		if (deviceByDeviceId == null) {
			return deviceMapper.add(device) > 0;
		}else {
			return deviceMapper.update(device) > 0;
		}

	}

	@Override
	public synchronized void updateChannel(String deviceId, DeviceChannel channel) {
		String channelId = channel.getChannelId();
		channel.setDeviceId(deviceId);
		DeviceChannel deviceChannel = deviceChannelMapper.queryChannel(deviceId, channelId);
		if (deviceChannel == null) {
			deviceChannelMapper.add(channel);
		}else {
			deviceChannelMapper.update(channel);
		}
	}

	@Override
	public void startPlay(String deviceId, String channelId, String streamId) {
		deviceChannelMapper.startPlay(deviceId, channelId, streamId);
	}

	@Override
	public void stopPlay(String deviceId, String channelId) {
		deviceChannelMapper.stopPlay(deviceId, channelId);
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
	public PageInfo queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, Boolean online, int page, int count) {
		// 获取到所有正在播放的流
		PageHelper.startPage(page, count);
		List<DeviceChannel> all = deviceChannelMapper.queryChannelsByDeviceId(deviceId, null, query, hasSubChannel, online);
		return new PageInfo<>(all);
	}

	@Override
	public List<DeviceChannel> queryChannelsByDeviceId(String deviceId) {
		return deviceChannelMapper.queryChannelsByDeviceId(deviceId, null,null, null, null);
	}

	@Override
	public PageInfo<DeviceChannel> querySubChannels(String deviceId, String parentChannelId, String query, Boolean hasSubChannel, String online, int page, int count) {
		PageHelper.startPage(page, count);
		List<DeviceChannel> all = deviceChannelMapper.queryChannelsByDeviceId(deviceId, parentChannelId, null, null, null);
		return new PageInfo<>(all);
	}

	@Override
	public DeviceChannel queryChannel(String deviceId, String channelId) {
		return deviceChannelMapper.queryChannel(deviceId, channelId);
	}


	/**
	 * 获取多个设备
	 *
	 * @param page 当前页数
	 * @param count 每页数量
	 * @return PageInfo<Device> 分页设备对象数组
	 */
	@Override
	public PageInfo<Device> queryVideoDeviceList(int page, int count) {
		PageHelper.startPage(page, count);
		List<Device> all = deviceMapper.getDevices();
		return new PageInfo<>(all);
	}

	/**
	 * 获取多个设备
	 *
	 * @return List<Device> 设备对象数组
	 */
	@Override
	public List<Device> queryVideoDeviceList() {

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
	public synchronized boolean online(String deviceId) {
		Device device = deviceMapper.getDeviceByDeviceId(deviceId);
		device.setOnline(1);
		System.out.println("更新设备在线");
		if (device == null) {
			return false;
		}
		return deviceMapper.update(device) > 0;
	}

	/**
	 * 更新设备离线
	 *
	 * @param deviceId 设备ID
	 * @return true：更新成功  false：更新失败
	 */
	@Override
	public synchronized boolean outline(String deviceId) {
		Device device = deviceMapper.getDeviceByDeviceId(deviceId);
		device.setOnline(0);
		System.out.println("更新设备离线");
		return deviceMapper.update(device) > 0;
	}


	@Override
	public void cleanChannelsForDevice(String deviceId) {
		int result = deviceChannelMapper.cleanChannelsByDeviceId(deviceId);
	}


}
