package com.genersoft.iot.vmp.vmanager.bean;

import java.util.List;

public class UpdateCommonChannelToGroup {

    private String commonGbBusinessGroupID;

    private List<Integer> commonGbIds;

    public String getCommonGbBusinessGroupID() {
        return commonGbBusinessGroupID;
    }

    public void setCommonGbBusinessGroupID(String commonGbBusinessGroupID) {
        this.commonGbBusinessGroupID = commonGbBusinessGroupID;
    }

    public List<Integer> getCommonGbIds() {
        return commonGbIds;
    }

    public void setCommonGbIds(List<Integer> commonGbIds) {
        this.commonGbIds = commonGbIds;
    }
}
