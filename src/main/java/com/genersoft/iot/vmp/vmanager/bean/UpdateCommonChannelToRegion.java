package com.genersoft.iot.vmp.vmanager.bean;

import java.util.List;

public class UpdateCommonChannelToRegion {

    private String commonGbCivilCode;

    private List<Integer> commonGbIds;

    public String getCommonGbCivilCode() {
        return commonGbCivilCode;
    }

    public void setCommonGbCivilCode(String commonGbCivilCode) {
        this.commonGbCivilCode = commonGbCivilCode;
    }

    public List<Integer> getCommonGbIds() {
        return commonGbIds;
    }

    public void setCommonGbIds(List<Integer> commonGbIds) {
        this.commonGbIds = commonGbIds;
    }
}
