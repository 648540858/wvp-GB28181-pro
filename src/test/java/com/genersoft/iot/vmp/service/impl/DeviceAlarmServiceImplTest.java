package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.service.IDeviceAlarmService;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;


@SpringBootTest
@RunWith(SpringRunner.class)
class DeviceAlarmServiceImplTest {

    @Resource
    private IDeviceAlarmService deviceAlarmService;

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @org.junit.jupiter.api.Test
    void getAllAlarm() {
//        deviceAlarmService.getAllAlarm(0, 10000, "11111111111111111111",null,null,null, null, null);
//        System.out.println(deviceAlarmService.getAllAlarm(0, 10000, null, null, null, null,
//                null, null).getSize());
//
//        System.out.println(deviceAlarmService.getAllAlarm(0, 10000, "11111111111111111111", null, null, null,
//                null, null).getSize());
//
//        System.out.println(deviceAlarmService.getAllAlarm(0, 10000, "11111111111111111111", "1", null, null,
//                null, null).getSize());
//
//        System.out.println(deviceAlarmService.getAllAlarm(0, 10000, "11111111111111111111", "2", null, null,
//                null, null).getSize());
//
//        System.out.println(deviceAlarmService.getAllAlarm(0, 10000, "11111111111111111111", "3", null, null,
//                null, null).getSize());
//
//        System.out.println(deviceAlarmService.getAllAlarm(0, 10000, "11111111111111111111", "4", null, null,
//                null, null).getSize());
//
//        System.out.println(deviceAlarmService.getAllAlarm(0, 10000, "11111111111111111111", "5", null, null,
//                null, null).getSize());
//
//        System.out.println(deviceAlarmService.getAllAlarm(0, 10000, "11111111111111111111", null, "1", null,
//                null, null).getSize());

//        System.out.println(deviceAlarmService.getAllAlarm(0, 10000, "11111111111111111111", null, "1", null,
//                null, null).getSize());


    }


    @org.junit.jupiter.api.Test
    void add() {
        for (int i = 0; i < 1000; i++) {
            DeviceAlarm deviceAlarm = new DeviceAlarm();
            deviceAlarm.setDeviceId("11111111111111111111");
            deviceAlarm.setAlarmDescription("test_" + i);

            /**
             * 报警方式 , 1为电话报警, 2为设备报警, 3为短信报警, 4为 GPS报警, 5为视频报警, 6为设备故障报警,
             * 	 * 7其他报警;可以为直接组合如12为电话报警或 设备报警-
             */
            deviceAlarm.setAlarmMethod((int)(Math.random()*7 + 1) + "");
            Date date = randomDate("2021-01-01 00:00:00", "2021-06-01 00:00:00");
            deviceAlarm.setAlarmTime(format.format(date));
            /**
             * 报警级别, 1为一级警情, 2为二级警情, 3为三级警情, 4为四级 警情-
             */
            deviceAlarm.setAlarmPriority((int)(Math.random()*4 + 1) + "");
            deviceAlarm.setLongitude(116.325);
            deviceAlarm.setLatitude(39.562);
            deviceAlarmService.add(deviceAlarm);
        }

    }

    @org.junit.jupiter.api.Test
    void clearAlarmBeforeTime() {
        deviceAlarmService.clearAlarmBeforeTime(null,null, null);
    }




    private Date randomDate(String beginDate, String endDate) {
        try {

            Date start = format.parse(beginDate);//构造开始日期
            Date end = format.parse(endDate);//构造结束日期
            //getTime()表示返回自 1970 年 1 月 1 日 00:00:00 GMT 以来此 Date 对象表示的毫秒数。
            if (start.getTime() >= end.getTime()) {
                return null;
            }
            long date = random(start.getTime(), end.getTime());
            return new Date(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static long random(long begin, long end) {
        long rtn = begin + (long) (Math.random() * (end - begin));
        //如果返回的是开始时间和结束时间，则递归调用本函数查找随机值
        if (rtn == begin || rtn == end) {
            return random(begin, end);
        }
        return rtn;
    }
}