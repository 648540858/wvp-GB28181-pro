package com.genersoft.iot.vmp.utils;

import com.genersoft.iot.vmp.common.CivilCodePo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum CivilCodeUtil {

    INSTANCE;
    private final static Logger logger = LoggerFactory.getLogger(CivilCodeUtil.class);

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
}
