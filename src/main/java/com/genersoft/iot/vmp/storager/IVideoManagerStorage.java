package com.genersoft.iot.vmp.storager;

import com.genersoft.iot.vmp.gb28181.bean.*;
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
	public boolean exists(String deviceId);
	
	/**   
	 * 获取设备
	 * 
	 * @param deviceId 设备ID
	 * @return DShadow 设备对象
	 */
	public Device queryVideoDevice(String deviceId);

	/**
	 * 获取某个设备的通道列表
	 *
	 * @param deviceId 设备ID
	 * @param page 分页 当前页
	 * @param count 每页数量
	 * @return
	 */
	public PageInfo<DeviceChannel> queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, Boolean online, Boolean catalogUnderDevice, int page, int count);

	/**
	 * 获取某个设备的通道
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 */
	public DeviceChannel queryChannel(String deviceId, String channelId);

	/**
	 * 获取多个设备
	 * @param page 当前页数
	 * @param count 每页数量
	 * @return List<Device> 设备对象数组
	 */
	public PageInfo<Device> queryVideoDeviceList(int page, int count,Boolean online);

	/**
	 * 获取多个设备
	 *
	 * @return List<Device> 设备对象数组
	 */
	public List<Device> queryVideoDeviceList(Boolean online);



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
	 * 查询通道信息，不区分设备(已关联平台或全部)
	 */
	PageInfo<ChannelReduce> queryAllChannelList(int page, int count, String query, Boolean online, Boolean channelType, String platformId, String catalogId);


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

	/**
	 * 获取目录信息
	 * @param platformId
	 * @param parentId
	 * @return
	 */
    List<PlatformCatalog> getChildrenCatalogByPlatform(String platformId, String parentId);

	int addCatalog(PlatformCatalog platformCatalog);

	PlatformCatalog getCatalog(String platformId, String id);

	int delCatalog(String platformId, String id);

	int updateCatalog(PlatformCatalog platformCatalog);

	int setDefaultCatalog(String platformId, String catalogId);

    int delRelation(PlatformCatalog platformCatalog);

	int updateStreamGPS(List<GPSMsgInfo> gpsMsgInfo);

	List<ParentPlatform> queryPlatFormListForGBWithGBId(String channelId, List<String> platforms);

	void delCatalogByPlatformId(String serverGBId);

	void delRelationByPlatformId(String serverGBId);

    PlatformCatalog queryDefaultCatalogInPlatform(String platformId);

	List<ChannelSourceInfo> getChannelSource(String platformId, String gbId);

	List<DeviceChannelExtend> queryChannelsByDeviceId(String serial, List<String> channelIds, Boolean online);

	List<ParentPlatform> queryEnablePlatformListWithAsMessageChannel();

	List<Device> queryDeviceWithAsMessageChannel();
}
