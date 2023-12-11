package com.genersoft.iot.vmp.storager.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxy;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.storager.dao.dto.ChannelSourceInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import com.genersoft.iot.vmp.web.gb28181.dto.DeviceChannelExtend;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;

import java.util.*;

/**
 * 视频设备数据存储-jdbc实现
 * swwheihei
 * 2020年5月6日 下午2:31:42
 */
@SuppressWarnings("rawtypes")
@Component
public class VideoManagerStorageImpl implements IVideoManagerStorage {

	private final Logger logger = LoggerFactory.getLogger(VideoManagerStorageImpl.class);

	@Autowired
	EventPublisher eventPublisher;

	@Autowired
	SipConfig sipConfig;


	@Autowired
	TransactionDefinition transactionDefinition;

	@Autowired
	DataSourceTransactionManager dataSourceTransactionManager;

	@Autowired
    private DeviceMapper deviceMapper;

	@Autowired
	private DeviceChannelMapper deviceChannelMapper;

	@Autowired
	private DeviceMobilePositionMapper deviceMobilePositionMapper;

	@Autowired
    private ParentPlatformMapper platformMapper;

	@Autowired
    private StreamProxyMapper streamProxyMapper;

	@Autowired
    private StreamPushMapper streamPushMapper;

	@Autowired
    private GbStreamMapper gbStreamMapper;

	@Autowired
    private UserSetting userSetting;

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


	@Override
	public void deviceChannelOnline(String deviceId, String channelId) {
		deviceChannelMapper.online(deviceId, channelId);
	}

	@Override
	public void deviceChannelOffline(String deviceId, String channelId) {
		deviceChannelMapper.offline(deviceId, channelId);
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
	public PageInfo queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, Boolean online, Boolean catalogUnderDevice, int page, int count) {
		// 获取到所有正在播放的流
		PageHelper.startPage(page, count);
		List<DeviceChannel> all;
		if (catalogUnderDevice != null && catalogUnderDevice) {
			all = deviceChannelMapper.queryChannels(deviceId, deviceId, query, hasSubChannel, online,null);
			// 海康设备的parentId是SIP id
			List<DeviceChannel> deviceChannels = deviceChannelMapper.queryChannels(deviceId, sipConfig.getId(), query, hasSubChannel, online,null);
			all.addAll(deviceChannels);
		}else {
			all = deviceChannelMapper.queryChannels(deviceId, null, query, hasSubChannel, online,null);
		}
		return new PageInfo<>(all);
	}

	@Override
	public List<DeviceChannelExtend> queryChannelsByDeviceIdWithStartAndLimit(String deviceId, List<String> channelIds, String query, Boolean hasSubChannel, Boolean online, int start, int limit) {
		return deviceChannelMapper.queryChannelsByDeviceIdWithStartAndLimit(deviceId, channelIds, null, query, hasSubChannel, online, start, limit);
	}

	@Override
	public List<DeviceChannelExtend> queryChannelsByDeviceId(String deviceId, List<String> channelIds, Boolean online) {
		return deviceChannelMapper.queryChannelsWithDeviceInfo(deviceId, null,null, null, online,channelIds);
	}

	@Override
	public PageInfo<DeviceChannel> querySubChannels(String deviceId, String parentChannelId, String query, Boolean hasSubChannel, Boolean online, int page, int count) {
		PageHelper.startPage(page, count);
		List<DeviceChannel> all = deviceChannelMapper.queryChannels(deviceId, parentChannelId, query, hasSubChannel, online,null);
		return new PageInfo<>(all);
	}

	@Override
	public DeviceChannel queryChannel(String deviceId, String channelId) {
		return deviceChannelMapper.queryChannel(deviceId, channelId);
	}


	@Override
	public int delChannel(String deviceId, String channelId) {
		return deviceChannelMapper.del(deviceId, channelId);
	}

	/**
	 * 获取多个设备
	 *
	 * @param page 当前页数
	 * @param count 每页数量
	 * @return PageInfo<Device> 分页设备对象数组
	 */
	@Override
	public PageInfo<Device> queryVideoDeviceList(int page, int count,Boolean online) {
		PageHelper.startPage(page, count);
		List<Device> all = deviceMapper.getDevices(online);
		return new PageInfo<>(all);
	}

