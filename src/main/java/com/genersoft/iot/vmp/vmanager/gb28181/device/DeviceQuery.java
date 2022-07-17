package com.genersoft.iot.vmp.vmanager.gb28181.device;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeHolder;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.CatalogSubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.BaseTree;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletResponse;
import javax.sip.DialogState;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

@Api(tags = "国标设备查询", value = "国标设备查询")
@SuppressWarnings("rawtypes")
@CrossOrigin
@RestController
@RequestMapping("/api/device/query")
public class DeviceQuery {
	
	private final static Logger logger = LoggerFactory.getLogger(DeviceQuery.class);
	
	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private IDeviceChannelService deviceChannelService;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;
	
	@Autowired
	private SIPCommander cmder;
	
	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private SubscribeHolder subscribeHolder;

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
			@ApiImplicitParam(name="catalogUnderDevice", value = "是否直属与设备的目录" ,dataTypeClass = Boolean.class),
	})
	public ResponseEntity<PageInfo> channels(@PathVariable String deviceId,
											   int page, int count,
											   @RequestParam(required = false) String query,
											   @RequestParam(required = false) Boolean online,
											   @RequestParam(required = false) Boolean channelType,
											   @RequestParam(required = false) Boolean catalogUnderDevice) {
//		if (logger.isDebugEnabled()) {
//			logger.debug("查询视频设备通道API调用");
//		}
		if (StringUtils.isEmpty(query)) {
			query = null;
		}

		PageInfo pageResult = storager.queryChannelsByDeviceId(deviceId, query, channelType, online, catalogUnderDevice, page, count);
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
	public WVPResult<SyncStatus> devicesSync(@PathVariable String deviceId){
		
		if (logger.isDebugEnabled()) {
			logger.debug("设备通道信息同步API调用，deviceId：" + deviceId);
		}
		Device device = storager.queryVideoDevice(deviceId);
		boolean status = deviceService.isSyncRunning(deviceId);
		// 已存在则返回进度
		if (status) {
			WVPResult<SyncStatus> wvpResult = new WVPResult<>();
			wvpResult.setCode(0);
			SyncStatus channelSyncStatus = deviceService.getChannelSyncStatus(deviceId);
			wvpResult.setData(channelSyncStatus);
			return wvpResult;
		}
		deviceService.sync(device);

		WVPResult<SyncStatus> wvpResult = new WVPResult<>();
		wvpResult.setCode(0);
		wvpResult.setMsg("开始同步");
		return wvpResult;
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

		// 清除redis记录
		boolean isSuccess = storager.delete(deviceId);
		if (isSuccess) {
			redisCatchStorage.clearCatchByDeviceId(deviceId);
			// 停止此设备的订阅更新
			Set<String> allKeys = dynamicTask.getAllKeys();
			for (String key : allKeys) {
				if (key.startsWith(deviceId)) {
					Runnable runnable = dynamicTask.get(key);
					if (runnable instanceof ISubscribeTask) {
						ISubscribeTask subscribeTask = (ISubscribeTask) runnable;
						subscribeTask.stop();
					}
					dynamicTask.stop(key);
				}
			}
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
			@ApiImplicitParam(name="online", value = "是否在线", dataTypeClass = Boolean.class),
			@ApiImplicitParam(name="channelType", value = "通道类型， 子目录", dataTypeClass = Boolean.class),
	})
	@GetMapping("/sub_channels/{deviceId}/{channelId}/channels")
	public ResponseEntity<PageInfo> subChannels(@PathVariable String deviceId,
												  @PathVariable String channelId,
												  int page,
												  int count,
												  @RequestParam(required = false) String query,
												  @RequestParam(required = false) Boolean online,
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
		deviceChannelService.updateChannel(deviceId, channel);
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
//		storager.updateDevice(device);
		deviceService.updateDevice(device);
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
			deviceService.updateDevice(device);
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
		DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(2*1000L);
		if(device == null) {
			result.setResult(new ResponseEntity(String.format("设备%s不存在", deviceId),HttpStatus.OK));
			return result;
		}
		cmder.deviceStatusQuery(device, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(String.format("获取设备状态失败，错误码： %s, %s", event.statusCode, event.msg));
			resultHolder.invokeResult(msg);
		});
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


	@GetMapping("/{deviceId}/sync_status")
	@ApiOperation(value = "获取通道同步进度", notes = "获取通道同步进度")
	public WVPResult<SyncStatus> getSyncStatus(@PathVariable String deviceId) {
		SyncStatus channelSyncStatus = deviceService.getChannelSyncStatus(deviceId);
		WVPResult<SyncStatus> wvpResult = new WVPResult<>();
		if (channelSyncStatus == null) {
			wvpResult.setCode(-1);
			wvpResult.setMsg("同步尚未开始");
		}else {
			wvpResult.setCode(0);
			wvpResult.setData(channelSyncStatus);
			if (channelSyncStatus.getErrorMsg() != null) {
				wvpResult.setMsg(channelSyncStatus.getErrorMsg());
			}
		}
		return wvpResult;
	}

	@GetMapping("/{deviceId}/subscribe_info")
	@ApiOperation(value = "获取设备的订阅状态", notes = "获取设备的订阅状态")
	public WVPResult<Map<String, String>> getSubscribeInfo(@PathVariable String deviceId) {
		Set<String> allKeys = dynamicTask.getAllKeys();
		Map<String, String> dialogStateMap = new HashMap<>();
		for (String key : allKeys) {
			if (key.startsWith(deviceId)) {
				ISubscribeTask subscribeTask = (ISubscribeTask)dynamicTask.get(key);
				DialogState dialogState = subscribeTask.getDialogState();
				if (dialogState == null) {
					continue;
				}
				if (subscribeTask instanceof CatalogSubscribeTask) {
					dialogStateMap.put("catalog", dialogState.toString());
				}else if (subscribeTask instanceof MobilePositionSubscribeTask) {
					dialogStateMap.put("mobilePosition", dialogState.toString());
				}
			}
		}
		WVPResult<Map<String, String>> wvpResult = new WVPResult<>();
		wvpResult.setCode(0);
		wvpResult.setData(dialogStateMap);
		return wvpResult;
	}

	@GetMapping("/snap/{deviceId}/{channelId}")
	@ApiOperation(value = "请求截图", notes = "请求截图")
	public void getSnap(HttpServletResponse resp, @PathVariable String deviceId, @PathVariable String channelId) {

		try {
			final InputStream in = Files.newInputStream(new File("snap" + File.separator + deviceId + "_" + channelId + ".jpg").toPath());
			resp.setContentType(MediaType.IMAGE_PNG_VALUE);
			IOUtils.copy(in, resp.getOutputStream());
		} catch (IOException e) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * 查询国标树
	 * @param deviceId 设备ID
	 * @param parentId 父ID
	 * @param page 当前页
	 * @param count 每页条数
	 * @return 国标设备
	 */
	@ApiOperation("查询国标树")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "parentId", value = "父ID", required = false, dataTypeClass = String.class),
			@ApiImplicitParam(name = "onlyCatalog", value = "只获取目录", required = false, dataTypeClass = Boolean.class),
			@ApiImplicitParam(name="page", value = "当前页", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name="count", value = "每页条数", required = true, dataTypeClass = Integer.class),
	})
	@GetMapping("/tree/{deviceId}")
	public ResponseEntity<PageInfo> getTree(@PathVariable String deviceId, @RequestParam(required = false) String parentId, @RequestParam(required = false) Boolean onlyCatalog, int page, int count){


		if (page <= 0) {
			page = 1;
		}
		if (onlyCatalog == null) {
			onlyCatalog = false;
		}

		List<BaseTree<DeviceChannel>> treeData = deviceService.queryVideoDeviceTree(deviceId, parentId, onlyCatalog);
		if (treeData == null || (page - 1) * count > treeData.size()) {
			PageInfo<BaseTree<DeviceChannel>> pageInfo = new PageInfo<>();
			pageInfo.setPageNum(page);
			pageInfo.setTotal(treeData == null? 0 : treeData.size());
			pageInfo.setSize(0);
			pageInfo.setList(new ArrayList<>());
			return new ResponseEntity<>(pageInfo,HttpStatus.OK);
		}

		int toIndex = Math.min(page * count, treeData.size());
		// 处理分页
		List<BaseTree<DeviceChannel>> trees = treeData.subList((page - 1) * count, toIndex);
		PageInfo<BaseTree<DeviceChannel>> pageInfo = new PageInfo<>();
		pageInfo.setPageNum(page);
		pageInfo.setTotal(treeData.size());
		pageInfo.setSize(trees.size());
		pageInfo.setList(trees);

		return new ResponseEntity<>(pageInfo,HttpStatus.OK);
	}


	/**
	 * 查询国标树下的通道
	 * @param deviceId 设备ID
	 * @param parentId 父ID
	 * @param page 当前页
	 * @param count 每页条数
	 * @return 国标设备
	 */
	@ApiOperation("查询国标树下的通道")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "parentId", value = "父ID", required = false, dataTypeClass = String.class),
			@ApiImplicitParam(name="page", value = "当前页", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name="count", value = "每页条数", required = true, dataTypeClass = Integer.class),
	})
	@GetMapping("/tree/channel/{deviceId}")
	public ResponseEntity<PageInfo> getChannelInTreeNode(@PathVariable String deviceId, @RequestParam(required = false) String parentId, int page, int count){


		if (page <= 0) {
			page = 1;
		}

		List<DeviceChannel> treeData = deviceService.queryVideoDeviceInTreeNode(deviceId, parentId);
		if (treeData == null || (page - 1) * count > treeData.size()) {
			PageInfo<BaseTree<DeviceChannel>> pageInfo = new PageInfo<>();
			pageInfo.setPageNum(page);
			pageInfo.setTotal(treeData == null? 0 : treeData.size());
			pageInfo.setSize(0);
			pageInfo.setList(new ArrayList<>());
			return new ResponseEntity<>(pageInfo,HttpStatus.OK);
		}

		int toIndex = Math.min(page * count, treeData.size());
		// 处理分页
		List<DeviceChannel> trees = treeData.subList((page - 1) * count, toIndex);
		PageInfo<DeviceChannel> pageInfo = new PageInfo<>();
		pageInfo.setPageNum(page);
		pageInfo.setTotal(treeData.size());
		pageInfo.setSize(trees.size());
		pageInfo.setList(trees);

		return new ResponseEntity<>(pageInfo,HttpStatus.OK);
	}
}
