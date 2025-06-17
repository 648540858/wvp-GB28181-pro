package com.genersoft.iot.vmp.gat1400.fontend;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.APEDevice;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;


@Component
public class DictContextHolder implements InitializingBean {
    private static final Map<String, JSONObject> DICT_MAP = new HashMap<>(8);


    @Override
    public void afterPropertiesSet() throws Exception {
        ClassPathResource resource = new ClassPathResource("gb_dict.json");
        InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        String data = FileCopyUtils.copyToString(reader);
        JSONArray dicts = JSONArray.parseArray(data);
        for (Object dict : dicts) {
            if (dict instanceof JSONObject) {
                JSONObject ele = (JSONObject) dict;
                DICT_MAP.put(ele.getString("id"), ele.getJSONObject("data"));
            } else if (dict instanceof Map) {
                JSONObject ele = new JSONObject((Map) dict);
                DICT_MAP.put(ele.getString("id"), ele.getJSONObject("data"));
            }
        }
    }

    public static String getDictValue(String dict, String key) {
        if (StringUtils.isBlank(key))
            return null;
        JSONObject json = DICT_MAP.get(dict);
        if (json == null)
            return key;
        return json.getString(key);
    }

    public static void setDictValue(String dict, Supplier<String> key, Consumer<String> consumer) {
        String dictValue = getDictValue(dict, key.get());
        consumer.accept(dictValue);
    }

    public static String analysisDirection(APEDevice device) {
        String monitorDirection = device.getMonitorDirection();
        String capDirection = device.getCapDirection();
        if (StringUtils.isBlank(monitorDirection)) {
            return null;
        }
        if ("0".equals(capDirection)) {
            //拍车头则行驶方向就是监视方向的反方向
            switch (monitorDirection) {
                case "1":
                    return "2";
                case "2":
                    return "1";
                case "3":
                    return "4";
                case "4":
                    return "3";
                case "5":
                    return "6";
                case "6":
                    return "5";
                case "7":
                    return "8";
                case "8":
                    return "7";
                default:
                    return "9";
            }
        } else if ("1".equals(capDirection)) {
            //拍车尾则监视方向就是行驶方向
            return monitorDirection;
        } else {
            //如果拍摄方向为空则直接返回监视方向
            return monitorDirection;
        }
    }
}
