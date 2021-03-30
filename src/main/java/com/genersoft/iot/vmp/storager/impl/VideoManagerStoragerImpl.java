package com.genersoft.iot.vmp.storager.impl;

import java.util.*;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyDto;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.vmanager.platform.bean.ChannelReduce;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.springframework.transaction.annotation.Transactional;

/**    
 * @Description:视频设备数据存储-jdbc实现
 * @author: swwheihei
 * @date:   2020年5月6日 下午2:31:42
 */
@SuppressWarnings("rawtypes")
@Component
public class VideoManagerStoragerImpl implements IVideoManagerStorager {

	@Autowired
    private DeviceMapper deviceMapper;

	@Autowired
	private DeviceChannelMapper deviceChannelMapper;

	@Autowired
	private DeviceMobilePositionMapper deviceMobilePositionMapper;

	@Autowired
    private ParentPlatformMapper platformMapper;

	@Autowired
    private IRedisCatchStorage redisCatchStorage;

	@Autowired
    private PatformChannelMapper patformChannelMapper;

	@Autowired
    private StreamProxyMapper streamProxyMapper;




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
		if (device == null) {
			return false;
		}
		device.setOnline(1);
		System.out.println("更新设备在线");
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

	/**
	 * 清空通道
	 * @param deviceId
	 */
	@Override
	public void cleanChannelsForDevice(String deviceId) {
		deviceChannelMapper.cleanChannelsByDeviceId(deviceId);
	}

	/**
	 * 添加Mobile Position设备移动位置
	 * @param mobilePosition
	 */
	@Override
	public synchronized boolean insertMobilePosition(MobilePosition mobilePosition) {
		return deviceMobilePositionMapper.insertNewPosition(mobilePosition) > 0;
	}

	/**
	 * 查询移动位置轨迹
	 * @param deviceId
	 * @param startTime
	 * @param endTime
	 */
	@Override
	public synchronized List<MobilePosition> queryMobilePositions(String deviceId, String startTime, String endTime) {
		return deviceMobilePositionMapper.queryPositionByDeviceIdAndTime(deviceId, startTime, endTime);
	}

	@Override
	public boolean addParentPlatform(ParentPlatform parentPlatform) {
		int result = platformMapper.addParentPlatform(parentPlatform);
		return result > 0;
	}

