package com.genersoft.iot.vmp.storager;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.dao.dto.ChannelSourceInfo;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
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
	 * 添加设备通道
	 *
	 * @param deviceId 设备id
	 * @param channel 通道
	 */
	public void updateChannel(String deviceId, DeviceChannel channel);

	/**
	 * 批量添加设备通道
	 *
	 * @param deviceId 设备id
	 * @param channels 多个通道
	 */
	public int updateChannels(String deviceId, List<DeviceChannel> channels);

	/**
	 * 开始播放
	 * @param deviceId 设备id
	 * @param channelId 通道ID
	 * @param streamId 流地址
	 */
	public void startPlay(String deviceId, String channelId, String streamId);

	/**
	 * 停止播放
	 * @param deviceId 设备id
	 * @param channelId 通道ID
	 */
	public void stopPlay(String deviceId, String channelId);
	
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
	public PageInfo queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, Boolean online, Boolean catalogUnderDevice, int page, int count);
	
	public List<DeviceChannel> queryChannelsByDeviceIdWithStartAndLimit(String deviceId, String query, Boolean hasSubChannel, Boolean online, int start, int limit);


	/**
	 * 获取某个设备的通道列表
	 *
	 * @param deviceId 设备ID
	 * @return
	 */
	public List<DeviceChannel> queryChannelsByDeviceId(String deviceId);
	public List<DeviceChannel> queryOnlineChannelsByDeviceId(String deviceId);

	/**
	 * 获取某个设备的通道
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 */
	public DeviceChannel queryChannel(String deviceId, String channelId);

	/**
	 * 删除通道
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 */
	public int delChannel(String deviceId, String channelId);

	/**
	 * 获取多个设备
	 * @param page 当前页数
	 * @param count 每页数量
	 * @return List<Device> 设备对象数组
	 */
	public PageInfo<Device> queryVideoDeviceList(int page, int count);

	/**
	 * 获取多个设备
	 *
	 * @return List<Device> 设备对象数组
	 */
	public List<Device> queryVideoDeviceList();

	/**   
	 * 删除设备
	 * 
	 * @param deviceId 设备ID
	 * @return true：删除成功  false：删除失败
	 */
	public boolean delete(String deviceId);
	
	/**   
	 * 更新设备在线
	 * 
	 * @param deviceId 设备ID
	 * @return true：更新成功  false：更新失败
	 */
	public boolean online(String deviceId);
	
	/**   
	 * 更新设备离线
	 * 
	 * @param deviceId 设备ID
	 * @return true：更新成功  false：更新失败
	 */
	public boolean outline(String deviceId);

	/**
	 * 更新所有设备离线
	 *
	 * @return true：更新成功  false：更新失败
	 */
	public boolean outlineForAll();


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
	 * 更新上级平台
	 * @param parentPlatform
	 */
	boolean updateParentPlatform(ParentPlatform parentPlatform);


	/**
	 * 添加上级平台
	 * @param parentPlatform
	 */
	boolean addParentPlatform(ParentPlatform parentPlatform);

	/**
	 * 删除上级平台
	 * @param parentPlatform
	 */
	boolean deleteParentPlatform(ParentPlatform parentPlatform);


	/**
	 * 分页获取上级平台
	 * @param page
	 * @param count
	 * @return
	 */
	PageInfo<ParentPlatform> queryParentPlatformList(int page, int count);

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
	 * 所有平台离线
	 */
	void outlineForAllParentPlatform();

	/**
	 * 查询通道信息，不区分设备(已关联平台或全部)
	 */
	PageInfo<ChannelReduce> queryAllChannelList(int page, int count, String query, Boolean online, Boolean channelType, String platformId, String catalogId);

	/**
	 * 查询设备的通道信息
	 */
	List<DeviceChannelInPlatform> queryChannelListInParentPlatform(String platformId);


	/**
	 * 更新上级平台的通道信息
	 * @param platformId
	 * @param channelReduces
	 * @return
	 */
	int updateChannelForGB(String platformId, List<ChannelReduce> channelReduces, String catalogId);

	/**
	 *  移除上级平台的通道信息
	 * @param platformId
	 * @param channelReduces
	 * @return
	 */
	int delChannelForGB(String platformId, List<ChannelReduce> channelReduces);


    DeviceChannel queryChannelInParentPlatform(String platformId, String channelId);

    List<PlatformCatalog> queryChannelInParentPlatformAndCatalog(String platformId, String catalogId);
    List<PlatformCatalog> queryStreamInParentPlatformAndCatalog(String platformId, String catalogId);

    Device queryVideoDeviceByPlatformIdAndChannelId(String platformId, String channelId);


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
	 * 删除指定设备的所有移动位置
	 * @param deviceId
	 */
	public int clearMobilePositionsByDeviceId(String deviceId);

	/**
	 * 新增代理流
	 * @param streamProxyDto
	 * @return
	 */
	public boolean addStreamProxy(StreamProxyItem streamProxyDto);

	/**
	 * 更新代理流
	 * @param streamProxyDto
	 * @return
	 */
	public boolean updateStreamProxy(StreamProxyItem streamProxyDto);

	/**
	 * 移除代理流
	 * @param app
	 * @param stream
	 * @return
	 */
	public int deleteStreamProxy(String app, String stream);

	/**
	 * 按照是否启用获取代理流
	 * @param enable
	 * @return
	 */
	public List<StreamProxyItem> getStreamProxyListForEnable(boolean enable);

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
	List<GbStream> queryGbStreamListInPlatform(String platformId);

	/**
	 * 批量更新推流列表
	 * @param streamPushItems
	 */
	void updateMediaList(List<StreamPushItem> streamPushItems);

	/**
	 * 更新单个推流
	 * @param streamPushItem
	 */
	void updateMedia(StreamPushItem streamPushItem);

	/**
	 * 移除单个推流
	 * @param app
	 * @param stream
	 */
	int removeMedia(String app, String stream);


	/**
	 * 获取但个推流
	 * @param app
	 * @param stream
	 * @return
	 */
	StreamPushItem getMedia(String app, String stream);


	/**
	 * 清空推流列表
	 */
	void clearMediaList();

	/**
	 * 设置流离线
	 * @param app
	 * @param streamId
	 */
	int mediaOutline(String app, String streamId);

	/**
	 * 设置平台在线/离线
	 * @param online
	 */
	void updateParentPlatformStatus(String platformGbID, boolean online);

	/**
	 * 根据媒体ID获取启用/不启用的代理列表
	 * @param id 媒体ID
	 * @param enable 启用/不启用
	 * @return
	 */
	List<StreamProxyItem> getStreamProxyListForEnableInMediaServer(String id,  boolean enable);

	/**
	 * 根据通道ID获取其所在设备
	 * @param channelId  通道ID
	 * @return
	 */
    Device queryVideoDeviceByChannelId(String channelId);

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

	/**
	 * catlog查询结束后完全重写通道信息
	 * @param deviceId
	 * @param deviceChannelList
	 */
	boolean resetChannels(String deviceId, List<DeviceChannel> deviceChannelList);

	/**
	 * 获取目录信息
	 * @param platformId
	 * @param parentId
	 * @return
	 */
    List<PlatformCatalog> getChildrenCatalogByPlatform(String platformId, String parentId);

	int addCatalog(PlatformCatalog platformCatalog);

	PlatformCatalog getCatalog(String id);

	int delCatalog(String id);

	int updateCatalog(PlatformCatalog platformCatalog);

	int setDefaultCatalog(String platformId, String catalogId);

	List<PlatformCatalog> queryCatalogInPlatform(String serverGBId);

    int delRelation(PlatformCatalog platformCatalog);

	int updateStreamGPS(List<GPSMsgInfo> gpsMsgInfo);

	List<ParentPlatform> queryPlatFormListForGBWithGBId(String channelId, List<String> platforms);

	List<ParentPlatform> queryPlatFormListForStreamWithGBId(String app, String stream, List<String> platforms);

	GbStream getGbStream(String app, String streamId);

	void delCatalogByPlatformId(String serverGBId);

	void delRelationByPlatformId(String serverGBId);

    PlatformCatalog queryDefaultCatalogInPlatform(String platformId);

	List<ChannelSourceInfo> getChannelSource(String platformId, String gbId);

    void updateChannelPotion(String deviceId, String channelId, double longitude, double latitude);
}
