package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 设备相关业务处理
 * @author lin
 */
public interface IDeviceService {

    /**
     * 设备上线
     * @param device 设备信息
     */
    void online(Device device, SipTransactionInfo sipTransactionInfo);

    /**
     * 设备下线
     * @param deviceId 设备编号
     */
    void offline(String deviceId, String reason);

    /**
     * 添加目录订阅
     * @param device 设备信息
     * @return 布尔
     */
    boolean addCatalogSubscribe(Device device);

    /**
     * 移除目录订阅
     * @param device 设备信息
     * @return 布尔
     */
    boolean removeCatalogSubscribe(Device device, CommonCallback<Boolean> callback);

    /**
     * 添加移动位置订阅
     * @param device 设备信息
     * @return 布尔
     */
    boolean addMobilePositionSubscribe(Device device);

    /**
     * 移除移动位置订阅
     * @param device 设备信息
     * @return 布尔
     */
    boolean removeMobilePositionSubscribe(Device device, CommonCallback<Boolean> callback);

    /**
     * 移除移动位置订阅
     * @param deviceId 设备ID
     * @return 同步状态
     */
    SyncStatus getChannelSyncStatus(String deviceId);

    /**
     * 查看是否仍在同步
     * @param deviceId 设备ID
     * @return 布尔
     */
    Boolean isSyncRunning(String deviceId);

    /**
     * 通道同步
     * @param device 设备信息
     */
    void sync(Device device);

    /**
     * 查询设备信息
     * @param deviceId 设备编号
     * @return 设备信息
     */
    Device getDeviceByDeviceId(String deviceId);

    /**
     * 获取所有在线设备
     * @return 设备列表
     */
    List<Device> getAllOnlineDevice(String serverId);

    List<Device> getAllByStatus(Boolean status);

    /**
     * 判断是否注册已经失效
     * @param device 设备信息
     * @return 布尔
     */
    boolean expire(Device device);

    /**
     * 检查设备状态
     * @param device 设备信息
     */
    Boolean getDeviceStatus(Device device);

    /**
     * 根据IP和端口获取设备信息
     * @param host IP
     * @param port 端口
     * @return 设备信息
     */
    Device getDeviceByHostAndPort(String host, int port);

    /**
     * 更新设备
     * @param device 设备信息
     */
    void updateDevice(Device device);

    /**
     * 检查设备编号是否已经存在
     * @param deviceId 设备编号
     * @return
     */
    boolean isExist(String deviceId);

    /**
     * 添加设备
     * @param device
     */
    void addDevice(Device device);

    /**
     * 页面表单更新设备信息
     * @param device
     */
    void updateCustomDevice(Device device);

    /**
     * 删除设备
     * @param deviceId
     * @return
     */
    boolean delete(String deviceId);

    /**
     * 获取统计信息
     * @return
     */
    ResourceBaseInfo getOverview();

    /**
     * 获取所有设备
     */
    List<Device> getAll();

    PageInfo<Device> getAll(int page, int count, String query, Boolean status);

    Device getDevice(Integer gbDeviceDbId);

    Device getDeviceByChannelId(Integer channelId);

    Device getDeviceBySourceChannelDeviceId(String requesterId);

    void subscribeCatalog(int id, int cycle);

    void subscribeMobilePosition(int id, int cycle, int interval);

    WVPResult<SyncStatus> devicesSync(Device device);

    void deviceBasicConfig(Device device, BasicParam basicParam, ErrorCallback<String> callback);

    void deviceConfigQuery(Device device, String channelId, String configType, ErrorCallback<Object> callback);

    void teleboot(Device device);

    void record(Device device, String channelId, String recordCmdStr, ErrorCallback<String> callback);

    void guard(Device device, String guardCmdStr, ErrorCallback<String> callback);

    void resetAlarm(Device device, String channelId, String alarmMethod, String alarmType, ErrorCallback<String> callback);

    void iFrame(Device device, String channelId);

    void homePosition(Device device, String channelId, Boolean enabled, Integer resetTime, Integer presetIndex, ErrorCallback<String> callback);

    void dragZoomIn(Device device, String channelId, int length, int width, int midpointx, int midpointy, int lengthx, int lengthy, ErrorCallback<String> callback);

    void dragZoomOut(Device device, String channelId, int length, int width, int midpointx, int midpointy, int lengthx, int lengthy, ErrorCallback<String> callback);

    void deviceStatus(Device device, ErrorCallback<String> callback);

    void updateDeviceHeartInfo(Device device);

    void alarm(Device device, String startPriority, String endPriority, String alarmMethod, String alarmType, String startTime, String endTime, ErrorCallback<Object> callback);

    void deviceInfo(Device device, ErrorCallback<Object> callback);

    void queryPreset(Device device, String channelId, ErrorCallback<Object> callback);
}
