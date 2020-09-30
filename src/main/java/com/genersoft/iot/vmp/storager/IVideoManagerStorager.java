package com.genersoft.iot.vmp.storager;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.PageResult;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;

/**    
 * @Description:视频设备数据存储接口
 * @author: swwheihei
 * @date:   2020年5月6日 下午2:14:31     
 */
public interface IVideoManagerStorager {

	/**
	 * 更新流媒体信息
	 * @param mediaServerConfig
	 * @return
	 */
	public boolean updateMediaInfo(MediaServerConfig mediaServerConfig);

	/**
	 * 获取流媒体信息
	 * @return
	 */
	public MediaServerConfig getMediaInfo();

	/**   
	 * 根据设备ID判断设备是否存在
	 * 
	 * @param deviceId 设备ID
	 * @return true:存在  false：不存在
	 */
	public boolean exists(String deviceId);
	
	/**   
	 * 视频设备创建
	 * 
	 * @param device 设备对象
	 * @return true：创建成功  false：创建失败
	 */
	public boolean create(Device device);
	
	/**   
	 * 视频设备更新
	 * 
	 * @param device 设备对象
	 * @return true：创建成功  false：创建失败
	 */
	public boolean updateDevice(Device device);

	/**
	 * 添加设备通道
	 *
	 * @param deviceId 设备id
	 * @param channel 通道
	 */
	public void updateChannel(String deviceId, DeviceChannel channel);
	
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
	public PageResult queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, String online, int page, int count);

	/**
	 * 获取某个设备的通道列表
	 *
	 * @param deviceId 设备ID
	 * @return
	 */
	public List<DeviceChannel> queryChannelsByDeviceId(String deviceId);
	/**
	 * 获取某个设备的通道
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 */
	public DeviceChannel queryChannel(String deviceId, String channelId);

	/**   
	 * 获取多个设备
	 * 
	 * @param deviceIds 设备ID数组
	 * @return List<Device> 设备对象数组
	 */
	public PageResult<Device> queryVideoDeviceList(String[] deviceIds, int page, int count);

	/**
	 * 获取多个设备
	 *
	 * @param deviceIds 设备ID数组
	 * @return List<Device> 设备对象数组
	 */
	public List<Device> queryVideoDeviceList(String[] deviceIds);

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
	 * 开始播放时将流存入
	 *
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 * @param stream 流信息
	 * @return
	 */
	public boolean startPlay(String deviceId, String channelId, StreamInfo stream);

	/**
	 * 停止播放时删除
	 *
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 * @return
	 */
	public boolean stopPlay(String deviceId, String channelId);

	/**
	 * 查找视频流
	 *
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 * @return
	 */
	public StreamInfo queryPlay(String deviceId, String channelId);

	/**
	 * 查询子设备
	 *
	 * @param deviceId
	 * @param channelId
	 * @param page
	 * @param count
	 * @return
	 */
    PageResult querySubChannels(String deviceId, String channelId, String query, Boolean hasSubChannel, String online, int page, int count);

	/**
	 * 更新缓存
	 */
	public void updateCatch();
}
