package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.utils.ConfigConst;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 每一个zlm流媒体服务器，都设置MAX_STRTEAM_COUNT个可用同步信源(SSRC)
 */
@Data
public class SsrcConfig {
    /**
     * zlm流媒体服务器IP
     */
    String mediaServerIp;
    /**
     * zlm流媒体服务器已用会话句柄
     */
    private List<String> isUsed;
    /**
     * zlm流媒体服务器可用会话句柄
     */
    private List<String> notUsed;

    public void init(String mediaServerIp, Set<String> usedSet) {
        this.mediaServerIp = mediaServerIp;
        this.isUsed = new ArrayList<>();

        this.notUsed = new ArrayList<>();
        for (int i = 1; i < ConfigConst.MAX_STRTEAM_COUNT; i++) {
            String ssrc;
            if (i < 10) {
                ssrc = "000" + i;
            } else if (i < 100) {
                ssrc = "00" + i;
            } else if (i < 1000) {
                ssrc = "0" + i;
            } else {
                ssrc = String.valueOf(i);
            }
            if (null == usedSet || !usedSet.contains(ssrc)) {
                this.notUsed.add(ssrc);
            } else {
                this.isUsed.add(ssrc);
            }
        }
    }
}
