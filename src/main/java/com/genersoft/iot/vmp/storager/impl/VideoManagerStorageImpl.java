package com.genersoft.iot.vmp.storager.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.storager.dao.dto.ChannelSourceInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private IRedisCatchStorage redisCatchStorage;

	@Autowired
    private PlatformChannelMapper platformChannelMapper;

	@Autowired
    private StreamProxyMapper streamProxyMapper;

	@Autowired
    private StreamPushMapper streamPushMapper;

	@Autowired
    private GbStreamMapper gbStreamMapper;

	@Autowired
    private PlatformCatalogMapper catalogMapper;

	@Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

	@Autowired
    private IGbStreamService gbStreamService;

	@Autowired
    private ParentPlatformMapper parentPlatformMapper;

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
	public boolean resetChannels(String deviceId, List<DeviceChannel> deviceChannelList) {
		if (CollectionUtils.isEmpty(deviceChannelList)) {
			return false;
		}
		List<DeviceChannel> allChannelInPlay = deviceChannelMapper.getAllChannelInPlay();
		Map<String,DeviceChannel> allChannelMapInPlay = new ConcurrentHashMap<>();
		if (allChannelInPlay.size() > 0) {
			for (DeviceChannel deviceChannel : allChannelInPlay) {
				allChannelMapInPlay.put(deviceChannel.getChannelId(), deviceChannel);
			}
		}
		TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
		// 数据去重
		List<DeviceChannel> channels = new ArrayList<>();
		StringBuilder stringBuilder = new StringBuilder();
		Map<String, Integer> subContMap = new HashMap<>();
		if (deviceChannelList.size() > 1) {
			// 数据去重
			Set<String> gbIdSet = new HashSet<>();
			for (DeviceChannel deviceChannel : deviceChannelList) {
				if (!gbIdSet.contains(deviceChannel.getChannelId())) {
					gbIdSet.add(deviceChannel.getChannelId());
					if (allChannelMapInPlay.containsKey(deviceChannel.getChannelId())) {
						deviceChannel.setStreamId(allChannelMapInPlay.get(deviceChannel.getChannelId()).getStreamId());
					}
					channels.add(deviceChannel);
					if (!StringUtils.isEmpty(deviceChannel.getParentId())) {
						if (subContMap.get(deviceChannel.getParentId()) == null) {
							subContMap.put(deviceChannel.getParentId(), 1);
						}else {
							Integer count = subContMap.get(deviceChannel.getParentId());
							subContMap.put(deviceChannel.getParentId(), count++);
						}
					}
				}else {
					stringBuilder.append(deviceChannel.getChannelId()).append(",");
				}
			}
			if (channels.size() > 0) {
				for (DeviceChannel channel : channels) {
					if (subContMap.get(channel.getChannelId()) != null){
						channel.setSubCount(subContMap.get(channel.getChannelId()));
					}
				}
			}

		}else {
			channels = deviceChannelList;
		}
		if (stringBuilder.length() > 0) {
			logger.info("[目录查询]收到的数据存在重复： {}" , stringBuilder);
		}
		if(CollectionUtils.isEmpty(channels)){
			logger.info("通道重设，数据为空={}" , deviceChannelList);
			return false;
		}
		try {
			int cleanChannelsResult = deviceChannelMapper.cleanChannelsNotInList(deviceId, channels);
			int limitCount = 300;
			boolean result = cleanChannelsResult < 0;
			if (!result && channels.size() > 0) {
				if (channels.size() > limitCount) {
					for (int i = 0; i < channels.size(); i += limitCount) {
						int toIndex = i + limitCount;
						if (i + limitCount > channels.size()) {
							toIndex = channels.size();
						}
						result = result || deviceChannelMapper.batchAdd(channels.subList(i, toIndex)) < 0;
					}
				}else {
					result = result || deviceChannelMapper.batchAdd(channels) < 0;
				}
			}
			if (result) {
				//事务回滚
				dataSourceTransactionManager.rollback(transactionStatus);
			}
			dataSourceTransactionManager.commit(transactionStatus);     //手动提交
			return true;
		}catch (Exception e) {
			e.printStackTrace();
			dataSourceTransactionManager.rollback(transactionStatus);
			return false;
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
	public PageInfo queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, Boolean online, Boolean catalogUnderDevice, int page, int count) {
		// 获取到所有正在播放的流
		PageHelper.startPage(page, count);
		List<DeviceChannel> all;
		if (catalogUnderDevice != null && catalogUnderDevice) {
			all = deviceChannelMapper.queryChannels(deviceId, deviceId, query, hasSubChannel, online);
			// 海康设备的parentId是SIP id
			List<DeviceChannel> deviceChannels = deviceChannelMapper.queryChannels(deviceId, sipConfig.getId(), query, hasSubChannel, online);
			all.addAll(deviceChannels);
		}else {
			all = deviceChannelMapper.queryChannels(deviceId, null, query, hasSubChannel, online);
		}
		return new PageInfo<>(all);
	}

	@Override
	public List<DeviceChannel> queryChannelsByDeviceIdWithStartAndLimit(String deviceId, String query, Boolean hasSubChannel, Boolean online, int start, int limit) {
		return deviceChannelMapper.queryChannelsByDeviceIdWithStartAndLimit(deviceId, null, query, hasSubChannel, online, start, limit);
	}


	@Override
	public List<DeviceChannel> queryChannelsByDeviceId(String deviceId) {
		return deviceChannelMapper.queryChannels(deviceId, null,null, null, null);
	}

	@Override
	public PageInfo<DeviceChannel> querySubChannels(String deviceId, String parentChannelId, String query, Boolean hasSubChannel, Boolean online, int page, int count) {
		PageHelper.startPage(page, count);
		List<DeviceChannel> all = deviceChannelMapper.queryChannels(deviceId, parentChannelId, query, hasSubChannel, online);
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
			platformChannelMapper.delChannelForDeviceId(deviceId);
			deviceChannelMapper.cleanChannelsByDeviceId(deviceId);
			if ( deviceMapper.del(deviceId) < 0 ) {
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
		redisCatchStorage.updateDevice(device);
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
		if (device == null) {
			return false;
		}
		device.setOnline(0);
		redisCatchStorage.updateDevice(device);
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
		if (mobilePosition.getDeviceId().equals(mobilePosition.getChannelId())) {
			mobilePosition.setChannelId(null);
		}
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
	public boolean addParentPlatform(ParentPlatform parentPlatform) {
		if (parentPlatform.getCatalogId() == null) {
			parentPlatform.setCatalogId(parentPlatform.getServerGBId());
		}
		int result = platformMapper.addParentPlatform(parentPlatform);
		return result > 0;
	}

	@Override
	public boolean updateParentPlatform(ParentPlatform parentPlatform) {
		int result = 0;
		if (parentPlatform.getCatalogGroup() == 0) {
			parentPlatform.setCatalogGroup(1);
		}
		if (parentPlatform.getAdministrativeDivision() == null) {
			parentPlatform.setAdministrativeDivision(parentPlatform.getAdministrativeDivision());
		}
		ParentPlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId()); // .getDeviceGBId());
		if (parentPlatform.getId() == null ) {
			if (parentPlatform.getCatalogId() == null) {
				parentPlatform.setCatalogId(parentPlatform.getServerGBId());
			}
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
				parentPlatformCatch = new ParentPlatformCatch();
				parentPlatformCatch.setId(parentPlatform.getServerGBId());
				redisCatchStorage.delPlatformCatchInfo(parentPlatById.getServerGBId());
			}

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
													   Boolean channelType, String platformId, String catalogId) {
		PageHelper.startPage(page, count);
		List<ChannelReduce> all = deviceChannelMapper.queryChannelListInAll(query, online, channelType, platformId, catalogId);
		return new PageInfo<>(all);
	}

	@Override
	public List<DeviceChannelInPlatform> queryChannelListInParentPlatform(String platformId) {

		return deviceChannelMapper.queryChannelByPlatformId(platformId);
	}


	@Override
	public int delChannelForGB(String platformId, List<ChannelReduce> channelReduces) {

		int result = platformChannelMapper.delChannelForGB(platformId, channelReduces);
		List<DeviceChannel> deviceChannelList = new ArrayList<>();
		for (ChannelReduce channelReduce : channelReduces) {
			DeviceChannel deviceChannel = new DeviceChannel();
			deviceChannel.setChannelId(channelReduce.getChannelId());
			deviceChannelList.add(deviceChannel);
		}
		eventPublisher.catalogEventPublish(platformId, deviceChannelList, CatalogEvent.DEL);
		return result;
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
	public List<PlatformCatalog> queryChannelInParentPlatformAndCatalog(String platformId, String catalogId) {
		List<PlatformCatalog> catalogs = platformChannelMapper.queryChannelInParentPlatformAndCatalog(platformId, catalogId);
		return catalogs;
	}

	@Override
	public List<PlatformCatalog> queryStreamInParentPlatformAndCatalog(String platformId, String catalogId) {
		List<PlatformCatalog> catalogs = platformGbStreamMapper.queryChannelInParentPlatformAndCatalogForCatalog(platformId, catalogId);
		return catalogs;
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
	@Override
	public int clearMobilePositionsByDeviceId(String deviceId) {
		return deviceMobilePositionMapper.clearMobilePositionsByDeviceId(deviceId);
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
	public int mediaOnline(String app, String stream) {
		GbStream gbStream = gbStreamMapper.selectOne(app, stream);
		int result;
		if ("proxy".equals(gbStream.getStreamType())) {
			result = streamProxyMapper.updateStatus(app, stream, true);
		}else {
			result = streamPushMapper.updatePushStatus(app, stream, true);
		}
		return result;
	}

	@Override
	public void updateParentPlatformStatus(String platformGbID, boolean online) {
		platformMapper.updateParentPlatformStatus(platformGbID, online);
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

	@Override
	public List<PlatformCatalog> getChildrenCatalogByPlatform(String platformId, String parentId) {
		return catalogMapper.selectByParentId(platformId, parentId);
	}

	@Override
	public int addCatalog(PlatformCatalog platformCatalog) {
		ParentPlatform platform = platformMapper.getParentPlatByServerGBId(platformCatalog.getPlatformId());
		if (platform == null) {
			return 0;
		}
		if (platform.getTreeType().equals(TreeType.BUSINESS_GROUP)) {
			if (platformCatalog.getPlatformId().equals(platformCatalog.getParentId())) {
				// 第一层节点
				platformCatalog.setBusinessGroupId(platformCatalog.getId());
				platformCatalog.setParentId(platform.getDeviceGBId());
			}else {
				// 获取顶层的
				PlatformCatalog topCatalog = getTopCatalog(platformCatalog.getParentId(), platform.getDeviceGBId());
				platformCatalog.setBusinessGroupId(topCatalog.getId());
			}
		}
		if (platform.getTreeType().equals(TreeType.CIVIL_CODE)) {
			platformCatalog.setCivilCode(platformCatalog.getId());
			if (platformCatalog.getPlatformId().equals(platformCatalog.getParentId())) {
				// 第一层节点
				platformCatalog.setParentId(platform.getDeviceGBId());
			}
		}

		int result = catalogMapper.add(platformCatalog);
		if (result > 0) {
			DeviceChannel deviceChannel = getDeviceChannelByCatalog(platformCatalog);
			eventPublisher.catalogEventPublish(platformCatalog.getPlatformId(), deviceChannel, CatalogEvent.ADD);
		}
		return result;
	}

	private PlatformCatalog getTopCatalog(String id, String platformId) {
		PlatformCatalog catalog = catalogMapper.selectParentCatalog(id);
		if (catalog.getParentId().equals(platformId)) {
			return catalog;
		}else {
			return getTopCatalog(catalog.getParentId(), platformId);
		}
	}

	@Override
	public PlatformCatalog getCatalog(String id) {
		return catalogMapper.select(id);
	}

	@Override
	public int delCatalog(String id) {
		PlatformCatalog platformCatalog = catalogMapper.select(id);
		if (platformCatalog.getChildrenCount() > 0) {
			List<PlatformCatalog> platformCatalogList = catalogMapper.selectByParentId(platformCatalog.getPlatformId(), platformCatalog.getId());
			for (PlatformCatalog catalog : platformCatalogList) {
				if (catalog.getChildrenCount() == 0) {
					delCatalogExecute(catalog.getId(), catalog.getPlatformId());
				}else {
					delCatalog(catalog.getId());
				}
			}
		}
		return delCatalogExecute(id, platformCatalog.getPlatformId());
	}
	private int delCatalogExecute(String id, String platformId) {
		int delresult =  catalogMapper.del(id);
		DeviceChannel deviceChannelForCatalog = new DeviceChannel();
		if (delresult > 0){
			deviceChannelForCatalog.setChannelId(id);
			eventPublisher.catalogEventPublish(platformId, deviceChannelForCatalog, CatalogEvent.DEL);
		}

		List<GbStream> gbStreams = platformGbStreamMapper.queryChannelInParentPlatformAndCatalog(platformId, id);
		if (gbStreams.size() > 0){
			List<DeviceChannel> deviceChannelList = new ArrayList<>();
			for (GbStream gbStream : gbStreams) {
				DeviceChannel deviceChannel = new DeviceChannel();
				deviceChannel.setChannelId(gbStream.getGbId());
				deviceChannelList.add(deviceChannel);
			}
			eventPublisher.catalogEventPublish(platformId, deviceChannelList, CatalogEvent.DEL);
		}
		int delStreamresult = platformGbStreamMapper.delByCatalogId(id);
		List<PlatformCatalog> platformCatalogs = platformChannelMapper.queryChannelInParentPlatformAndCatalog(platformId, id);
		if (platformCatalogs.size() > 0){
			List<DeviceChannel> deviceChannelList = new ArrayList<>();
			for (PlatformCatalog platformCatalog : platformCatalogs) {
				DeviceChannel deviceChannel = new DeviceChannel();
				deviceChannel.setChannelId(platformCatalog.getId());
				deviceChannelList.add(deviceChannel);
			}
			eventPublisher.catalogEventPublish(platformId, deviceChannelList, CatalogEvent.DEL);
		}
		int delChannelresult = platformChannelMapper.delByCatalogId(id);
		return delresult + delChannelresult + delStreamresult;
	}


	@Override
	public int updateCatalog(PlatformCatalog platformCatalog) {
		int result = catalogMapper.update(platformCatalog);
		if (result > 0) {
			DeviceChannel deviceChannel = getDeviceChannelByCatalog(platformCatalog);
			eventPublisher.catalogEventPublish(platformCatalog.getPlatformId(), deviceChannel, CatalogEvent.UPDATE);
		}
		return result;
	}

	@Override
	public int setDefaultCatalog(String platformId, String catalogId) {
		return platformMapper.setDefaultCatalog(platformId, catalogId, DateUtil.getNow());
	}

	@Override
	public List<DeviceChannel> queryCatalogInPlatform(String platformId) {
		return catalogMapper.queryCatalogInPlatform(platformId);
	}

	@Override
	public int delRelation(PlatformCatalog platformCatalog) {
		if (platformCatalog.getType() == 1) {
			DeviceChannel deviceChannel = new DeviceChannel();
			deviceChannel.setChannelId(platformCatalog.getId());
			eventPublisher.catalogEventPublish(platformCatalog.getPlatformId(), deviceChannel, CatalogEvent.DEL);
			return platformChannelMapper.delByCatalogIdAndChannelIdAndPlatformId(platformCatalog);
		}else if (platformCatalog.getType() == 2) {
			List<GbStream> gbStreams = platformGbStreamMapper.queryChannelInParentPlatformAndCatalog(platformCatalog.getPlatformId(), platformCatalog.getParentId());
			for (GbStream gbStream : gbStreams) {
				if (gbStream.getGbId().equals(platformCatalog.getId())) {
					DeviceChannel deviceChannel = new DeviceChannel();
					deviceChannel.setChannelId(gbStream.getGbId());
					eventPublisher.catalogEventPublish(platformCatalog.getPlatformId(), deviceChannel, CatalogEvent.DEL);
					return platformGbStreamMapper.delByAppAndStream(gbStream.getApp(), gbStream.getStream());
				}
			}
		}
		return 0;
	}

	@Override
	public int updateStreamGPS(List<GPSMsgInfo> gpsMsgInfos) {
		return gbStreamMapper.updateStreamGPS(gpsMsgInfos);
	}

	private List<DeviceChannel> getDeviceChannelListByChannelReduceList(List<ChannelReduce> channelReduces, String catalogId) {
		List<DeviceChannel> deviceChannelList = new ArrayList<>();
		if (channelReduces.size() > 0){
			for (ChannelReduce channelReduce : channelReduces) {
				DeviceChannel deviceChannel = queryChannel(channelReduce.getDeviceId(), channelReduce.getChannelId());
				deviceChannel.setParental(1);
				deviceChannel.setParentId(catalogId);
				deviceChannelList.add(deviceChannel);
			}
		}
		return deviceChannelList;
	}

	private DeviceChannel getDeviceChannelByCatalog(PlatformCatalog catalog) {
		ParentPlatform platform = platformMapper.getParentPlatByServerGBId(catalog.getPlatformId());
		DeviceChannel deviceChannel = new DeviceChannel();
		deviceChannel.setChannelId(catalog.getId());
		deviceChannel.setName(catalog.getName());
		deviceChannel.setLongitude(0.0);
		deviceChannel.setLatitude(0.0);
		deviceChannel.setDeviceId(platform.getDeviceGBId());
		deviceChannel.setManufacture("wvp-pro");
		deviceChannel.setStatus(1);
		deviceChannel.setParental(1);

		deviceChannel.setRegisterWay(1);
		// 行政区划应该是Domain的前八位
		if (platform.getTreeType().equals(TreeType.BUSINESS_GROUP)) {
			deviceChannel.setParentId(catalog.getParentId());
			deviceChannel.setBusinessGroupId(catalog.getBusinessGroupId());
		}

		deviceChannel.setModel("live");
		deviceChannel.setOwner("wvp-pro");
		deviceChannel.setSecrecy("0");
		return deviceChannel;
	}

	@Override
	public List<DeviceChannel> queryOnlineChannelsByDeviceId(String deviceId) {
		return deviceChannelMapper.queryOnlineChannelsByDeviceId(deviceId);
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
	public void delCatalogByPlatformId(String serverGBId) {
		catalogMapper.delByPlatformId(serverGBId);
	}

	@Override
	public void delRelationByPlatformId(String serverGBId) {
		platformGbStreamMapper.delByPlatformId(serverGBId);
		platformChannelMapper.delByPlatformId(serverGBId);
	}

	@Override
	public PlatformCatalog queryDefaultCatalogInPlatform(String platformId) {
		return catalogMapper.selectDefaultByPlatFormId(platformId);
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

	@Override
	public void cleanContentForPlatform(String serverGBId) {
//		List<PlatformCatalog> catalogList = catalogMapper.selectByPlatForm(serverGBId);
//		if (catalogList.size() > 0) {
//			int result = catalogMapper.delByPlatformId(serverGBId);
//			if (result > 0) {
//				List<DeviceChannel> deviceChannels = new ArrayList<>();
//				for (PlatformCatalog catalog : catalogList) {
//					deviceChannels.add(getDeviceChannelByCatalog(catalog));
//				}
//				eventPublisher.catalogEventPublish(serverGBId, deviceChannels, CatalogEvent.DEL);
//			}
//		}
		catalogMapper.delByPlatformId(serverGBId);
		platformChannelMapper.delByPlatformId(serverGBId);
		platformGbStreamMapper.delByPlatformId(serverGBId);
	}

	@Override
	public List<DeviceChannel> queryChannelWithCatalog(String serverGBId) {
		return deviceChannelMapper.queryChannelWithCatalog(serverGBId);
	}
}
