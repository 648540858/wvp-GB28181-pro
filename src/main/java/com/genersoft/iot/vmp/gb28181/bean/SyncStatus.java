package com.genersoft.iot.vmp.gb28181.bean;

/**
 * 摄像机同步状态
 */
public class SyncStatus {
    private int total;
    private int current;
    private String errorMsg;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
