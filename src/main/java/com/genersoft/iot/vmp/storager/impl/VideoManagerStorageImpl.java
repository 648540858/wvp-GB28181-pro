package com.genersoft.iot.vmp.storager.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelReduce;
import com.genersoft.iot.vmp.gb28181.dao.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.dto.ChannelSourceInfo;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@DS("master")
public class VideoManagerStorageImpl implements IVideoManagerStorage {


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
    private PlatformMapper platformMapper;

	@Autowired
    private IRedisCatchStorage redisCatchStorage;

	@Autowired
    private PlatformChannelMapper platformChannelMapper;

	@Autowired
	private PlatformCatalogMapper platformCatalogMapper;

	@Autowired
    private GbStreamMapper gbStreamMapper;

	@Autowired
    private PlatformCatalogMapper catalogMapper;

	@Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;




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
	public boolean updateParentPlatform(Platform parentPlatform) {
		int result = 0;
		if (parentPlatform.getCatalogGroup() == 0) {
			parentPlatform.setCatalogGroup(1);
		}
		PlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId()); // .getDeviceGBId());
		if (parentPlatform.getId() == null ) {
			if (parentPlatform.getCatalogId() == null) {
				parentPlatform.setCatalogId(parentPlatform.getServerGBId());
			}
			result = platformMapper.addParentPlatform(parentPlatform);
			if (parentPlatformCatch == null) {
				parentPlatformCatch = new PlatformCatch();
				parentPlatformCatch.setParentPlatform(parentPlatform);
				parentPlatformCatch.setId(parentPlatform.getServerGBId());
			}
		}else {
			if (parentPlatformCatch == null) { // serverGBId 已变化
				Platform parentPlatById = platformMapper.getParentPlatById(parentPlatform.getId());
				// 使用旧的查出缓存ID
				parentPlatformCatch = new PlatformCatch();
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
	public boolean deleteParentPlatform(Platform parentPlatform) {
		int result = platformMapper.delParentPlatform(parentPlatform);
		// 删除关联的通道
		platformChannelMapper.cleanChannelForGB(parentPlatform.getServerGBId());
		return result > 0;
	}

	@Override
	public Platform queryParentPlatByServerGBId(String platformGbId) {
		return platformMapper.getParentPlatByServerGBId(platformGbId);
	}

	@Override
	public List<Platform> queryEnableParentPlatformList(boolean enable) {
		return platformMapper.getEnableParentPlatformList(enable);
	}

	@Override
	public List<Platform> queryEnablePlatformListWithAsMessageChannel() {
		return platformMapper.queryEnablePlatformListWithAsMessageChannel();
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
//		eventPublisher.catalogEventPublish(platformId, deviceChannelList, CatalogEvent.DEL);
		return result;
	}

	@Override
	public DeviceChannel queryChannelInParentPlatform(String platformId, String channelId) {
		List<DeviceChannel> channels = platformChannelMapper.queryChannelInParentPlatform(platformId, channelId);
		if (channels.size() > 1) {
			// 出现长度大于0的时候肯定是国标通道的ID重复了
			log.warn("国标ID存在重复：{}", channelId);
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
	public int delRelation(PlatformCatalog platformCatalog) {
		if (platformCatalog.getType() == 1) {
			CommonGBChannel deviceChannel = new CommonGBChannel();
			deviceChannel.setGbDeviceId(platformCatalog.getId());
//			eventPublisher.catalogEventPublish(platformCatalog.getPlatformId(), deviceChannel, CatalogEvent.DEL);
			return platformChannelMapper.delByCatalogIdAndChannelIdAndPlatformId(platformCatalog);
		}else if (platformCatalog.getType() == 2) {
			List<GbStream> gbStreams = platformGbStreamMapper.queryChannelInParentPlatformAndCatalog(platformCatalog.getPlatformId(), platformCatalog.getParentId());
			for (GbStream gbStream : gbStreams) {
				if (gbStream.getGbId().equals(platformCatalog.getId())) {
					CommonGBChannel deviceChannel = new CommonGBChannel();
					deviceChannel.setGbDeviceId(gbStream.getGbId());
//					eventPublisher.catalogEventPublish(platformCatalog.getPlatformId(), deviceChannel, CatalogEvent.DEL);
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

	@Override
	public List<Platform> queryPlatFormListForGBWithGBId(String channelId, List<String> platforms) {
		return platformChannelMapper.queryPlatFormListForGBWithGBId(channelId, platforms);
	}

	@Override
	public void delRelationByPlatformId(String serverGBId) {
		platformGbStreamMapper.delByPlatformId(serverGBId);
		platformChannelMapper.delByPlatformId(serverGBId);
	}


	@Override
	public List<ChannelSourceInfo> getChannelSource(String platformId, String gbId) {
		return platformMapper.getChannelSource(platformId, gbId);
	}
}
