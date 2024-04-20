package com.genersoft.iot.vmp.jt1078.bean.config;

/**
 * 违规行驶时段范围 ,精确到分
 */
public class IllegalDrivingPeriods {
    /**
     * 违规行驶时段-开始时间 HH:mm
     */
    private String startTime;

    /**
     * 违规行驶时段-结束时间 HH:mm
     */
    private String endTime;

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
