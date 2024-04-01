package com.genersoft.iot.vmp.media.abl.bean.hook;

public class OnRecordProgressABLHookParam extends OnRecordMp4ABLHookParam{
    private Integer currentFileDuration;
    private Integer TotalVideoDuration;

    public Integer getCurrentFileDuration() {
        return currentFileDuration;
    }

    public void setCurrentFileDuration(Integer currentFileDuration) {
        this.currentFileDuration = currentFileDuration;
    }

    public Integer getTotalVideoDuration() {
        return TotalVideoDuration;
    }

    public void setTotalVideoDuration(Integer totalVideoDuration) {
        TotalVideoDuration = totalVideoDuration;
    }
}
