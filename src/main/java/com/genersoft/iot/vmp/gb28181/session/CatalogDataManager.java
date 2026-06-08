package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.gb28181.service.IRegionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class CatalogDataManager{

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private IRegionService regionService;

    @Autowired
    private IGroupService groupService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final Map<String, CatalogData> dataMap = new ConcurrentHashMap<>();

    private final Set<String> syncingDevices = ConcurrentHashMap.newKeySet();

    private final Map<String, ReentrantLock> deviceWriteLocks = new ConcurrentHashMap<>();

    public ReentrantLock getDeviceWriteLock(String deviceId) {
        return deviceWriteLocks.computeIfAbsent(deviceId, k -> new ReentrantLock());
    }

    public void removeDeviceWriteLock(String deviceId) {
        deviceWriteLocks.remove(deviceId);
    }

    private final String key = "VMP_CATALOG_DATA";

    public String buildMapKey(String deviceId, int sn ) {
        return deviceId + "_" + sn;
    }

    public void addReady(Device device, int sn ) {
        // 清除该设备的所有旧条目
        Iterator<Map.Entry<String, CatalogData>> it = dataMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CatalogData> entry = it.next();
            CatalogData old = entry.getValue();
            if (old != null && device.getDeviceId().equals(old.getDevice().getDeviceId())) {
                deleteRedisKeys(old);
                it.remove();
            }
        }
        CatalogData catalogData = new CatalogData();
        catalogData.setDevice(device);
        catalogData.setSn(sn);
        catalogData.setStatus(CatalogData.CatalogDataStatus.ready);
        catalogData.setTime(Instant.now());
        dataMap.put(buildMapKey(device.getDeviceId(),sn), catalogData);
        syncingDevices.add(device.getDeviceId());
    }

    private void deleteRedisKeys(CatalogData catalogData) {
        Set<String> redisKeysForChannel = catalogData.getRedisKeysForChannel();
        if (redisKeysForChannel != null && !redisKeysForChannel.isEmpty()) {
            for (String deleteKey : redisKeysForChannel) {
                redisTemplate.opsForHash().delete(key, deleteKey);
            }
        }
        Set<String> redisKeysForRegion = catalogData.getRedisKeysForRegion();
        if (redisKeysForRegion != null && !redisKeysForRegion.isEmpty()) {
            for (String deleteKey : redisKeysForRegion) {
                redisTemplate.opsForHash().delete(key, deleteKey);
            }
        }
        Set<String> redisKeysForGroup = catalogData.getRedisKeysForGroup();
        if (redisKeysForGroup != null && !redisKeysForGroup.isEmpty()) {
            for (String deleteKey : redisKeysForGroup) {
                redisTemplate.opsForHash().delete(key, deleteKey);
            }
        }
    }

    public void put(String deviceId, int sn, int total, Device device, List<DeviceChannel> deviceChannelList,
                    List<Region> regionList, List<Group> groupList) {
        CatalogData catalogData = dataMap.get(buildMapKey(device.getDeviceId(),sn));
        if (catalogData == null ) {
            log.warn("[缓存-Catalog] 未找到缓存对象，可能已经结束");
            return;
        }
        catalogData.setStatus(CatalogData.CatalogDataStatus.runIng);
        catalogData.setTotal(total);
        catalogData.setTime(Instant.now());

        if (deviceChannelList != null && !deviceChannelList.isEmpty()) {
            for (DeviceChannel deviceChannel : deviceChannelList) {
                String keyForChannel = "CHANNEL:" + deviceId + ":" + deviceChannel.getDeviceId() + ":" + sn;
                redisTemplate.opsForHash().put(key, keyForChannel, deviceChannel);
                catalogData.getRedisKeysForChannel().add(keyForChannel);
            }
        }

        if (regionList != null && !regionList.isEmpty()) {
            for (Region region : regionList) {
                String keyForRegion = "REGION:" + deviceId + ":" + region.getDeviceId() + ":" + sn;
                redisTemplate.opsForHash().put(key, keyForRegion, region);
                catalogData.getRedisKeysForRegion().add(keyForRegion);
            }
        }

        if (groupList != null && !groupList.isEmpty()) {
            for (Group group : groupList) {
                String keyForGroup = "GROUP:" + deviceId + ":" + group.getDeviceId() + ":" + sn;
                redisTemplate.opsForHash().put(key, keyForGroup, group);
                catalogData.getRedisKeysForGroup().add(keyForGroup);
            }
        }
    }

    public List<DeviceChannel> getDeviceChannelList(String deviceId, int sn) {
        List<DeviceChannel> result = new ArrayList<>();
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null ) {
            log.warn("[Redis-Catalog] 未找到缓存对象，可能已经结束");
            return result;
        }
        for (String objectKey : catalogData.getRedisKeysForChannel()) {
            DeviceChannel deviceChannel = (DeviceChannel) redisTemplate.opsForHash().get(key, objectKey);
            if (deviceChannel != null) {
                result.add(deviceChannel);
            }
        }
        return result;
    }

    public List<Region> getRegionList(String deviceId, int sn) {
        List<Region> result = new ArrayList<>();
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null ) {
            log.warn("[Redis-Catalog] 未找到缓存对象，可能已经结束");
            return result;
        }
        for (String objectKey : catalogData.getRedisKeysForRegion()) {
            Region region = (Region) redisTemplate.opsForHash().get(key, objectKey);
            if (region != null) {
                result.add(region);
            }
        }
        return result;
    }

    public List<Group> getGroupList(String deviceId, int sn) {
        List<Group> result = new ArrayList<>();
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null ) {
            log.warn("[Redis-Catalog] 未找到缓存对象，可能已经结束");
            return result;
        }
        for (String objectKey : catalogData.getRedisKeysForGroup()) {
            Group group = (Group) redisTemplate.opsForHash().get(key, objectKey);
            if (group != null) {
                result.add(group);
            }
        }
        return result;
    }

    public SyncStatus getSyncStatus(String deviceId) {
        if (dataMap.isEmpty()) {
            return null;
        }
        Set<String> keySet = dataMap.keySet();
        for (String key : keySet) {
            CatalogData catalogData = dataMap.get(key);
            if (catalogData != null && deviceId.equals(catalogData.getDevice().getDeviceId())) {
                SyncStatus syncStatus = new SyncStatus();
                syncStatus.setCurrent(catalogData.getRedisKeysForChannel().size());
                syncStatus.setTotal(catalogData.getTotal());
                syncStatus.setErrorMsg(catalogData.getErrorMsg());
                syncStatus.setTime(catalogData.getTime());
                if (catalogData.getStatus().equals(CatalogData.CatalogDataStatus.ready) || catalogData.getStatus().equals(CatalogData.CatalogDataStatus.end)) {
                    syncStatus.setSyncIng(false);
                }else {
                    syncStatus.setSyncIng(true);
                }
                return syncStatus;
            }
        }
        return null;
    }

    public boolean isSyncRunning(String deviceId) {
        if (dataMap.isEmpty()) {
            return false;
        }
        Set<String> keySet = dataMap.keySet();
        for (String key : keySet) {
            CatalogData catalogData = dataMap.get(key);
            if (catalogData != null && deviceId.equals(catalogData.getDevice().getDeviceId())) {
                // 此时检查是否过期
                Instant instantBefore30S = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(30));
                if ((catalogData.getStatus().equals(CatalogData.CatalogDataStatus.end)
                        || catalogData.getStatus().equals(CatalogData.CatalogDataStatus.ready))
                        && catalogData.getTime().isBefore(instantBefore30S)) {
                    dataMap.remove(key);
                    return false;
                }

                return !catalogData.getStatus().equals(CatalogData.CatalogDataStatus.end);
            }
        }
        return false;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(){
        // 启动时清理旧的数据
        redisTemplate.delete(key);
    }

    //每5秒执行一次, 发现数据5秒未更新则移除数据并认为数据接收超时
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    private void timerTask(){
        if (dataMap.isEmpty()) {
            return;
        }
        Set<String> keys = dataMap.keySet();
        // 消息间等待间隔最大五秒
        Instant instantBefore5S = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(5));
        // 消息接收完毕，等待30秒后移除数据
        Instant instantBefore30S = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(30));
        // 初次等待的时间长度，兼容部分下级平台发送初次数据很慢的情况
        Instant instantBefore2M = Instant.now().minusMillis(TimeUnit.MINUTES.toMillis(2));
        for (String dataKey : keys) {
            CatalogData catalogData = dataMap.get(dataKey);
            if (catalogData.getStatus().equals(CatalogData.CatalogDataStatus.ready)) {
                if ( catalogData.getTime().isBefore(instantBefore2M)) {
                    String errorMsg = "同步失败，等待回复超时";
                    catalogData.setErrorMsg(errorMsg);
                    catalogData.setStatus(CatalogData.CatalogDataStatus.end);
                    syncingDevices.remove(catalogData.getDevice().getDeviceId());
                }
            }else if (catalogData.getStatus().equals(CatalogData.CatalogDataStatus.runIng)) {
                boolean complete = catalogData.isComplete();
                boolean timeout = catalogData.getTime().isBefore(instantBefore5S);
                if (complete || timeout) {
                    String deviceId = catalogData.getDevice().getDeviceId();
                    ReentrantLock lock = getDeviceWriteLock(deviceId);
                    if (!lock.tryLock()) {
                        continue;
                    }
                    try {
                        int sn = catalogData.getSn();
                        List<DeviceChannel> deviceChannelList = getDeviceChannelList(deviceId, sn);
                        if (!deviceChannelList.isEmpty()) {
                            deviceChannelService.resetChannels(catalogData.getDevice().getId(), deviceChannelList);
                        }
                        List<Region> regionList = getRegionList(deviceId, sn);
                        if ( regionList!= null && !regionList.isEmpty()) {
                            regionService.batchAdd(regionList);
                        }
                        List<Group> groupList = getGroupList(deviceId, sn);
                        if (groupList != null && !groupList.isEmpty()) {
                            groupService.batchAdd(groupList);
                        }
                        catalogData.setErrorMsg(null);
                    } catch (Exception e) {
                        log.error("[国标通道同步] 入库失败： ", e);
                        catalogData.setErrorMsg("入库失败: " + e.getMessage());
                    } finally {
                        lock.unlock();
                    }
                    catalogData.setStatus(CatalogData.CatalogDataStatus.end);
                    syncingDevices.remove(deviceId);
                }
            }else {
                if (catalogData.getTime().isBefore(instantBefore30S)) {
                    String deviceId = catalogData.getDevice().getDeviceId();
                    dataMap.remove(dataKey);
                    // 清理可能残留的设备锁
                    if (deviceWriteLocks.containsKey(deviceId)) {
                        deviceWriteLocks.remove(deviceId);
                    }
                    syncingDevices.remove(deviceId);
                    deleteRedisKeys(catalogData);
                }
            }
        }
    }


    public void setComplete(String deviceId, int sn) {
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null) {
            return;
        }
        catalogData.setComplete(true);
    }

    public void setChannelSyncEnd(String deviceId, int sn, String errorMsg) {
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null) {
            return;
        }
        catalogData.setStatus(CatalogData.CatalogDataStatus.end);
        catalogData.setErrorMsg(errorMsg);
        catalogData.setTime(Instant.now());
        syncingDevices.remove(deviceId);
    }

    public boolean isSyncing(String deviceId) {
        return syncingDevices.contains(deviceId);
    }

    public boolean isEnd(String deviceId, int sn) {
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId, sn));
        return catalogData != null && catalogData.getStatus() == CatalogData.CatalogDataStatus.end;
    }

    public int size(String deviceId, int sn) {
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null) {
            return 0;
        }
        return catalogData.getRedisKeysForChannel().size();
    }

    public int sumNum(String deviceId, int sn) {
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null) {
            return 0;
        }
        return catalogData.getTotal();
    }
}