	@Override
	public boolean updateParentPlatform(ParentPlatform parentPlatform) {
		int result = 0;
		ParentPlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId()); // .getDeviceGBId());
		if ( platformMapper.getParentPlatById(parentPlatform.getServerGBId()) == null) {
			result = platformMapper.addParentPlatform(parentPlatform);

			if (parentPlatformCatch == null) {
				parentPlatformCatch = new ParentPlatformCatch();
				parentPlatformCatch.setParentPlatform(parentPlatform);
				parentPlatformCatch.setId(parentPlatform.getServerGBId());
			}
		}else {
			result = platformMapper.updateParentPlatform(parentPlatform);
		}
		// 更新缓存
		parentPlatformCatch.setParentPlatform(parentPlatform);
		redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
		return result > 0;
	}

	@Transactional
	@Override
	public boolean deleteParentPlatform(ParentPlatform parentPlatform) {
		int result = platformMapper.delParentPlatform(parentPlatform);
		// 删除关联的通道
		patformChannelMapper.cleanChannelForGB(parentPlatform.getServerGBId());
		return result > 0;
	}

	@Override
	public PageInfo<ParentPlatform> queryParentPlatformList(int page, int count) {
		PageHelper.startPage(page, count);
		List<ParentPlatform> all = platformMapper.getParentPlatformList();
		return new PageInfo<>(all);
	}

	@Override
	public ParentPlatform queryParentPlatById(String platformGbId) {
		return platformMapper.getParentPlatById(platformGbId);
	}

	@Override
	public List<ParentPlatform> queryEnableParentPlatformList(boolean enable) {
		return platformMapper.getEnableParentPlatformList(enable);
	}

	@Override
	public void outlineForAllParentPlatform() {
		platformMapper.outlineForAllParentPlatform();
	}


	@Override
	public PageInfo<ChannelReduce> queryAllChannelList(int page, int count, String query, Boolean online,
													   Boolean channelType, String platformId, Boolean inPlatform) {
		PageHelper.startPage(page, count);
		List<ChannelReduce> all = deviceChannelMapper.queryChannelListInAll(query, online, channelType, platformId, inPlatform);
		return new PageInfo<>(all);
	}

	@Override
	public List<ChannelReduce> queryChannelListInParentPlatform(String platformId) {

		return deviceChannelMapper.queryChannelListInAll(null, null, null, platformId, true);
	}

	@Override
	public int updateChannelForGB(String platformId, List<ChannelReduce> channelReduces) {

		Map<String, ChannelReduce> deviceAndChannels = new HashMap<>();
		for (ChannelReduce channelReduce : channelReduces) {
			deviceAndChannels.put(channelReduce.getDeviceId() + "_" + channelReduce.getChannelId(), channelReduce);
		}
		List<String> deviceAndChannelList = new ArrayList<>(deviceAndChannels.keySet());
		// 查询当前已经存在的
		List<String> relatedPlatformchannels = patformChannelMapper.findChannelRelatedPlatform(platformId, deviceAndChannelList);
		if (relatedPlatformchannels != null) {
			deviceAndChannelList.removeAll(relatedPlatformchannels);
		}
		for (String relatedPlatformchannel : relatedPlatformchannels) {
			deviceAndChannels.remove(relatedPlatformchannel);
		}
		List<ChannelReduce> channelReducesToAdd = new ArrayList<>(deviceAndChannels.values());
		// 对剩下的数据进行存储
		int result = 0;
		if (channelReducesToAdd.size() > 0) {
			result = patformChannelMapper.addChannels(platformId, channelReducesToAdd);
		}

		return result;
	}


	@Override
	public int delChannelForGB(String platformId, List<ChannelReduce> channelReduces) {

		int result = patformChannelMapper.delChannelForGB(platformId, channelReduces);

		return result;
	}

	@Override
	public DeviceChannel queryChannelInParentPlatform(String platformId, String channelId) {
		DeviceChannel channel = patformChannelMapper.queryChannelInParentPlatform(platformId, channelId);
		return channel;
	}

	@Override
	public Device queryVideoDeviceByPlatformIdAndChannelId(String platformId, String channelId) {
		Device device = patformChannelMapper.queryVideoDeviceByPlatformIdAndChannelId(platformId, channelId);
		return device;
	}

	/**
	 * 查询最新移动位置
	 * @param deviceId
	 */
	@Override
	public MobilePosition queryLatestPosition(String deviceId) {
		return deviceMobilePositionMapper.queryLatestPositionByDevice(deviceId);
	}

	/**
	 * 删除指定设备的所有移动位置
	 * @param deviceId
	 */
	public int clearMobilePositionsByDeviceId(String deviceId) {
		return deviceMobilePositionMapper.clearMobilePositionsByDeviceId(deviceId);
	}

	/**
	 * 新增代理流
	 * @param streamProxyDto
	 * @return
	 */
	@Override
	public int addStreamProxy(StreamProxyDto streamProxyDto) {
		return streamProxyMapper.add(streamProxyDto);
	}

	/**
	 * 更新代理流
	 * @param streamProxyDto
	 * @return
	 */
	@Override
	public int updateStreamProxy(StreamProxyDto streamProxyDto) {
		return streamProxyMapper.update(streamProxyDto);
	}

	/**
	 * 移除代理流
	 * @param id
	 * @return
	 */
	@Override
	public int deleteStreamProxy(String app, String stream) {
		return streamProxyMapper.del(app, stream);
	}

	/**
	 * 根据是否启用获取代理流列表
	 * @param enable
	 * @return
	 */
	@Override
	public List<StreamProxyDto> getStreamProxyListForEnable(boolean enable) {
		return streamProxyMapper.selectForEnable(enable);
	}

	/**
	 * 分页查询代理流列表
	 * @param page
	 * @param count
	 * @return
	 */
	@Override
	public PageInfo<StreamProxyDto> queryStreamProxyList(Integer page, Integer count) {
		PageHelper.startPage(page, count);
		List<StreamProxyDto> all = streamProxyMapper.selectAll();
		return new PageInfo<>(all);
	}


	/**
	 * 按照是app和stream获取代理流
	 * @param app
	 * @param stream
	 * @return
	 */
	@Override
	public StreamProxyDto queryStreamProxy(String app, String stream){
		return streamProxyMapper.selectOne(app, stream);
	}
}
