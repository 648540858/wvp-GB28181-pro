package com.genersoft.iot.vmp.storager;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.dao.dto.ChannelSourceInfo;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelReduce;

import java.util.List;

/**    
 * @description:视频设备数据存储接口
 * @author: swwheihei
 * @date:   2020年5月6日 下午2:14:31     
 */
@SuppressWarnings("rawtypes")
public interface IVideoManagerStorage {


	/**
	 * 更新上级平台
	 * @param parentPlatform
	 */
	boolean updateParentPlatform(ParentPlatform parentPlatform);

	/**
	 * 删除上级平台
	 * @param parentPlatform
	 */
	boolean deleteParentPlatform(ParentPlatform parentPlatform);

	/**
	 * 获取所有已启用的平台
	 * @return
	 */
	List<ParentPlatform> queryEnableParentPlatformList(boolean enable);

	/**
	 * 获取上级平台
	 * @param platformGbId
	 * @return
	 */
	ParentPlatform queryParentPlatByServerGBId(String platformGbId);

	/**
	 *  移除上级平台的通道信息
	 * @param platformId
	 * @param channelReduces
	 * @return
	 */
	int delChannelForGB(String platformId, List<ChannelReduce> channelReduces);


    DeviceChannel queryChannelInParentPlatform(String platformId, String channelId);

    Device queryVideoDeviceByPlatformIdAndChannelId(String platformId, String channelId);

	/**
	 * 针对deviceinfo指令的查询接口
	 * @param platformId 平台id
	 * @param channelId 通道id
	 * @return 设备信息
	 */
	Device queryDeviceInfoByPlatformIdAndChannelId(String platformId, String channelId);

	/**
	 * 查询移动位置轨迹
	 * @param deviceId
	 * @param startTime
	 * @param endTime
	 */
	public List<MobilePosition> queryMobilePositions(String deviceId, String channelId, String startTime, String endTime);

	/**
	 * 查询最新移动位置
	 * @param deviceId
	 */
	public MobilePosition queryLatestPosition(String deviceId);

	/**
	 * 根据国标ID获取平台关联的直播流
	 * @param platformId
	 * @param channelId
	 * @return
	 */
	GbStream queryStreamInParentPlatform(String platformId, String channelId);

	/**
	 * 根据通道ID获取其所在设备
	 * @param channelId  通道ID
	 * @return
	 */
    Device queryVideoDeviceByChannelId(String channelId);

    int delRelation(PlatformCatalog platformCatalog);

	int updateStreamGPS(List<GPSMsgInfo> gpsMsgInfo);

	List<ParentPlatform> queryPlatFormListForGBWithGBId(String channelId, List<String> platforms);

	void delRelationByPlatformId(String serverGBId);

	List<ChannelSourceInfo> getChannelSource(String platformId, String gbId);

	List<ParentPlatform> queryEnablePlatformListWithAsMessageChannel();

}
