package com.genersoft.iot.vmp.vmanager.channel.bean;

import com.genersoft.iot.vmp.common.CommonGbChannel;

public class ShareCommonChannelListResult extends CommonGbChannel {

    private int platformId;

    public int getPlatformId() {
        return platformId;
    }

    public void setPlatformId(int platformId) {
        this.platformId = platformId;
    }
}
