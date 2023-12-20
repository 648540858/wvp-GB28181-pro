package com.genersoft.iot.vmp.gb28181.bean;

import java.time.Instant;
import java.util.List;

/**
 * 预置位数据
 * @author lin
 */
public class PresetData {
    /**
     * 命令序列号
     */
    private int sn;
    private int total;
    private List<PresetItem> presetItemList;
    private Instant lastTime;
    private String errorMsg;

    private DataStatus status;


    public int getSn() {
        return sn;
    }

    public void setSn(int sn) {
        this.sn = sn;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<PresetItem> getPresetItemList() {
        return presetItemList;
    }

    public void setPresetItemList(List<PresetItem> presetItemList) {
        this.presetItemList = presetItemList;
    }

    public Instant getLastTime() {
        return lastTime;
    }

    public void setLastTime(Instant lastTime) {
        this.lastTime = lastTime;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public DataStatus getStatus() {
        return status;
    }

    public void setStatus(DataStatus status) {
        this.status = status;
    }
}
