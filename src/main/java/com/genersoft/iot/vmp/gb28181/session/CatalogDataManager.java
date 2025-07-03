package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.gb28181.service.IRegionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CatalogDataManager implements CommandLineRunner {

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private IRegionService regionService;

    @Autowired
    private IGroupService groupService;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    private final Map<String, CatalogData> dataMap = new ConcurrentHashMap<>();

    private final String key = "VMP_CATALOG_DATA";

    public String buildMapKey(String deviceId, int sn ) {
        return deviceId + "_" + sn;
    }

    public void addReady(Device device, int sn ) {
        CatalogData catalogData = dataMap.get(buildMapKey(device.getDeviceId(),sn));
        if (catalogData != null) {
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
            dataMap.remove(buildMapKey(device.getDeviceId(),sn));
        }
        catalogData = new CatalogData();
        catalogData.setDevice(device);
        catalogData.setSn(sn);
        catalogData.setStatus(CatalogData.CatalogDataStatus.ready);
        catalogData.setTime(Instant.now());
        dataMap.put(buildMapKey(device.getDeviceId(),sn), catalogData);
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
                if (catalogData.getErrorMsg() != null) {
                    // 失败的同步信息,返回一次后直接移除
                    dataMap.remove(key);
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

    @Override
    public void run(String... args) throws Exception {
        // 启动时清理旧的数据
        redisTemplate.delete(key);
    }

    @Scheduled(fixedDelay = 5 * 1000)   //每5秒执行一次, 发现数据5秒未更新则移除数据并认为数据接收超时
    private void timerTask(){
        if (dataMap.isEmpty()) {
            return;
        }
        Set<String> keys = dataMap.keySet();

        Instant instantBefore5S = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(5));
        Instant instantBefore30S = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(30));
        for (String dataKey : keys) {
            CatalogData catalogData = dataMap.get(dataKey);
            if ( catalogData.getTime().isBefore(instantBefore5S)) {
                if (catalogData.getStatus().equals(CatalogData.CatalogDataStatus.runIng)) {
                    String deviceId = catalogData.getDevice().getDeviceId();
                    int sn = catalogData.getSn();
                    List<DeviceChannel> deviceChannelList = getDeviceChannelList(deviceId, sn);
                    try {
                        if (catalogData.getTotal() == deviceChannelList.size()) {
                            deviceChannelService.resetChannels(catalogData.getDevice().getId(), deviceChannelList);
                        }else {
                            deviceChannelService.updateChannels(catalogData.getDevice(), deviceChannelList);
                        }
                        List<Region> regionList = getRegionList(deviceId, sn);
                        if ( regionList!= null && !regionList.isEmpty()) {
                            regionService.batchAdd(regionList);
                        }
                        List<Group> groupList = getGroupList(deviceId, sn);
                        if (groupList != null && !groupList.isEmpty()) {
                            groupService.batchAdd(groupList);
                        }
                    }catch (Exception e) {
                        log.error("[国标通道同步] 入库失败： ", e);
                    }
                    String errorMsg = "更新成功，共" + catalogData.getTotal() + "条，已更新" + deviceChannelList.size() + "条";
                    catalogData.setErrorMsg(errorMsg);
                    catalogData.setStatus(CatalogData.CatalogDataStatus.end);
                }else if (catalogData.getStatus().equals(CatalogData.CatalogDataStatus.ready)) {
                    String errorMsg = "同步失败，等待回复超时";
                    catalogData.setErrorMsg(errorMsg);
                }
            }
            if ((catalogData.getStatus().equals(CatalogData.CatalogDataStatus.end) || catalogData.getStatus().equals(CatalogData.CatalogDataStatus.ready))
                    && catalogData.getTime().isBefore(instantBefore30S)) { // 超过三十秒，如果标记为end则删除
                dataMap.remove(dataKey);
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
        }
    }


    public void setChannelSyncEnd(String deviceId, int sn, String errorMsg) {
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null) {
            return;
        }
        catalogData.setStatus(CatalogData.CatalogDataStatus.end);
        catalogData.setErrorMsg(errorMsg);
        catalogData.setTime(Instant.now());
    }

    public int size(String deviceId, int sn) {
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null) {
            return 0;
        }
        return catalogData.getRedisKeysForChannel().size() + catalogData.getErrorChannel().size();
    }

    public int sumNum(String deviceId, int sn) {
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null) {
            return 0;
        }
        return catalogData.getTotal();
    }
}
