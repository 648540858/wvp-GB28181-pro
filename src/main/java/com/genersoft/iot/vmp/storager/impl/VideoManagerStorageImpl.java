package com.genersoft.iot.vmp.storager.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.storager.dao.dto.ChannelSourceInfo;
import com.genersoft.iot.vmp.streamProxy.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.streamPush.dao.StreamPushMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频设备数据存储-jdbc实现
 * swwheihei
 * 2020年5月6日 下午2:31:42
 */
@SuppressWarnings("rawtypes")
@Component
@DS("master")
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
	private PlatformCatalogMapper platformCatalogMapper;

	@Autowired
    private StreamProxyMapper streamProxyMapper;

	@Autowired
    private StreamPushMapper streamPushMapper;

	@Autowired
    private GbStreamMapper gbStreamMapper;

	@Autowired
    private UserSetting userSetting;

	@Autowired
    private PlatformCatalogMapper catalogMapper;

	@Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

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
	public PageInfo<ChannelReduce> queryAllChannelList(int page, int count, String query, Boolean online,
													   Boolean channelType, String platformId, String catalogId) {
		PageHelper.startPage(page, count);
		List<ChannelReduce> all = deviceChannelMapper.queryChannelListInAll(query, online, channelType, platformId, catalogId);
		return new PageInfo<>(all);
	}


	@Override
	public int delChannelForGB(String platformId, List<ChannelReduce> channelReduces) {

		int result = platformChannelMapper.delChannelForGB(platformId, channelReduces);
		List<CommonGBChannel> deviceChannelList = new ArrayList<>();
		for (ChannelReduce channelReduce : channelReduces) {
			CommonGBChannel deviceChannel = new CommonGBChannel();
			deviceChannel.setGbDeviceId(channelReduce.getChannelId());
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
	 * 根据国标ID获取平台关联的直播流
	 * @param platformId
	 * @param gbId
	 * @return
	 */
	@Override
	public GbStream queryStreamInParentPlatform(String platformId, String gbId) {
		return gbStreamMapper.queryStreamInPlatform(platformId, gbId);
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
	public List<PlatformCatalog> getChildrenCatalogByPlatform(String platformId, String parentId) {
		return catalogMapper.selectByParentId(platformId, parentId);
	}

	@Override
	public int addCatalog(PlatformCatalog platformCatalog) {
		ParentPlatform platform = platformMapper.getParentPlatByServerGBId(platformCatalog.getPlatformId());
		if (platform == null) {
			return 0;
		}
		if (platformCatalog.getId().length() <= 8) {
			platformCatalog.setCivilCode(platformCatalog.getParentId());
		}else {
			if (platformCatalog.getId().length() != 20) {
				return 0;
			}
			if (platformCatalog.getParentId() != null) {
				switch (Integer.parseInt(platformCatalog.getId().substring(10, 13))){
					case 200:
					case 215:
						if (platformCatalog.getParentId().length() <= 8) {
							platformCatalog.setCivilCode(platformCatalog.getParentId());
						}else {
							PlatformCatalog catalog = catalogMapper.selectByPlatFormAndCatalogId(platformCatalog.getPlatformId(), platformCatalog.getParentId());
							if (catalog != null) {
								platformCatalog.setCivilCode(catalog.getCivilCode());
							}
						}
						break;
					case 216:
						if (platformCatalog.getParentId().length() <= 8) {
							platformCatalog.setCivilCode(platformCatalog.getParentId());
						}else {
							PlatformCatalog catalog = catalogMapper.selectByPlatFormAndCatalogId(platformCatalog.getPlatformId(),platformCatalog.getParentId());
							if (catalog == null) {
								logger.warn("[添加目录] 无法获取目录{}的CivilCode和BusinessGroupId", platformCatalog.getPlatformId());
								break;
							}
							platformCatalog.setCivilCode(catalog.getCivilCode());
							if (Integer.parseInt(platformCatalog.getParentId().substring(10, 13)) == 215) {
								platformCatalog.setBusinessGroupId(platformCatalog.getParentId());
							}else {
								if (Integer.parseInt(platformCatalog.getParentId().substring(10, 13)) == 216) {
									platformCatalog.setBusinessGroupId(catalog.getBusinessGroupId());
								}
							}
						}
						break;
					default:
						break;
				}
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
		PlatformCatalog catalog = catalogMapper.selectByPlatFormAndCatalogId(platformId, id);
		if (catalog.getParentId().equals(platformId)) {
			return catalog;
		}else {
			return getTopCatalog(catalog.getParentId(), platformId);
		}
	}

	@Override
	public PlatformCatalog getCatalog(String platformId, String id) {
		return catalogMapper.selectByPlatFormAndCatalogId(platformId, id);
	}

	@Override
	public int delCatalog(String platformId, String id) {
		return delCatalogExecute(id, platformId);
	}
	private int delCatalogExecute(String id, String platformId) {
		int delresult =  catalogMapper.del(platformId, id);
		DeviceChannel deviceChannelForCatalog = new DeviceChannel();
		if (delresult > 0){
			deviceChannelForCatalog.setDeviceId(id);
			eventPublisher.catalogEventPublish(platformId, deviceChannelForCatalog, CatalogEvent.DEL);
		}

		List<GbStream> gbStreams = platformGbStreamMapper.queryChannelInParentPlatformAndCatalog(platformId, id);
		if (!gbStreams.isEmpty()){
			List<CommonGBChannel> deviceChannelList = new ArrayList<>();
			for (GbStream gbStream : gbStreams) {
				CommonGBChannel deviceChannel = new CommonGBChannel();
				deviceChannel.setGbDeviceId(gbStream.getGbId());
				deviceChannelList.add(deviceChannel);
			}
			eventPublisher.catalogEventPublish(platformId, deviceChannelList, CatalogEvent.DEL);
		}
		int delStreamresult = platformGbStreamMapper.delByPlatformAndCatalogId(platformId,id);
		List<PlatformCatalog> platformCatalogs = platformChannelMapper.queryChannelInParentPlatformAndCatalog(platformId, id);
		if (!platformCatalogs.isEmpty()){
			List<CommonGBChannel> deviceChannelList = new ArrayList<>();
			for (PlatformCatalog platformCatalog : platformCatalogs) {
				CommonGBChannel deviceChannel = new CommonGBChannel();
				deviceChannel.setGbDeviceId(platformCatalog.getId());
				deviceChannelList.add(deviceChannel);
			}
			eventPublisher.catalogEventPublish(platformId, deviceChannelList, CatalogEvent.DEL);
		}
		int delChannelresult = platformChannelMapper.delByCatalogId(platformId, id);
		// 查看是否存在子目录，如果存在一并删除
		List<String> allChildCatalog = getAllChildCatalog(id, platformId);
		if (!allChildCatalog.isEmpty()) {
			int limitCount = 50;
			if (allChildCatalog.size() > limitCount) {
				for (int i = 0; i < allChildCatalog.size(); i += limitCount) {
					int toIndex = i + limitCount;
					if (i + limitCount > allChildCatalog.size()) {
						toIndex = allChildCatalog.size();
					}
					delChannelresult += platformCatalogMapper.deleteAll(platformId, allChildCatalog.subList(i, toIndex));
				}
			}else {
				delChannelresult += platformCatalogMapper.deleteAll(platformId, allChildCatalog);
			}
		}
		return delresult + delChannelresult + delStreamresult;
	}

	private List<String> getAllChildCatalog(String id, String platformId) {
		List<String> catalogList = platformCatalogMapper.queryCatalogFromParent(id, platformId);
		List<String> catalogListChild = new ArrayList<>();
		if (catalogList != null && !catalogList.isEmpty()) {
			for (String childId : catalogList) {
				List<String> allChildCatalog = getAllChildCatalog(childId, platformId);
				if (allChildCatalog != null && !allChildCatalog.isEmpty()) {
					catalogListChild.addAll(allChildCatalog);
				}

			}
		}
		if (!catalogListChild.isEmpty()) {
			catalogList.addAll(catalogListChild);
		}
		return catalogList;
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
	public int delRelation(PlatformCatalog platformCatalog) {
		if (platformCatalog.getType() == 1) {
			CommonGBChannel deviceChannel = new CommonGBChannel();
			deviceChannel.setGbDeviceId(platformCatalog.getId());
			eventPublisher.catalogEventPublish(platformCatalog.getPlatformId(), deviceChannel, CatalogEvent.DEL);
			return platformChannelMapper.delByCatalogIdAndChannelIdAndPlatformId(platformCatalog);
		}else if (platformCatalog.getType() == 2) {
			List<GbStream> gbStreams = platformGbStreamMapper.queryChannelInParentPlatformAndCatalog(platformCatalog.getPlatformId(), platformCatalog.getParentId());
			for (GbStream gbStream : gbStreams) {
				if (gbStream.getGbId().equals(platformCatalog.getId())) {
					CommonGBChannel deviceChannel = new CommonGBChannel();
					deviceChannel.setGbDeviceId(gbStream.getGbId());
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


	private DeviceChannel getDeviceChannelByCatalog(PlatformCatalog catalog) {
		ParentPlatform platform = platformMapper.getParentPlatByServerGBId(catalog.getPlatformId());
		DeviceChannel deviceChannel = new DeviceChannel();
		deviceChannel.setDeviceId(catalog.getId());
		deviceChannel.setName(catalog.getName());
		deviceChannel.setDeviceId(platform.getDeviceGBId());
		deviceChannel.setManufacturer("wvp-pro");
		deviceChannel.setStatus("ON");
		deviceChannel.setParental(1);

		deviceChannel.setRegisterWay(1);
		deviceChannel.setParentId(catalog.getParentId());
		deviceChannel.setBusinessGroupId(catalog.getBusinessGroupId());

		deviceChannel.setModel("live");
		deviceChannel.setOwner("wvp-pro");
		deviceChannel.setSecrecy(0);
		return deviceChannel;
	}

	@Override
	public List<ParentPlatform> queryPlatFormListForGBWithGBId(String channelId, List<String> platforms) {
		return platformChannelMapper.queryPlatFormListForGBWithGBId(channelId, platforms);
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
}
