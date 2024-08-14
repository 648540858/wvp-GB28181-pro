package com.genersoft.iot.vmp.storager;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
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
	boolean updateParentPlatform(Platform parentPlatform);

	/**
	 * 删除上级平台
	 * @param parentPlatform
	 */
	boolean deleteParentPlatform(Platform parentPlatform);

	/**
	 * 获取所有已启用的平台
	 * @return
	 */
	List<Platform> queryEnableParentPlatformList(boolean enable);

	/**
	 * 获取上级平台
	 * @param platformGbId
	 * @return
	 */
	Platform queryParentPlatByServerGBId(String platformGbId);

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
	 * 根据通道ID获取其所在设备
	 * @param channelId  通道ID
	 * @return
	 */
    Device queryVideoDeviceByChannelId(String channelId);

	List<Platform> queryPlatFormListForGBWithGBId(String channelId, List<String> platforms);

	List<Platform> queryEnablePlatformListWithAsMessageChannel();

}
