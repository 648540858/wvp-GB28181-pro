package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 摄像机同步状态
 * @author lin
 */
@Schema(description = "摄像机同步状态")
public class SyncStatus {
    @Schema(description = "总数")
    private int total;
    @Schema(description = "当前更新多少")
    private int current;
    @Schema(description = "错误描述")
    private String errorMsg;
    @Schema(description = "是否同步中")
    private boolean syncIng;

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

    public boolean isSyncIng() {
        return syncIng;
    }

    public void setSyncIng(boolean syncIng) {
        this.syncIng = syncIng;
    }
}
