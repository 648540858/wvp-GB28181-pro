package com.genersoft.iot.vmp.utils;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.gb28181.bean.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public enum CivilCodeUtil {

    INSTANCE;
    // 用与消息的缓存
    private final Map<String, CivilCodePo> civilCodeMap = new ConcurrentHashMap<>();

    CivilCodeUtil() {
    }

    public void add(List<CivilCodePo> civilCodePoList) {
        if (!civilCodePoList.isEmpty()) {
            for (CivilCodePo civilCodePo : civilCodePoList) {
                civilCodeMap.put(civilCodePo.getCode(), civilCodePo);
            }
        }
    }

    public void add(CivilCodePo civilCodePo) {
        civilCodeMap.put(civilCodePo.getCode(), civilCodePo);
    }

    public CivilCodePo get(String code) {
        return civilCodeMap.get(code);
    }

    public CivilCodePo getParentCode(String code) {
        if (code.length() > 8) {
            return null;
        }
        if (code.length() == 8) {
            String parentCode = code.substring(0, 6);
            return civilCodeMap.get(parentCode);
        }else {
            CivilCodePo civilCodePo = civilCodeMap.get(code);
            if (civilCodePo == null){
                return null;
            }
            String parentCode = civilCodePo.getParentCode();
            if (parentCode == null) {
                return null;
            }
            return civilCodeMap.get(parentCode);
        }
    }

    public CivilCodePo getCivilCodePo(String code) {
        if (code.length() > 8) {
            return null;
        }else {
            return civilCodeMap.get(code);
        }
    }

    public List<CivilCodePo> getAllParentCode(String civilCode) {
        List<CivilCodePo> civilCodePoList = new ArrayList<>();
        CivilCodePo parentCode = getParentCode(civilCode);
        if (parentCode != null) {
            civilCodePoList.add(parentCode);
            List<CivilCodePo> allParentCode = getAllParentCode(parentCode.getCode());
            if (!allParentCode.isEmpty()) {
                civilCodePoList.addAll(allParentCode);
            }else {
                return civilCodePoList;
            }
        }
        return civilCodePoList;
    }

    public boolean isEmpty() {
        return civilCodeMap.isEmpty();
    }

    public int size() {
        return civilCodeMap.size();
    }

    public List<Region> getAllChild(String parent) {
        List<Region> result = new ArrayList<>();
        for (String key : civilCodeMap.keySet()) {
            if (parent == null) {
                if (ObjectUtils.isEmpty(civilCodeMap.get(key).getParentCode().trim())) {
                    result.add(Region.getInstance(key, civilCodeMap.get(key).getName(), civilCodeMap.get(key).getParentCode()));
                }
            }else if (civilCodeMap.get(key).getParentCode().equals(parent)) {
                result.add(Region.getInstance(key, civilCodeMap.get(key).getName(), civilCodeMap.get(key).getParentCode()));
            }
        }
        return result;
    }
}
