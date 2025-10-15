package com.genersoft.iot.vmp.web.custom.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.service.IMapService;
import com.genersoft.iot.vmp.vmanager.bean.MapConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 第三方平台适配
 */
@Slf4j
@Service
public class SyServiceImpl implements IMapService {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public List<MapConfig> getConfig() {
        List<MapConfig> configList = new ArrayList<>();
        JSONObject configObject = (JSONObject)redisTemplate.opsForValue().get("interfaceConfig1");
        if (configObject == null) {
            return configList;
        }
        // 浅色地图
        MapConfig mapConfigForDefault = readConfig("FRAGMENTIMG_SERVER", configObject);
        if (mapConfigForDefault != null) {
            mapConfigForDefault.setName("浅色地图");
            configList.add(mapConfigForDefault);
        }

        // 深色地图
        MapConfig mapConfigForDark = readConfig("POLARNIGHTBLUE_FRAGMENTIMG_SERVER", configObject);
        if (mapConfigForDark != null) {
            mapConfigForDark.setName("深色地图");
            configList.add(mapConfigForDark);
        }

        // 卫星地图
        MapConfig mapConfigForSatellited = readConfig("SATELLITE_FRAGMENTIMG_SERVER", configObject);
        if (mapConfigForSatellited != null) {
            mapConfigForSatellited.setName("卫星地图");
            configList.add(mapConfigForSatellited);
        }
        return configList;
    }

    private MapConfig readConfig(String key, JSONObject jsonObject) {
        JSONArray fragmentimgServerArray = jsonObject.getJSONArray(key);
        if (fragmentimgServerArray == null || fragmentimgServerArray.isEmpty()) {
            return null;
        }
        JSONObject fragmentimgServer = fragmentimgServerArray.getJSONObject(0);
        // 坐标系
        String geoCoordSys = fragmentimgServer.getString("csysType").toUpperCase();
        // 获取地址
        String path = fragmentimgServer.getString("path");
        String ip = fragmentimgServer.getString("ip");
        JSONObject portJson = fragmentimgServer.getJSONObject("port");
        JSONObject httpPortJson = portJson.getJSONObject("httpPort");
        String protocol = httpPortJson.getString("portType");
        Integer port = httpPortJson.getInteger("port");
        String tileUrl = String.format("%s://%s:%s%s", protocol, ip, port, path);
        MapConfig mapConfig = new MapConfig();
        mapConfig.setCoordinateSystem(geoCoordSys);
        mapConfig.setTilesUrl(tileUrl);
        return mapConfig;

    }
}
