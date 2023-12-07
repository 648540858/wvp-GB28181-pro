package com.genersoft.iot.vmp.storager;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.dao.dto.ChannelSourceInfo;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import com.genersoft.iot.vmp.web.gb28181.dto.DeviceChannelExtend;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**    
 * @description:视频设备数据存储接口
 * @author: swwheihei
 * @date:   2020年5月6日 下午2:14:31     
 */
@SuppressWarnings("rawtypes")
public interface IVideoManagerStorage {

	/**   
	 * 根据设备ID判断设备是否存在
	 * 
	 * @param deviceId 设备ID
	 * @return true:存在  false：不存在
	 */
	boolean exists(String deviceId);

	/**
	 * 开始播放
	 * @param deviceId 设备id
	 * @param channelId 通道ID
	 * @param streamId 流地址
	 */
	void startPlay(String deviceId, String channelId, String streamId);

	/**
	 * 停止播放
	 * @param deviceId 设备id
	 * @param channelId 通道ID
	 */
	void stopPlay(String deviceId, String channelId);
	
	/**   
	 * 获取设备
	 * 
	 * @param deviceId 设备ID
	 * @return DShadow 设备对象
	 */
	Device queryVideoDevice(String deviceId);

	/**
	 * 获取某个设备的通道列表
	 *
	 * @param deviceId 设备ID
	 * @param page 分页 当前页
	 * @param count 每页数量
	 * @return
	 */
	PageInfo<DeviceChannel> queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, Boolean online, Boolean catalogUnderDevice, int page, int count);
	
	List<DeviceChannelExtend> queryChannelsByDeviceIdWithStartAndLimit(String deviceId, List<String> channelIds, String query, Boolean hasSubChannel, Boolean online, int start, int limit);


	/**
	 * 获取某个设备的通道
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 */
	DeviceChannel queryChannel(String deviceId, String channelId);

	/**
	 * 删除通道
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 */
	int delChannel(String deviceId, String channelId);

	/**
	 * 获取多个设备
	 * @param page 当前页数
	 * @param count 每页数量
	 * @return List<Device> 设备对象数组
	 */
	PageInfo<Device> queryVideoDeviceList(int page, int count,Boolean online);

	/**
	 * 获取多个设备
	 *
	 * @return List<Device> 设备对象数组
	 */
	List<Device> queryVideoDeviceList(Boolean online);



	/**
	 * 查询子设备
	 *
	 * @param deviceId
	 * @param channelId
	 * @param page
	 * @param count
	 * @return
	 */
	PageInfo querySubChannels(String deviceId, String channelId, String query, Boolean hasSubChannel, Boolean online, int page, int count);


	/**
	 * 清空通道
	 * @param deviceId
	 */
	void cleanChannelsForDevice(String deviceId);

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
	 * 添加Mobile Position设备移动位置
	 * @param mobilePosition
	 * @return
	 */
	public boolean insertMobilePosition(MobilePosition mobilePosition);

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
	 * 移除代理流
	 * @param app
	 * @param stream
	 * @return
	 */
	public int deleteStreamProxy(String app, String stream);

	/**
	 * 按照是app和stream获取代理流
	 * @param app
	 * @param stream
	 * @return
	 */
	public StreamProxyItem queryStreamProxy(String app, String stream);

	/**
	 * 获取代理流
	 * @param page
	 * @param count
	 * @return
	 */
	PageInfo<StreamProxyItem> queryStreamProxyList(Integer page, Integer count);

	/**
	 * 根据国标ID获取平台关联的直播流
	 * @param platformId
	 * @param channelId
	 * @return
	 */
	GbStream queryStreamInParentPlatform(String platformId, String channelId);

	/**
	 * 获取平台关联的直播流
	 * @param platformId
	 * @return
	 */
	List<DeviceChannel> queryGbStreamListInPlatform(String platformId);

	/**
	 * 移除单个推流
	 * @param app
	 * @param stream
	 */
	int removeMedia(String app, String stream);

	/**
	 * 设置流离线
	 */
	int mediaOffline(String app, String streamId);

	/**
	 * 根据媒体ID获取启用/不启用的代理列表
	 * @param id 媒体ID
	 * @param enable 启用/不启用
	 * @return
	 */
	List<StreamProxyItem> getStreamProxyListForEnableInMediaServer(String id,  boolean enable);

	/**
	 * 通道上线
	 * @param channelId 通道ID
	 */
	void deviceChannelOnline(String deviceId, String channelId);

	/**
	 * 通道离线
	 * @param channelId 通道ID
	 */
	void deviceChannelOffline(String deviceId, String channelId);

	/**
	 * 通过app与stream获取StreamProxy
	 * @param app
	 * @param streamId
	 * @return
	 */
    StreamProxyItem getStreamProxyByAppAndStream(String app, String streamId);

	int updateStreamGPS(List<GPSMsgInfo> gpsMsgInfo);

	List<ParentPlatform> queryPlatFormListForGBWithGBId(String channelId, List<String> platforms);

	List<ParentPlatform> queryPlatFormListForStreamWithGBId(String app, String stream, List<String> platforms);

	GbStream getGbStream(String app, String streamId);


	List<ChannelSourceInfo> getChannelSource(String platformId, String gbId);

    void updateChannelPosition(DeviceChannel deviceChannel);


	List<DeviceChannelExtend> queryChannelsByDeviceId(String serial, List<String> channelIds, Boolean online);

	List<ParentPlatform> queryEnablePlatformListWithAsMessageChannel();

	List<Device> queryDeviceWithAsMessageChannel();
}
