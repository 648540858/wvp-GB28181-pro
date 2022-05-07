package com.genersoft.iot.vmp.domain.req;


/**
 * @author chenjialing
 */
public class PresetQuerySipReq {

    private String presetId;

    private String presetName;

    public String getPresetId() {
        return presetId;
    }

    public void setPresetId(String presetId) {
        this.presetId = presetId;
    }

    public String getPresetName() {
        return presetName;
    }

    public void setPresetName(String presetName) {
        this.presetName = presetName;
    }
}
