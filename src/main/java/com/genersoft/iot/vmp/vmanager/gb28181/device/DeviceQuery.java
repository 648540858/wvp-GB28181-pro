package com.genersoft.iot.vmp.vmanager.gb28181.device;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.CatalogSubscribeTask;
import com.genersoft.iot.vmp.gb28181.task.impl.MobilePositionSubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.BaseTree;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.ibatis.annotations.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.*;

@Tag(name  = "国标设备查询", description = "国标设备查询")
@SuppressWarnings("rawtypes")

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
	private IInviteStreamService inviteStreamService;
	
	@Autowired
	private SIPCommander cmder;
	
	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private DynamicTask dynamicTask;

	/**
	 * 使用ID查询国标设备
	 * @param deviceId 国标ID
	 * @return 国标设备
	 */
	@Operation(summary = "查询国标设备")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@GetMapping("/devices/{deviceId}")
	public Device devices(@PathVariable String deviceId){
		
		return storager.queryVideoDevice(deviceId);
	}

	/**
	 * 分页查询国标设备
	 * @param page 当前页
	 * @param count 每页查询数量
	 * @return 分页国标列表
	 */
	@Operation(summary = "分页查询国标设备")
	@Parameter(name = "page", description = "当前页", required = true)
	@Parameter(name = "count", description = "每页查询数量", required = true)
	@GetMapping("/devices")
	@Options()
	public PageInfo<Device> devices(int page, int count){
//		if (page == null) page = 0;
//		if (count == null) count = 20;
		return storager.queryVideoDeviceList(page, count,null);
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
	 * @param catalogUnderDevice 是否直属与设备的目录
	 * @return 通道列表
	 */
	@GetMapping("/devices/{deviceId}/channels")
	@Operation(summary = "分页查询通道")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "page", description = "当前页", required = true)
	@Parameter(name = "count", description = "每页查询数量", required = true)
	@Parameter(name = "query", description = "查询内容")
	@Parameter(name = "online", description = "是否在线")
	@Parameter(name = "channelType", description = "设备/子目录-> false/true")
	@Parameter(name = "catalogUnderDevice", description = "是否直属与设备的目录")
	public PageInfo<DeviceChannel> channels(@PathVariable String deviceId,
											   int page, int count,
											   @RequestParam(required = false) String query,
											   @RequestParam(required = false) Boolean online,
											   @RequestParam(required = false) Boolean channelType,
											   @RequestParam(required = false) Boolean catalogUnderDevice) {
		if (ObjectUtils.isEmpty(query)) {
			query = null;
		}

		return storager.queryChannelsByDeviceId(deviceId, query, channelType, online, catalogUnderDevice, page, count);
	}

	/**
	 * 同步设备通道
	 * @param deviceId 设备id
	 * @return
	 */
	@Operation(summary = "同步设备通道")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@GetMapping("/devices/{deviceId}/sync")
	public WVPResult<SyncStatus> devicesSync(@PathVariable String deviceId){
		
		if (logger.isDebugEnabled()) {
			logger.debug("设备通道信息同步API调用，deviceId：" + deviceId);
		}
		Device device = storager.queryVideoDevice(deviceId);
		boolean status = deviceService.isSyncRunning(deviceId);
		// 已存在则返回进度
		if (status) {
			SyncStatus channelSyncStatus = deviceService.getChannelSyncStatus(deviceId);
			return WVPResult.success(channelSyncStatus);
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
	@Operation(summary = "移除设备")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@DeleteMapping("/devices/{deviceId}/delete")
	public String delete(@PathVariable String deviceId){
		
		if (logger.isDebugEnabled()) {
			logger.debug("设备信息删除API调用，deviceId：" + deviceId);
		}

		// 清除redis记录
		boolean isSuccess = deviceService.delete(deviceId);
		if (isSuccess) {
			inviteStreamService.clearInviteInfo(deviceId);
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
			return json.toString();
		} else {
			logger.warn("设备信息删除API调用失败！");
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备信息删除API调用失败！");
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
	@Operation(summary = "分页查询子目录通道")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "page", description = "当前页", required = true)
	@Parameter(name = "count", description = "每页查询数量", required = true)
	@Parameter(name = "query", description = "查询内容")
	@Parameter(name = "online", description = "是否在线")
	@Parameter(name = "channelType", description = "设备/子目录-> false/true")
	@GetMapping("/sub_channels/{deviceId}/{channelId}/channels")
	public PageInfo<DeviceChannel> subChannels(@PathVariable String deviceId,
												  @PathVariable String channelId,
												  int page,
												  int count,
												  @RequestParam(required = false) String query,
												  @RequestParam(required = false) Boolean online,
												  @RequestParam(required = false) Boolean channelType){

		DeviceChannel deviceChannel = storager.queryChannel(deviceId,channelId);
		if (deviceChannel == null) {
			PageInfo<DeviceChannel> deviceChannelPageResult = new PageInfo<>();
			return deviceChannelPageResult;
		}

		return storager.querySubChannels(deviceId, channelId, query, channelType, online, page, count);
	}

	/**
	 * 更新通道信息
	 * @param deviceId 设备id
	 * @param channel 通道
	 * @return
	 */
	@Operation(summary = "更新通道信息")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channel", description = "通道信息", required = true)
	@PostMapping("/channel/update/{deviceId}")
	public void updateChannel(@PathVariable String deviceId,DeviceChannel channel){
		deviceChannelService.updateChannel(deviceId, channel);
	}

	/**
	 * 修改数据流传输模式
	 * @param deviceId 设备id
	 * @param streamMode 数据流传输模式
	 * @return
	 */
	@Operation(summary = "修改数据流传输模式")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "streamMode", description = "数据流传输模式, 取值：" +
			"UDP（udp传输），TCP-ACTIVE（tcp主动模式,暂不支持），TCP-PASSIVE（tcp被动模式）", required = true)
	@PostMapping("/transport/{deviceId}/{streamMode}")
	public void updateTransport(@PathVariable String deviceId, @PathVariable String streamMode){
		Device device = deviceService.getDevice(deviceId);
		device.setStreamMode(streamMode);
		deviceService.updateCustomDevice(device);
	}

	/**
	 * 添加设备信息
	 * @param device 设备信息
	 * @return
	 */
	@Operation(summary = "添加设备信息")
	@Parameter(name = "device", description = "设备", required = true)
	@PostMapping("/device/add/")
	public void addDevice(Device device){

		if (device == null || device.getDeviceId() == null) {
			throw new ControllerException(ErrorCode.ERROR400);
		}

		// 查看deviceId是否存在
		boolean exist = deviceService.isExist(device.getDeviceId());
		if (exist) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备编号已存在");
		}
		deviceService.addDevice(device);
	}

	/**
	 * 更新设备信息
	 * @param device 设备信息
	 * @return
	 */
	@Operation(summary = "更新设备信息")
	@Parameter(name = "device", description = "设备", required = true)
	@PostMapping("/device/update/")
	public void updateDevice(Device device){

		if (device != null && device.getDeviceId() != null) {
			deviceService.updateCustomDevice(device);
		}
	}

	/**
	 * 设备状态查询请求API接口
	 * 
	 * @param deviceId 设备id
	 */
	@Operation(summary = "设备状态查询")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
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
		try {
			cmder.deviceStatusQuery(device, event -> {
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				msg.setData(String.format("获取设备状态失败，错误码： %s, %s", event.statusCode, event.msg));
				resultHolder.invokeResult(msg);
			});
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 获取设备状态: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
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
	@Operation(summary = "设备状态查询")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "startPriority", description = "报警起始级别")
	@Parameter(name = "endPriority", description = "报警终止级别")
	@Parameter(name = "alarmMethod", description = "报警方式条件")
	@Parameter(name = "alarmType", description = "报警类型")
	@Parameter(name = "startTime", description = "报警发生起始时间")
	@Parameter(name = "endTime", description = "报警发生终止时间")
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
		try {
			cmder.alarmInfoQuery(device, startPriority, endPriority, alarmMethod, alarmType, startTime, endTime, event -> {
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				msg.setData(String.format("设备报警查询失败，错误码： %s, %s",event.statusCode, event.msg));
				resultHolder.invokeResult(msg);
			});
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 设备报警查询: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
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
	@Operation(summary = "获取通道同步进度")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	public WVPResult<SyncStatus> getSyncStatus(@PathVariable String deviceId) {
		SyncStatus channelSyncStatus = deviceService.getChannelSyncStatus(deviceId);
		WVPResult<SyncStatus> wvpResult = new WVPResult<>();
		if (channelSyncStatus == null) {
			wvpResult.setCode(-1);
			wvpResult.setMsg("同步尚未开始");
		}else {
			wvpResult.setCode(ErrorCode.SUCCESS.getCode());
			wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());
			wvpResult.setData(channelSyncStatus);
			if (channelSyncStatus.getErrorMsg() != null) {
				wvpResult.setMsg(channelSyncStatus.getErrorMsg());
			}
		}
		return wvpResult;
	}

	@GetMapping("/{deviceId}/subscribe_info")
	@Operation(summary = "获取设备的订阅状态")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	public WVPResult<Map<String, Integer>> getSubscribeInfo(@PathVariable String deviceId) {
		Set<String> allKeys = dynamicTask.getAllKeys();
		Map<String, Integer> dialogStateMap = new HashMap<>();
		for (String key : allKeys) {
			if (key.startsWith(deviceId)) {
				ISubscribeTask subscribeTask = (ISubscribeTask)dynamicTask.get(key);
				if (subscribeTask instanceof CatalogSubscribeTask) {
					dialogStateMap.put("catalog", 1);
				}else if (subscribeTask instanceof MobilePositionSubscribeTask) {
					dialogStateMap.put("mobilePosition", 1);
				}
			}
		}
		WVPResult<Map<String, Integer>> wvpResult = new WVPResult<>();
		wvpResult.setCode(0);
		wvpResult.setData(dialogStateMap);
		return wvpResult;
	}

	@GetMapping("/snap/{deviceId}/{channelId}")
	@Operation(summary = "请求截图")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "mark", description = "标识", required = false)
	public void getSnap(HttpServletResponse resp, @PathVariable String deviceId, @PathVariable String channelId, @RequestParam(required = false) String mark) {

		try {
			final InputStream in = Files.newInputStream(new File("snap" + File.separator + deviceId + "_" + channelId + (mark == null? ".jpg": ("_" + mark + ".jpg"))).toPath());
			resp.setContentType(MediaType.IMAGE_PNG_VALUE);
			ServletOutputStream outputStream = resp.getOutputStream();
			IOUtils.copy(in, resp.getOutputStream());
			in.close();
			outputStream.close();
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
	@Operation(summary = "查询国标树")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "parentId", description = "父级国标编号")
	@Parameter(name = "onlyCatalog", description = "只获取目录")
	@Parameter(name = "page", description = "当前页", required = true)
	@Parameter(name = "count", description = "每页条数", required = true)
	@GetMapping("/tree/{deviceId}")
	public ResponseEntity<PageInfo> getTree(@PathVariable String deviceId,
											@RequestParam(required = false) String parentId,
											@RequestParam(required = false) Boolean onlyCatalog,
											int page, int count){


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
	@Operation(summary = "查询国标树下的通道")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "parentId", description = "父级国标编号")
	@Parameter(name = "page", description = "当前页", required = true)
	@Parameter(name = "count", description = "每页条数", required = true)
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
