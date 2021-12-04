package com.genersoft.iot.vmp.storager.impl;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**    
 * @description:视频设备数据存储-jdbc实现
 * @author: swwheihei
 * @date:   2020年5月6日 下午2:31:42
 */
@SuppressWarnings("rawtypes")
@Component
public class VideoManagerStoragerImpl implements IVideoManagerStorager {

	private Logger logger = LoggerFactory.getLogger(VideoManagerStoragerImpl.class);

	@Autowired
	DataSourceTransactionManager dataSourceTransactionManager;

	@Autowired
	TransactionDefinition transactionDefinition;

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
    private PlatformChannelMapper platformChannelMapper;

	@Autowired
    private StreamProxyMapper streamProxyMapper;

	@Autowired
    private StreamPushMapper streamPushMapper;

	@Autowired
    private GbStreamMapper gbStreamMapper;
;

	@Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

	@Autowired
    private IGbStreamService gbStreamService;

	@Autowired
    private ParentPlatformMapper parentPlatformMapper;

	@Autowired
    private VideoStreamSessionManager streamSession;

	@Autowired
    private MediaServerMapper mediaServerMapper;

	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


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
		String now = this.format.format(System.currentTimeMillis());
		device.setUpdateTime(now);
		Device deviceByDeviceId = deviceMapper.getDeviceByDeviceId(device.getDeviceId());
		if (deviceByDeviceId == null) {
			device.setCreateTime(now);
			return deviceMapper.add(device) > 0;
		}else {
			return deviceMapper.update(device) > 0;
		}

	}

	@Override
	public synchronized void updateChannel(String deviceId, DeviceChannel channel) {
		String channelId = channel.getChannelId();
		channel.setDeviceId(deviceId);
		channel.setStreamId(streamSession.getStreamId(deviceId, channel.getChannelId()));
		String now = this.format.format(System.currentTimeMillis());
		channel.setUpdateTime(now);
		DeviceChannel deviceChannel = deviceChannelMapper.queryChannel(deviceId, channelId);
		if (deviceChannel == null) {
			channel.setCreateTime(now);
			deviceChannelMapper.add(channel);
		}else {
			deviceChannelMapper.update(channel);
		}
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
		TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
		boolean result = false;
		try {
			if (platformChannelMapper.delChannelForDeviceId(deviceId) <0  // 删除与国标平台的关联
					|| deviceChannelMapper.cleanChannelsByDeviceId(deviceId) < 0 // 删除他的通道
					|| deviceMapper.del(deviceId) < 0 // 移除设备信息
			) {
				//事务回滚
				dataSourceTransactionManager.rollback(transactionStatus);
			}
			result = true;
			dataSourceTransactionManager.commit(transactionStatus);     //手动提交
		}catch (Exception e) {
			dataSourceTransactionManager.rollback(transactionStatus);
		}
		return result;
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
		logger.info("更新设备在线: " + deviceId);
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
		logger.info("更新设备离线: " + deviceId);
		Device device = deviceMapper.getDeviceByDeviceId(deviceId);
		if (device == null) return false;
		device.setOnline(0);
		return deviceMapper.update(device) > 0;
	}

	/**
	 * 更新所有设备离线
	 *
	 * @return true：更新成功  false：更新失败
	 */
	@Override
	public synchronized boolean outlineForAll() {
		logger.info("更新所有设备离线");
		int result = deviceMapper.outlineForAll();
		return result > 0;
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
		if (parentPlatform.getId() == null ) {
			result = platformMapper.addParentPlatform(parentPlatform);
			if (parentPlatformCatch == null) {
				parentPlatformCatch = new ParentPlatformCatch();
				parentPlatformCatch.setParentPlatform(parentPlatform);
				parentPlatformCatch.setId(parentPlatform.getServerGBId());
			}
		}else {
			if (parentPlatformCatch == null) { // serverGBId 已变化
				ParentPlatform parentPlatById = platformMapper.getParentPlatById(parentPlatform.getId());
				// 使用旧的查出缓存ID
				parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatById.getServerGBId());
				parentPlatformCatch.setId(parentPlatform.getServerGBId());
				redisCatchStorage.delPlatformCatchInfo(parentPlatById.getServerGBId());
			}
			result = platformMapper.updateParentPlatform(parentPlatform);
		}
		// 更新缓存
		parentPlatformCatch.setParentPlatform(parentPlatform);
		redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
		// 共享所有视频流，需要将现有视频流添加到此平台
		List<GbStream> gbStreams = gbStreamMapper.selectAll();
		if (gbStreams.size() > 0) {
			if (parentPlatform.isShareAllLiveStream()) {
				gbStreamService.addPlatformInfo(gbStreams, parentPlatform.getServerGBId());
			}else {
				gbStreamService.delPlatformInfo(gbStreams);
			}
		}
		return result > 0;
	}

	@Transactional
	@Override
	public boolean deleteParentPlatform(ParentPlatform parentPlatform) {
		int result = platformMapper.delParentPlatform(parentPlatform);
		// 删除关联的通道
		platformChannelMapper.cleanChannelForGB(parentPlatform.getServerGBId());
		return result > 0;
	}

	@Override
	public PageInfo<ParentPlatform> queryParentPlatformList(int page, int count) {
		PageHelper.startPage(page, count);
		List<ParentPlatform> all = platformMapper.getParentPlatformList();
		return new PageInfo<>(all);
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
		List<String> relatedPlatformchannels = platformChannelMapper.findChannelRelatedPlatform(platformId, deviceAndChannelList);
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
			result = platformChannelMapper.addChannels(platformId, channelReducesToAdd);
		}

		return result;
	}


	@Override
	public int delChannelForGB(String platformId, List<ChannelReduce> channelReduces) {

		int result = platformChannelMapper.delChannelForGB(platformId, channelReduces);

		return result;
	}

	@Override
	public DeviceChannel queryChannelInParentPlatform(String platformId, String channelId) {
		DeviceChannel channel = platformChannelMapper.queryChannelInParentPlatform(platformId, channelId);
		return channel;
	}

	@Override
	public Device queryVideoDeviceByPlatformIdAndChannelId(String platformId, String channelId) {
		Device device = platformChannelMapper.queryVideoDeviceByPlatformIdAndChannelId(platformId, channelId);
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
	 * @param streamProxyItem
	 * @return
	 */
	@Override
	public boolean addStreamProxy(StreamProxyItem streamProxyItem) {
		TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
		boolean result = false;
		streamProxyItem.setStreamType("proxy");
		streamProxyItem.setStatus(true);
		String now = this.format.format(System.currentTimeMillis());
		streamProxyItem.setCreateTime(now);
		try {
			if (gbStreamMapper.add(streamProxyItem)<0 || streamProxyMapper.add(streamProxyItem) < 0) {
				//事务回滚
				dataSourceTransactionManager.rollback(transactionStatus);
			}
			result = true;
			dataSourceTransactionManager.commit(transactionStatus);     //手动提交
		}catch (Exception e) {
			logger.error("向数据库添加流代理失败：", e);
			dataSourceTransactionManager.rollback(transactionStatus);
		}
		return result;
	}

	/**
	 * 更新代理流
	 * @param streamProxyItem
	 * @return
	 */
	@Override
	public boolean updateStreamProxy(StreamProxyItem streamProxyItem) {
		TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
		boolean result = false;
		streamProxyItem.setStreamType("proxy");
		try {
			if (gbStreamMapper.update(streamProxyItem)<0 || streamProxyMapper.update(streamProxyItem) < 0) {
				//事务回滚
				dataSourceTransactionManager.rollback(transactionStatus);
			}
			dataSourceTransactionManager.commit(transactionStatus);     //手动提交
			result = true;
		}catch (Exception e) {
			e.printStackTrace();
			dataSourceTransactionManager.rollback(transactionStatus);
		}
		return result;
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
	 * 根据是否启用获取代理流列表
	 * @param enable
	 * @return
	 */
	@Override
	public List<StreamProxyItem> getStreamProxyListForEnable(boolean enable) {
		return streamProxyMapper.selectForEnable(enable);
	}

	/**
	 * 分页查询代理流列表
	 * @param page
	 * @param count
	 * @return
	 */
	@Override
	public PageInfo<StreamProxyItem> queryStreamProxyList(Integer page, Integer count) {
		PageHelper.startPage(page, count);
		List<StreamProxyItem> all = streamProxyMapper.selectAll();
		return new PageInfo<>(all);
	}

	/**
	 * 根据国标ID获取平台关联的直播流
	 * @param platformId
	 * @param gbId
	 * @return
	 */
	@Override
	public List<GbStream> queryStreamInParentPlatform(String platformId, String gbId) {
		return gbStreamMapper.queryStreamInPlatform(platformId, gbId);
	}

	/**
	 * 获取平台关联的直播流
	 * @param platformId
	 * @return
	 */
	@Override
	public List<GbStream> queryGbStreamListInPlatform(String platformId) {
		return gbStreamMapper.queryGbStreamListInPlatform(platformId);
	}

	/**
	 * 按照是app和stream获取代理流
	 * @param app
	 * @param stream
	 * @return
	 */
	@Override
	public StreamProxyItem queryStreamProxy(String app, String stream){
		return streamProxyMapper.selectOne(app, stream);
	}

	@Override
	public void updateMediaList(List<StreamPushItem> streamPushItems) {
		if (streamPushItems == null || streamPushItems.size() == 0) return;
		logger.info("updateMediaList:  " + streamPushItems.size());
		streamPushMapper.addAll(streamPushItems);
		// TODO 待优化
		for (int i = 0; i < streamPushItems.size(); i++) {
			gbStreamMapper.setStatus(streamPushItems.get(i).getApp(), streamPushItems.get(i).getStream(), true);
		}
	}

	@Override
	public void updateMedia(StreamPushItem streamPushItem) {
		streamPushMapper.del(streamPushItem.getApp(), streamPushItem.getStream());
		streamPushMapper.add(streamPushItem);
		gbStreamMapper.setStatus(streamPushItem.getApp(), streamPushItem.getStream(), true);
		if(!StringUtils.isEmpty(streamPushItem.getGbId() )){
			// 查找开启了全部直播流共享的上级平台
			List<ParentPlatform> parentPlatforms = parentPlatformMapper.selectAllAhareAllLiveStream();
			if (parentPlatforms.size() > 0) {
				for (ParentPlatform parentPlatform : parentPlatforms) {
					streamPushItem.setPlatformId(parentPlatform.getServerGBId());
					String stream = streamPushItem.getStream();
					StreamProxyItem streamProxyItems = platformGbStreamMapper.selectOne(streamPushItem.getApp(), stream, parentPlatform.getServerGBId());
					if (streamProxyItems == null) {
						platformGbStreamMapper.add(streamPushItem);
					}

				}
			}
		}

	}

	@Override
	public int removeMedia(String app, String stream) {
		return streamPushMapper.del(app, stream);
	}

	@Override
	public void clearMediaList() {
		streamPushMapper.clear();
	}

	@Override
	public int mediaOutline(String app, String streamId) {
		return gbStreamMapper.setStatus(app, streamId, false);
	}

	@Override
	public void updateParentPlatformStatus(String platformGbID, boolean online) {
		platformMapper.updateParentPlatformStatus(platformGbID, online);
	}

	@Override
	public void updateMediaServer(MediaServerItem mediaServerItem) {
		String now = this.format.format(System.currentTimeMillis());
		mediaServerItem.setUpdateTime(now);
		if (mediaServerMapper.queryOne(mediaServerItem.getId()) != null) {
			mediaServerMapper.update(mediaServerItem);
		}else {
			mediaServerItem.setCreateTime(now);
			mediaServerMapper.add(mediaServerItem);
		}
	}

	@Override
	public List<StreamProxyItem> getStreamProxyListForEnableInMediaServer(String id, boolean enable) {
		return streamProxyMapper.selectForEnableInMediaServer(id, enable);
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
	public StreamProxyItem getStreamProxyByAppAndStream(String app, String streamId) {
		return streamProxyMapper.selectOne(app, streamId);
	}
}
