package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class CatalogDataCatch {

    public static Map<String, CatalogData> data = new ConcurrentHashMap<>();

    @Autowired
    private IDeviceChannelService deviceChannelService;

    public void addReady(Device device, int sn ) {
        CatalogData catalogData = data.get(device.getDeviceId());
        if (catalogData == null || catalogData.getStatus().equals(CatalogData.CatalogDataStatus.end)) {
            catalogData = new CatalogData();
            catalogData.setChannelList(Collections.synchronizedList(new ArrayList<>()));
            catalogData.setDevice(device);
            catalogData.setSn(sn);
            catalogData.setStatus(CatalogData.CatalogDataStatus.ready);
            catalogData.setLastTime(Instant.now());
            data.put(device.getDeviceId(), catalogData);
        }
    }

    public void put(String deviceId, int sn, int total, Device device, List<DeviceChannel> deviceChannelList,
                    List<Region> regionList, List<Group> groupList) {
        CatalogData catalogData = data.get(deviceId);
        if (catalogData == null) {
            catalogData = new CatalogData();
            catalogData.setSn(sn);
            catalogData.setTotal(total);
            catalogData.setDevice(device);
            catalogData.setChannelList(deviceChannelList);
            catalogData.setRegionListList(regionList);
            catalogData.setGroupListListList(groupList);
            catalogData.setStatus(CatalogData.CatalogDataStatus.runIng);
            catalogData.setLastTime(Instant.now());
            data.put(deviceId, catalogData);
        }else {
            // 同一个设备的通道同步请求只考虑一个，其他的直接忽略
            if (catalogData.getSn() != sn) {
                return;
            }
            catalogData.setTotal(total);
            catalogData.setDevice(device);
            catalogData.setStatus(CatalogData.CatalogDataStatus.runIng);

            if (deviceChannelList != null && !deviceChannelList.isEmpty()) {
                if (catalogData.getChannelList() != null) {
                    catalogData.getChannelList().addAll(deviceChannelList);
                }
            }
            if (regionList != null && !regionList.isEmpty()) {
                if (catalogData.getRegionListList() != null) {
                    catalogData.getRegionListList().addAll(regionList);
                }else {
                    catalogData.setRegionListList(regionList);
                }
            }
            if (groupList != null && !groupList.isEmpty()) {
                if (catalogData.getGroupListListList() != null) {
                    catalogData.getGroupListListList().addAll(groupList);
                }else {
                    catalogData.setGroupListListList(groupList);
                }
            }
            catalogData.setLastTime(Instant.now());
        }
    }

    public List<DeviceChannel> getDeviceChannelList(String deviceId) {
        CatalogData catalogData = data.get(deviceId);
        if (catalogData == null) {
            return null;
        }
        return catalogData.getChannelList();
    }

    public List<Region> getRegionList(String deviceId) {
        CatalogData catalogData = data.get(deviceId);
        if (catalogData == null) {
            return null;
        }
        return catalogData.getRegionListList();
    }

    public List<Group> getGroupList(String deviceId) {
        CatalogData catalogData = data.get(deviceId);
        if (catalogData == null) {
            return null;
        }
        return catalogData.getGroupListListList();
    }

    public int getTotal(String deviceId) {
        CatalogData catalogData = data.get(deviceId);
        if (catalogData == null) {
            return 0;
        }
        return catalogData.getTotal();
    }

    public SyncStatus getSyncStatus(String deviceId) {
        CatalogData catalogData = data.get(deviceId);
        if (catalogData == null) {
            return null;
        }
        SyncStatus syncStatus = new SyncStatus();
        syncStatus.setCurrent(catalogData.getChannelList().size());
        syncStatus.setTotal(catalogData.getTotal());
        syncStatus.setErrorMsg(catalogData.getErrorMsg());
        if (catalogData.getStatus().equals(CatalogData.CatalogDataStatus.end)) {
            syncStatus.setSyncIng(false);
        }else {
            syncStatus.setSyncIng(true);
        }
        return syncStatus;
    }

    public boolean isSyncRunning(String deviceId) {
        CatalogData catalogData = data.get(deviceId);
        if (catalogData == null) {
            return false;
        }
        return !catalogData.getStatus().equals(CatalogData.CatalogDataStatus.end);
    }

    @Scheduled(fixedDelay = 5 * 1000)   //每5秒执行一次, 发现数据5秒未更新则移除数据并认为数据接收超时
    private void timerTask(){
        Set<String> keys = data.keySet();

        Instant instantBefore5S = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(5));
        Instant instantBefore30S = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(30));

        for (String deviceId : keys) {
            CatalogData catalogData = data.get(deviceId);
            if ( catalogData.getLastTime().isBefore(instantBefore5S)) {
                // 超过五秒收不到消息任务超时， 只更新这一部分数据, 收到数据与声明的总数一致，则重置通道数据，数据不全则只对收到的数据做更新操作
                if (catalogData.getStatus().equals(CatalogData.CatalogDataStatus.runIng)) {
                    if (catalogData.getTotal() == catalogData.getChannelList().size()) {
                        deviceChannelService.resetChannels(catalogData.getDevice().getId(), catalogData.getChannelList());
                    }else {
                        deviceChannelService.updateChannels(catalogData.getDevice(), catalogData.getChannelList());
                    }
                    String errorMsg = "更新成功，共" + catalogData.getTotal() + "条，已更新" + catalogData.getChannelList().size() + "条";
                    catalogData.setErrorMsg(errorMsg);
                    if (catalogData.getTotal() != catalogData.getChannelList().size()) {

                    }
                }else if (catalogData.getStatus().equals(CatalogData.CatalogDataStatus.ready)) {
                    String errorMsg = "同步失败，等待回复超时";
                    catalogData.setErrorMsg(errorMsg);
                }
                catalogData.setStatus(CatalogData.CatalogDataStatus.end);
            }
            if (catalogData.getStatus().equals(CatalogData.CatalogDataStatus.end) && catalogData.getLastTime().isBefore(instantBefore30S)) { // 超过三十秒，如果标记为end则删除
                data.remove(deviceId);
            }
        }
    }


    public void setChannelSyncEnd(String deviceId, String errorMsg) {
        CatalogData catalogData = data.get(deviceId);
        if (catalogData == null) {
            return;
        }
        catalogData.setStatus(CatalogData.CatalogDataStatus.end);
        catalogData.setErrorMsg(errorMsg);
    }
}
