package com.genersoft.iot.vmp.storager;

import com.genersoft.iot.vmp.common.Page;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import com.github.pagehelper.PageInfo;

import java.util.List;
 /**
 * @description:视频设备数据存储接口
 * @author: swwheihei
 * @date: 2020年5月6日 下午2:14:31
 */
@SuppressWarnings("rawtypes")
public interface IVideoManagerStorager {

    /**
     * 根据设备ID判断设备是否存在
     *
     * @param deviceId 设备ID
     * @return true:存在  false：不存在
     */
    boolean exists(String deviceId);

    /**
     * 视频设备创建
     *
     * @param device 设备对象
     * @return true：创建成功  false：创建失败
     */
    boolean create(Device device);

    /**
     * 视频设备更新
     *
     * @param device 设备对象
     * @return true：创建成功  false：创建失败
     */
    boolean updateDevice(Device device);

    /**
     * 添加设备通道
     *
     * @param deviceId 设备id
     * @param channel  通道
     */
    void updateChannel(String deviceId, DeviceChannel channel);

    /**
     * 开始播放
     *
     * @param deviceId  设备id
     * @param channelId 通道ID
     * @param streamId  流地址
     */
    void startPlay(String deviceId, String channelId, String streamId);

    /**
     * 停止播放
     *
     * @param deviceId  设备id
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
     * @param pageNo     分页 当前页
     * @param pageSize    每页数量
     * @return
     */
    Page<DeviceChannel> queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, Boolean online, int pageNo, int pageSize);

    /**
     * 获取某个设备的通道列表
     *
     * @param deviceId 设备ID
     * @return
     */
    List<DeviceChannel> queryChannelsByDeviceId(String deviceId);

    /**
     * 获取某个设备的通道
     *
     * @param deviceId  设备ID
     * @param channelId 通道ID
     */
    DeviceChannel queryChannel(String deviceId, String channelId);

    /**
     * 获取多个设备
     *
     * @param page  当前页数
     * @param count 每页数量
     * @return List<Device> 设备对象数组
     */
    Page<Device> queryVideoDeviceList(int page, int count, Device device);

    /**
     * 获取多个设备
     *
     * @return List<Device> 设备对象数组
     */
    List<Device> queryVideoDeviceList();

    /**
     * 删除设备
     *
     * @param deviceId 设备ID
     * @return true：删除成功  false：删除失败
     */
    boolean delete(String deviceId);

    /**
     * 更新设备在线
     *
     * @param deviceId 设备ID
     * @return true：更新成功  false：更新失败
     */
    boolean online(String deviceId);

    /**
     * 更新设备离线
     *
     * @param deviceId 设备ID
     * @return true：更新成功  false：更新失败
     */
    boolean outline(String deviceId);

    /**
     * 更新所有设备离线
     *
     * @return true：更新成功  false：更新失败
     */
    boolean outlineForAll();


    /**
     * 查询子设备
     *
     * @param deviceId
     * @param channelId
     * @param page
     * @param count
     * @return
     */
    PageInfo querySubChannels(String deviceId, String channelId, String query, Boolean hasSubChannel, String online, int page, int count);


    /**
     * 清空通道
     *
     * @param deviceId
     */
    void cleanChannelsForDevice(String deviceId);


    /**
     * 更新上级平台
     *
     * @param parentPlatform
     */
    boolean updateParentPlatform(ParentPlatform parentPlatform);


    /**
     * 添加上级平台
     *
     * @param parentPlatform
     */
    boolean addParentPlatform(ParentPlatform parentPlatform);

    /**
     * 删除上级平台
     *
     * @param parentPlatform
     */
    boolean deleteParentPlatform(ParentPlatform parentPlatform);


    /**
     * 分页获取上级平台
     *
     * @param page
     * @param count
     * @return
     */
    Page<ParentPlatform> queryParentPlatformList(int page, int count);

    /**
     * 获取所有已启用的平台
     *
     * @return
     */
    List<ParentPlatform> queryEnableParentPlatformList(boolean enable);

    /**
     * 获取上级平台
     *
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
    PageInfo<ChannelReduce> queryAllChannelList(int page, int count, String query, Boolean online, Boolean channelType, String platformId, Boolean inPlatform);

    /**
     * 查询设备的通道信息
     */
    List<ChannelReduce> queryChannelListInParentPlatform(String platformId);


    /**
     * 更新上级平台的通道信息
     *
     * @param platformId
     * @param channelReduces
     * @return
     */
    int updateChannelForGB(String platformId, List<ChannelReduce> channelReduces);

    /**
     * 移除上级平台的通道信息
     *
     * @param platformId
     * @param channelReduces
     * @return
     */
    int delChannelForGB(String platformId, List<ChannelReduce> channelReduces);


    DeviceChannel queryChannelInParentPlatform(String platformId, String channelId);

    Device queryVideoDeviceByPlatformIdAndChannelId(String platformId, String channelId);


    /**
     * 添加Mobile Position设备移动位置
     *
     * @param mobilePosition
     * @return
     */
    boolean insertMobilePosition(MobilePosition mobilePosition);

    /**
     * 查询移动位置轨迹
     *
     * @param deviceId
     * @param startTime
     * @param endTime
     */
    List<MobilePosition> queryMobilePositions(String deviceId, String startTime, String endTime);

    /**
     * 查询最新移动位置
     *
     * @param deviceId
     */
    MobilePosition queryLatestPosition(String deviceId);

    /**
     * 删除指定设备的所有移动位置
     *
     * @param deviceId
     */
    int clearMobilePositionsByDeviceId(String deviceId);

    /**
     * 新增代理流
     *
     * @param streamProxyDto
     * @return
     */
    boolean addStreamProxy(StreamProxyItem streamProxyDto);

    /**
     * 更新代理流
     *
     * @param streamProxyDto
     * @return
     */
    boolean updateStreamProxy(StreamProxyItem streamProxyDto);

    /**
     * 移除代理流
     *
     * @param app
     * @param stream
     * @return
     */
    int deleteStreamProxy(String app, String stream);

    /**
     * 按照是否启用获取代理流
     *
     * @param enable
     * @return
     */
    List<StreamProxyItem> getStreamProxyListForEnable(boolean enable);

    /**
     * 按照是app和stream获取代理流
     *
     * @param app
     * @param stream
     * @return
     */
    StreamProxyItem queryStreamProxy(String app, String stream);

    /**
     * 获取代理流
     *
     * @param page
     * @param count
     * @return
     */
    Page<StreamProxyItem> queryStreamProxyList(Integer page, Integer count, String query, Boolean enable);

    /**
     * 根据国标ID获取平台关联的直播流
     *
     * @param platformId
     * @param channelId
     * @return
     */
    GbStream queryStreamInParentPlatform(String platformId, String channelId);

    /**
     * 获取平台关联的直播流
     *
     * @param platformId
     * @return
     */
    List<GbStream> queryGbStreamListInPlatform(String platformId);

    /**
     * 批量更新推流列表
     *
     * @param streamPushItems
     */
    void updateMediaList(List<StreamPushItem> streamPushItems);

    /**
     * 更新单个推流
     *
     * @param streamPushItem
     */
    void updateMedia(StreamPushItem streamPushItem);

    /**
     * 移除单个推流
     *
     * @param app
     * @param stream
     */
    void removeMedia(String app, String stream);


    /**
     * 清空推流列表
     */
    void clearMediaList();

    /**
     * 设置流离线
     *
     * @param app
     * @param streamId
     */
    void mediaOutline(String app, String streamId);

    /**
     * 设置平台在线/离线
     *
     * @param online
     */
    void updateParentPlatformStatus(String platformGbID, boolean online);

    /**
     * 更新媒体节点
     *
     * @param mediaServerItem
     */
    void updateMediaServer(MediaServerItem mediaServerItem);

	/**
	 * 根据媒体ID获取启用/不启用的代理列表
	 * @param id 媒体ID
	 * @param b 启用/不启用
	 * @return
	 */
	List<StreamProxyItem> getStreamProxyListForEnableInMediaServer(String id, boolean b);

	/**
	 * 根据通道ID获取其所在设备
	 * @param channelId  通道ID
	 * @return
	 */
    Device queryVideoDeviceByChannelId(String channelId);
}
