package com.genersoft.iot.vmp.storager.jdbc;

import java.util.List;
import java.util.Map;

import com.genersoft.iot.vmp.common.PageResult;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

/**    
 * @Description:视频设备数据存储-jdbc实现  
 * @author: swwheihei
 * @date:   2020年5月6日 下午2:28:12     
 */
@Component("jdbcStorager")
public class VideoManagerJdbcStoragerImpl implements IVideoManagerStorager {

	@Override
	public boolean updateMediaInfo(MediaServerConfig mediaServerConfig) {
		return false;
	}

	@Override
	public MediaServerConfig getMediaInfo() {
		return null;
	}

	/**
	 * 根据设备ID判断设备是否存在
	 * 
	 * @param deviceId 设备ID
	 * @return true:存在  false：不存在
	 */ 
	@Override
	public boolean exists(String deviceId) {
		// TODO Auto-generated method stub
		return false;
	}

	/**   
	 * 视频设备创建
	 * 
	 * @param device 设备对象
	 * @return true：创建成功  false：创建失败
	 */ 
	@Override
	public boolean create(Device device) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updateDevice(Device device) {
		return false;
	}

	@Override
	public void updateChannel(String deviceId, DeviceChannel channel) {

	}


	/**   
	 * 获取设备
	 * 
	 * @param deviceId 设备ID
	 * @return Device 设备对象
	 */  
	@Override
	public Device queryVideoDevice(String deviceId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageResult queryChannelsByDeviceId(String deviceId, String query, Boolean hasSubChannel, String online, int page, int count) {
		return null;
	}


	@Override
	public List<DeviceChannel> queryChannelsByDeviceId(String deviceId) {
		return null;
	}

	@Override
	public DeviceChannel queryChannel(String deviceId, String channelId) {
		return null;
	}

	@Override
	public PageResult<Device> queryVideoDeviceList(String[] deviceIds, int page, int count) {
		return null;
	}

	/**   
	 * 获取多个设备
	 * 
	 * @param deviceIds 设备ID数组
	 * @return List<Device> 设备对象数组
	 */  
	@Override
	public List<Device> queryVideoDeviceList(String[] deviceIds) {
		// TODO Auto-generated method stub
		return null;
	}

	/**   
	 * 删除设备
	 * 
	 * @param deviceId 设备ID
	 * @return true：删除成功  false：删除失败
	 */  
	@Override
	public boolean delete(String deviceId) {
		// TODO Auto-generated method stub
		return false;
	}

	/**   
	 * 更新设备在线
	 * 
	 * @param deviceId 设备ID
	 * @return true：更新成功  false：更新失败
	 */ 
	@Override
	public boolean online(String deviceId) {
		// TODO Auto-generated method stub
		return false;
	}

	/**   
	 * 更新设备离线
	 * 
	 * @param deviceId 设备ID
	 * @return true：更新成功  false：更新失败
	 */ 
	@Override
	public boolean outline(String deviceId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stopPlay(StreamInfo streamInfo) {
		return false;
	}

	@Override
	public StreamInfo queryPlay(StreamInfo streamInfo) {
		return null;
	}

	@Override
	public PageResult querySubChannels(String deviceId, String channelId, String query, Boolean hasSubChannel, String online, int page, int count) {
		return null;
	}

	@Override
	public void updateCatch() {
		System.out.println("##################");
	}

	@Override
	public void cleanChannelsForDevice(String deviceId) {

	}

	@Override
	public boolean startPlay(StreamInfo stream) {
		return false;
	}

	@Override
	public StreamInfo queryPlayBySSRC(String ssrc) {
		return null;
	}

	@Override
	public StreamInfo queryPlayByDevice(String deviceId, String code) {
		return null;
	}

	@Override
	public Map<String, StreamInfo> queryPlayByDeviceId(String deviceId) {

		return null;
	}

	@Override
	public boolean startPlayback(StreamInfo streamInfo) {
		return false;
	}

	@Override
	public boolean stopPlayback(StreamInfo streamInfo) {
		return false;
	}

	@Override
	public StreamInfo queryPlaybackByDevice(String deviceId, String channelId) {
		return null;
	}

	@Override
	public StreamInfo queryPlaybackBySSRC(String ssrc) {
		return null;
	}

	@Override
	public boolean updateParentPlatform(ParentPlatform parentPlatform) {
		return false;
	}

	@Override
	public boolean deleteParentPlatform(ParentPlatform parentPlatform) {
		return false;
	}

	@Override
	public PageResult<ParentPlatform> queryParentPlatformList(int page, int count) {
		return null;
	}

	@Override
	public ParentPlatform queryParentPlatById(String platformGbId) {
		return null;
	}
}
