package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.gb28181.bean.CatalogData;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CatalogDataCatch {

    public static Map<String, CatalogData> data = new ConcurrentHashMap<>();

    @Autowired
    private DeferredResultHolder deferredResultHolder;

    @Autowired
    private IVideoManagerStorage storager;

    public void put(String key, int total, Device device, List<DeviceChannel> deviceChannelList) {
        CatalogData catalogData = data.get(key);
        if (catalogData == null) {
            catalogData = new CatalogData();
            catalogData.setTotal(total);
            catalogData.setDevice(device);
            catalogData.setChannelList(new ArrayList<>());
            data.put(key, catalogData);
        }
        catalogData.getChannelList().addAll(deviceChannelList);
        catalogData.setLastTime(new Date(System.currentTimeMillis()));
    }

    public List<DeviceChannel> get(String key) {
        CatalogData catalogData = data.get(key);
        if (catalogData == null) return null;
        return catalogData.getChannelList();
    }

    public void del(String key) {
        data.remove(key);
    }

    @Scheduled(fixedRate = 5 * 1000)   //每5秒执行一次, 发现数据5秒未更新则移除数据并认为数据接收超时
    private void timerTask(){
        Set<String> keys = data.keySet();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) - 5);
        for (String key : keys) {
            CatalogData catalogData = data.get(key);
            if (catalogData.getLastTime().before(calendar.getTime())) {

                storager.resetChannels(catalogData.getDevice().getDeviceId(), catalogData.getChannelList());
                RequestMessage msg = new RequestMessage();
                msg.setKey(key);
                WVPResult<Object> result = new WVPResult<>();
                result.setCode(0);
                result.setMsg("更新成功，共" + catalogData.getTotal() + "条，已更新" + catalogData.getChannelList().size() + "条");
                result.setData(catalogData.getDevice());
                msg.setData(result);
                deferredResultHolder.invokeAllResult(msg);
                data.remove(key);
            }
        }
    }
}
