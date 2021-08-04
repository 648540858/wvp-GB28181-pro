package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 报警相关业务处理
 */
public interface IDeviceAlarmService {

    /**
     * 根据多个添加获取报警列表
     * @param page 当前页
     * @param count 每页数量
     * @param deviceId 设备id
     * @param alarmPriority  报警级别, 1为一级警情, 2为二级警情, 3为三级警情, 4为四级 警情-
     * @param alarmMethod 报警方式 , 1为电话报警, 2为设备报警, 3为短信报警, 4为 GPS报警, 5为视频报警, 6为设备故障报警,
     * 	                            7其他报警;可以为直接组合如12为电话报警或 设备报警-
     * @param alarmType 报警类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 报警列表
     */
    PageInfo<DeviceAlarm> getAllAlarm(int page, int count, String deviceId, String alarmPriority, String alarmMethod,
                                      String alarmType, String startTime, String endTime);

    /**
     * 添加一个报警
     * @param deviceAlarm 添加报警
     */
    void add(DeviceAlarm deviceAlarm);

    /**
     * 清空时间以前的报警
     * @param id 数据库id
     * @param deviceIdList 制定需要清理的设备id
     * @param time 不写时间则清空所有时间的
     */
    int clearAlarmBeforeTime(Integer id, List<String> deviceIdList, String time);

}
