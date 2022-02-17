package com.genersoft.iot.vmp.vmanager.gb28181.device;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.DeviceOffLineDetector;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.bean.DeviceChannelTree;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.UUID;

@Api(tags = "国标设备查询", value = "国标设备查询")
@SuppressWarnings("rawtypes")
@CrossOrigin
@RestController
@RequestMapping("/api/device/query")
public class DeviceQuery {
	
	private final static Logger logger = LoggerFactory.getLogger(DeviceQuery.class);
	
	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;
	
	@Autowired
	private SIPCommander cmder;
	
	@Autowired
	private DeferredResultHolder resultHolder;
	
	@Autowired
	private DeviceOffLineDetector offLineDetector;

	@Autowired
	private IDeviceService deviceService;

	/**
	 * 使用ID查询国标设备
	 * @param deviceId 国标ID
	 * @return 国标设备
	 */
	@ApiOperation("使用ID查询国标设备")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
	})
	@GetMapping("/devices/{deviceId}")
	public ResponseEntity<Device> devices(@PathVariable String deviceId){
		
//		if (logger.isDebugEnabled()) {
//			logger.debug("查询视频设备API调用，deviceId：" + deviceId);
//		}
		
		Device device = storager.queryVideoDevice(deviceId);
		return new ResponseEntity<>(device,HttpStatus.OK);
	}

	/**
	 * 分页查询国标设备
	 * @param page 当前页
	 * @param count 每页查询数量
	 * @return 分页国标列表
	 */
	@ApiOperation("分页查询国标设备")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "page", value = "当前页", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "count", value = "每页查询数量", required = true, dataTypeClass = Integer.class),
	})
	@GetMapping("/devices")
	public PageInfo<Device> devices(int page, int count){
		
//		if (logger.isDebugEnabled()) {
//			logger.debug("查询所有视频设备API调用");
//		}
		
		return storager.queryVideoDeviceList(page, count);
	}

	/**
	 * 分页查询通道数
	 *
	 * @param deviceId 设备id
	 * @param page 当前页
	 * @param count 每页条数
	 * @param query 查询内容
	 * @param online 是否在线  在线 true / 离线 false
	 * @param channelType 设备 false/子目录 true
	 * @return 通道列表
	 */
	@ApiOperation("分页查询通道")
	@GetMapping("/devices/{deviceId}/channels")
	@ApiImplicitParams({
			@ApiImplicitParam(name="deviceId", value = "设备id", required = true ,dataTypeClass = String.class),
			@ApiImplicitParam(name="page", value = "当前页", required = true ,dataTypeClass = Integer.class),
			@ApiImplicitParam(name="count", value = "每页查询数量", required = true ,dataTypeClass = Integer.class),
			@ApiImplicitParam(name="query", value = "查询内容" ,dataTypeClass = String.class),
			@ApiImplicitParam(name="online", value = "是否在线"  ,dataTypeClass = Boolean.class),
			@ApiImplicitParam(name="channelType", value = "设备/子目录-> false/true" ,dataTypeClass = Boolean.class),
	})
	public ResponseEntity<PageInfo> channels(@PathVariable String deviceId,
											   int page, int count,
											   @RequestParam(required = false) String query,
											   @RequestParam(required = false) Boolean online,
											   @RequestParam(required = false) Boolean channelType) {
//		if (logger.isDebugEnabled()) {
//			logger.debug("查询视频设备通道API调用");
//		}
		if (StringUtils.isEmpty(query)) {
			query = null;
		}

		PageInfo pageResult = storager.queryChannelsByDeviceId(deviceId, query, channelType, online, page, count);
		return new ResponseEntity<>(pageResult,HttpStatus.OK);
	}

	/**
	 * 同步设备通道
	 * @param deviceId 设备id
	 * @return
	 */
	@ApiOperation("同步设备通道")
	@ApiImplicitParams({
			@ApiImplicitParam(name="deviceId", value = "设备id", required = true, dataTypeClass = String.class),
	})
	@PostMapping("/devices/{deviceId}/sync")
	public DeferredResult<ResponseEntity<Device>> devicesSync(@PathVariable String deviceId){
		
		if (logger.isDebugEnabled()) {
			logger.debug("设备通道信息同步API调用，deviceId：" + deviceId);
		}
		Device device = storager.queryVideoDevice(deviceId);
		String key = DeferredResultHolder.CALLBACK_CMD_CATALOG + deviceId;
		String uuid = UUID.randomUUID().toString();
		// 默认超时时间为30分钟
		DeferredResult<ResponseEntity<Device>> result = new DeferredResult<ResponseEntity<Device>>(30*60*1000L);
		result.onTimeout(()->{
			logger.warn("设备[{}]通道信息同步超时", deviceId);
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setKey(key);
			msg.setId(uuid);
			WVPResult<Object> wvpResult = new WVPResult<>();
			wvpResult.setCode(-1);
			wvpResult.setData(device);
			wvpResult.setMsg("更新超时");
			msg.setData(wvpResult);
			resultHolder.invokeAllResult(msg);

		});
		// 等待其他相同请求返回时一起返回
		if (resultHolder.exist(key, null)) {
			return result;
		}
        cmder.catalogQuery(device, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setKey(key);
			msg.setId(uuid);
			WVPResult<Object> wvpResult = new WVPResult<>();
			wvpResult.setCode(-1);
			wvpResult.setData(device);
			wvpResult.setMsg(String.format("同步通道失败，错误码： %s, %s", event.statusCode, event.msg));
			msg.setData(wvpResult);
			resultHolder.invokeAllResult(msg);
		});

        resultHolder.put(key, uuid, result);
        return result;
	}

	/**
	 * 移除设备
	 * @param deviceId 设备id
	 * @return
	 */
	@ApiOperation("移除设备")
	@ApiImplicitParams({
			@ApiImplicitParam(name="deviceId", value = "设备id", required = true, dataTypeClass = String.class),
	})
	@DeleteMapping("/devices/{deviceId}/delete")
	public ResponseEntity<String> delete(@PathVariable String deviceId){
		
		if (logger.isDebugEnabled()) {
			logger.debug("设备信息删除API调用，deviceId：" + deviceId);
		}
		
		if (offLineDetector.isOnline(deviceId)) {
			return new ResponseEntity<String>("不允许删除在线设备！", HttpStatus.NOT_ACCEPTABLE);
		}
		// 清除redis记录
		boolean isSuccess = storager.delete(deviceId);
		if (isSuccess) {
			redisCatchStorage.clearCatchByDeviceId(deviceId);
			JSONObject json = new JSONObject();
			json.put("deviceId", deviceId);
			return new ResponseEntity<>(json.toString(),HttpStatus.OK);
		} else {
			logger.warn("设备信息删除API调用失败！");
			return new ResponseEntity<String>("设备信息删除API调用失败！", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 分页查询子目录通道
	 * @param deviceId 通道id
	 * @param channelId 通道id
	 * @param page 当前页
	 * @param count 每页条数
	 * @param query 查询内容
	 * @param online 是否在线
	 * @param channelType 通道类型
	 * @return 子通道列表
	 */
	@ApiOperation("分页查询子目录通道")
	@ApiImplicitParams({
			@ApiImplicitParam(name="deviceId", value = "设备id", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name="channelId", value = "通道id", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name="page", value = "当前页", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name="count", value = "每页条数", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name="query", value = "查询内容", dataTypeClass = String.class),
			@ApiImplicitParam(name="online", value = "是否在线", dataTypeClass = String.class),
			@ApiImplicitParam(name="channelType", value = "通道类型， 子目录", dataTypeClass = Boolean.class),
	})
	@GetMapping("/sub_channels/{deviceId}/{channelId}/channels")
	public ResponseEntity<PageInfo> subChannels(@PathVariable String deviceId,
												  @PathVariable String channelId,
												  int page,
												  int count,
												  @RequestParam(required = false) String query,
												  @RequestParam(required = false) String online,
												  @RequestParam(required = false) Boolean channelType){

//		if (logger.isDebugEnabled()) {
//			logger.debug("查询所有视频通道API调用");
//		}
		DeviceChannel deviceChannel = storager.queryChannel(deviceId,channelId);
		if (deviceChannel == null) {
			PageInfo<DeviceChannel> deviceChannelPageResult = new PageInfo<>();
			return new ResponseEntity<>(deviceChannelPageResult,HttpStatus.OK);
		}

		PageInfo pageResult = storager.querySubChannels(deviceId, channelId, query, channelType, online, page, count);
		return new ResponseEntity<>(pageResult,HttpStatus.OK);
	}

	/**
	 * 更新通道信息
	 * @param deviceId 设备id
	 * @param channel 通道
	 * @return
	 */
	@ApiOperation("更新通道信息")
	@ApiImplicitParams({
			@ApiImplicitParam(name="deviceId", value = "设备id", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name="channel", value = "通道", required = true, dataTypeClass = String.class),
	})
	@PostMapping("/channel/update/{deviceId}")
	public ResponseEntity<PageInfo> updateChannel(@PathVariable String deviceId,DeviceChannel channel){
		storager.updateChannel(deviceId, channel);
		return new ResponseEntity<>(null,HttpStatus.OK);
	}

	/**
	 * 修改数据流传输模式
	 * @param deviceId 设备id
	 * @param streamMode 数据流传输模式
	 * @return
	 */
	@ApiOperation("修改数据流传输模式")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备id", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "streamMode", value = "数据流传输模式, 取值：" +
					"UDP（udp传输），TCP-ACTIVE（tcp主动模式,暂不支持），TCP-PASSIVE（tcp被动模式）", dataTypeClass = String.class),
	})
	@PostMapping("/transport/{deviceId}/{streamMode}")
	public ResponseEntity<PageInfo> updateTransport(@PathVariable String deviceId, @PathVariable String streamMode){
		Device device = storager.queryVideoDevice(deviceId);
		device.setStreamMode(streamMode);
		storager.updateDevice(device);
		return new ResponseEntity<>(null,HttpStatus.OK);
	}

	/**
	 * 更新设备信息
	 * @param device 设备信息
	 * @return
	 */
	@ApiOperation("更新设备信息")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "device", value = "设备信息", required = true, dataTypeClass = Device.class)
	})
	@PostMapping("/device/update/")
	public ResponseEntity<WVPResult<String>> updateDevice(Device device){

		if (device != null && device.getDeviceId() != null) {
			Device deviceInStore = storager.queryVideoDevice(device.getDeviceId());
			if (!StringUtils.isEmpty(device.getName())) deviceInStore.setName(device.getName());
			if (!StringUtils.isEmpty(device.getCharset())) deviceInStore.setCharset(device.getCharset());
			if (!StringUtils.isEmpty(device.getMediaServerId())) deviceInStore.setMediaServerId(device.getMediaServerId());

			if ((deviceInStore.getSubscribeCycleForCatalog() <=0 && device.getSubscribeCycleForCatalog() > 0)
					|| deviceInStore.getSubscribeCycleForCatalog() != device.getSubscribeCycleForCatalog()) {
				deviceInStore.setSubscribeCycleForCatalog(device.getSubscribeCycleForCatalog());
				// 开启订阅
				deviceService.addCatalogSubscribe(deviceInStore);
			}
			if (deviceInStore.getSubscribeCycleForCatalog() > 0 && device.getSubscribeCycleForCatalog() <= 0) {
				deviceInStore.setSubscribeCycleForCatalog(device.getSubscribeCycleForCatalog());
				// 取消订阅
				deviceService.removeCatalogSubscribe(deviceInStore);
			}

			storager.updateDevice(deviceInStore);
			cmder.deviceInfoQuery(deviceInStore);
		}
		WVPResult<String> result = new WVPResult<>();
		result.setCode(0);
		result.setMsg("success");
		return new ResponseEntity<>(result,HttpStatus.OK);
	}

	/**
	 * 设备状态查询请求API接口
	 * 
	 * @param deviceId 设备id
	 */
	@ApiOperation("设备状态查询")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备id", required = true, dataTypeClass = String.class),
	})
	@GetMapping("/devices/{deviceId}/status")
	public DeferredResult<ResponseEntity<String>> deviceStatusApi(@PathVariable String deviceId) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备状态查询API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		String uuid = UUID.randomUUID().toString();
		String key = DeferredResultHolder.CALLBACK_CMD_DEVICESTATUS + deviceId;
		cmder.deviceStatusQuery(device, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(String.format("获取设备状态失败，错误码： %s, %s", event.statusCode, event.msg));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(2*1000L);
		result.onTimeout(()->{
			logger.warn(String.format("获取设备状态超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("Timeout. Device did not response to this command.");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_DEVICESTATUS + deviceId, uuid, result);
		return result;
	}

	/**
	 * 设备报警查询请求API接口
	 * @param deviceId 设备id
	 * @param startPriority	报警起始级别（可选）
	 * @param endPriority	报警终止级别（可选）
	 * @param alarmMethod	报警方式条件（可选）
	 * @param alarmType		报警类型
	 * @param startTime		报警发生起始时间（可选）
	 * @param endTime		报警发生终止时间（可选）
	 * @return				true = 命令发送成功
	 */
	@ApiOperation("设备报警查询")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备id", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "startPriority", value = "报警起始级别", dataTypeClass = String.class),
			@ApiImplicitParam(name = "endPriority", value = "报警终止级别", dataTypeClass = String.class),
			@ApiImplicitParam(name = "alarmMethod", value = "报警方式条件", dataTypeClass = String.class),
			@ApiImplicitParam(name = "alarmType", value = "报警类型", dataTypeClass = String.class),
			@ApiImplicitParam(name = "startTime", value = "报警发生起始时间", dataTypeClass = String.class),
			@ApiImplicitParam(name = "endTime", value = "报警发生终止时间", dataTypeClass = String.class),
	})
	@GetMapping("/alarm/{deviceId}")
	public DeferredResult<ResponseEntity<String>> alarmApi(@PathVariable String deviceId,
														@RequestParam(required = false) String startPriority, 
														@RequestParam(required = false) String endPriority, 
														@RequestParam(required = false) String alarmMethod,
														@RequestParam(required = false) String alarmType,
														@RequestParam(required = false) String startTime,
														@RequestParam(required = false) String endTime) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备报警查询API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		String key = DeferredResultHolder.CALLBACK_CMD_ALARM + deviceId;
		String uuid = UUID.randomUUID().toString();
		cmder.alarmInfoQuery(device, startPriority, endPriority, alarmMethod, alarmType, startTime, endTime, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(String.format("设备报警查询失败，错误码： %s, %s",event.statusCode, event.msg));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String >> (3 * 1000L);
		result.onTimeout(()->{
			logger.warn(String.format("设备报警查询超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("设备报警查询超时");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_ALARM + deviceId, uuid, result);
		return result;
	}

	@GetMapping("/{deviceId}/tree")
	@ApiOperation(value = "通道树形结构", notes = "通道树形结构")
	public WVPResult<List<DeviceChannelTree>> tree(@PathVariable String deviceId) {
		return WVPResult.Data(storager.tree(deviceId));
	}
}