	/**
	 * 获取多个设备
	 *
	 * @return List<Device> 设备对象数组
	 */
	@Override
	public List<Device> queryVideoDeviceList(Boolean online) {

		List<Device> deviceList =  deviceMapper.getDevices(online);
		return deviceList;
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
	public synchronized List<MobilePosition> queryMobilePositions(String deviceId, String channelId, String startTime, String endTime) {
		return deviceMobilePositionMapper.queryPositionByDeviceIdAndTime(deviceId, channelId, startTime, endTime);
	}

	@Override
	public ParentPlatform queryParentPlatByServerGBId(String platformGbId) {
		return platformMapper.getParentPlatByServerGBId(platformGbId);
	}

	@Override
	public List<ParentPlatform> queryEnableParentPlatformList(boolean enable) {
		return platformMapper.getEnableParentPlatformList(enable);
	}

	@Override
	public List<ParentPlatform> queryEnablePlatformListWithAsMessageChannel() {
		return platformMapper.queryEnablePlatformListWithAsMessageChannel();
	}

	@Override
	public List<Device> queryDeviceWithAsMessageChannel() {
		return deviceMapper.queryDeviceWithAsMessageChannel();
	}

	@Override
	public DeviceChannel queryChannelInParentPlatform(String platformId, String channelId) {
		List<DeviceChannel> channels = platformChannelMapper.queryChannelInParentPlatform(platformId, channelId);
		if (channels.size() > 1) {
			// 出现长度大于0的时候肯定是国标通道的ID重复了
			logger.warn("国标ID存在重复：{}", channelId);
		}
		if (channels.size() == 0) {
			return null;
		}else {
			return channels.get(0);
		}
	}

	@Override
	public Device queryVideoDeviceByPlatformIdAndChannelId(String platformId, String channelId) {
		List<Device> devices = platformChannelMapper.queryVideoDeviceByPlatformIdAndChannelId(platformId, channelId);
		if (devices.size() > 1) {
			// 出现长度大于0的时候肯定是国标通道的ID重复了
			logger.warn("国标ID存在重复：{}", channelId);
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
			logger.warn("国标ID存在重复：{}", channelId);
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

	/**
	 * 移除代理流
	 * @param app
	 * @param stream
	 * @return
	 */
	@Override
	public int deleteStreamProxy(String app, String stream) {
		return streamProxyMapper.del(app, stream);
	}

	/**
	 * 分页查询代理流列表
	 * @param page
	 * @param count
	 * @return
	 */
	@Override
	public PageInfo<StreamProxy> queryStreamProxyList(Integer page, Integer count) {
		PageHelper.startPage(page, count);
		List<StreamProxy> all = streamProxyMapper.selectAll();
		return new PageInfo<>(all);
	}

	/**
	 * 根据国标ID获取平台关联的直播流
	 * @param platformId
	 * @param gbId
	 * @return
	 */
	@Override
	public GbStream queryStreamInParentPlatform(String platformId, String gbId) {
		return gbStreamMapper.queryStreamInPlatform(platformId, gbId);
	}

	/**
	 * 获取平台关联的直播流
	 * @param platformId
	 * @return
	 */
	@Override
	public List<DeviceChannel> queryGbStreamListInPlatform(String platformId) {
		return gbStreamMapper.queryGbStreamListInPlatform(platformId, userSetting.isUsePushingAsStatus());
	}

	/**
	 * 按照是app和stream获取代理流
	 * @param app
	 * @param stream
	 * @return
	 */
	@Override
	public StreamProxy queryStreamProxy(String app, String stream){
		return streamProxyMapper.selectOne(app, stream);
	}

	@Override
	public int removeMedia(String app, String stream) {
		return streamPushMapper.del(app, stream);
	}

	@Override
	public int mediaOffline(String app, String stream) {
		GbStream gbStream = gbStreamMapper.selectOne(app, stream);
		int result;
		if ("proxy".equals(gbStream.getStreamType())) {
			result = streamProxyMapper.updateStatus(app, stream, false);
		}else {
			result = streamPushMapper.updatePushStatus(app, stream, false);
		}
		return result;
	}

	@Override
	public List<StreamProxy> getStreamProxyListForEnableInMediaServer(String id, boolean enable) {
		return streamProxyMapper.selectForEnableInMediaServer(id, enable);
	}

	@Override
	public StreamProxy getStreamProxyByAppAndStream(String app, String streamId) {
		return streamProxyMapper.selectOne(app, streamId);
	}

	@Override
	public int updateStreamGPS(List<GPSMsgInfo> gpsMsgInfos) {
		return gbStreamMapper.updateStreamGPS(gpsMsgInfos);
	}


	private DeviceChannel getDeviceChannelByCatalog(PlatformCatalog catalog) {
		ParentPlatform platform = platformMapper.getParentPlatByServerGBId(catalog.getPlatformId());
		DeviceChannel deviceChannel = new DeviceChannel();
		deviceChannel.setChannelId(catalog.getId());
		deviceChannel.setName(catalog.getName());
		deviceChannel.setDeviceId(platform.getDeviceGBId());
		deviceChannel.setManufacture("wvp-pro");
		deviceChannel.setStatus(true);
		deviceChannel.setParental(1);

		deviceChannel.setRegisterWay(1);
		deviceChannel.setParentId(catalog.getParentId());
		deviceChannel.setBusinessGroupId(catalog.getBusinessGroupId());

		deviceChannel.setModel("live");
		deviceChannel.setOwner("wvp-pro");
		deviceChannel.setSecrecy("0");
		return deviceChannel;
	}

	@Override
	public List<ParentPlatform> queryPlatFormListForGBWithGBId(String channelId, List<String> platforms) {
		return platformChannelMapper.queryPlatFormListForGBWithGBId(channelId, platforms);
	}

	@Override
	public List<ParentPlatform> queryPlatFormListForStreamWithGBId(String app, String stream, List<String> platforms) {
		if (platforms == null || platforms.size() == 0) {
			return new ArrayList<>();
		}
		return platformGbStreamMapper.queryPlatFormListForGBWithGBId(app, stream, platforms);
	}

	@Override
	public GbStream getGbStream(String app, String streamId) {
		return gbStreamMapper.selectOne(app, streamId);
	}

	@Override
	public List<ChannelSourceInfo> getChannelSource(String platformId, String gbId) {
		return platformMapper.getChannelSource(platformId, gbId);
	}

	@Override
	public void updateChannelPosition(DeviceChannel deviceChannel) {
		if (deviceChannel.getChannelId().equals(deviceChannel.getDeviceId())) {
			deviceChannel.setChannelId(null);
		}
		if (deviceChannel.getGpsTime() == null) {
			deviceChannel.setGpsTime(DateUtil.getNow());
		}

		deviceChannelMapper.updatePosition(deviceChannel);
	}
}
