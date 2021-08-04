package com.genersoft.iot.vmp.vmanager.gb28181.alarm;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Api(tags = "报警信息管理")
@CrossOrigin
@RestController
@RequestMapping("/api/alarm")
public class AlarmController {

    @Autowired
    private IDeviceAlarmService deviceAlarmService;

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     *  分页查询报警
     *
     * @param deviceId 设备id
     * @param page 当前页
     * @param count 每页查询数量
     * @param alarmPriority  报警级别
     * @param alarmMethod 报警方式
     * @param alarmType  报警类型
     * @param startTime  开始时间
     * @param endTime 结束时间
     * @return
     */
    @ApiOperation("分页查询报警")
    @GetMapping("/all")
    @ApiImplicitParams({
            @ApiImplicitParam(name="deviceId", value = "设备id", dataTypeClass = String.class),
            @ApiImplicitParam(name="page", value = "当前页", required = true ,dataTypeClass = Integer.class),
            @ApiImplicitParam(name="count", value = "每页查询数量", required = true ,dataTypeClass = Integer.class),
            @ApiImplicitParam(name="alarmPriority", value = "查询内容" ,dataTypeClass = String.class),
            @ApiImplicitParam(name="alarmMethod", value = "查询内容" ,dataTypeClass = String.class),
            @ApiImplicitParam(name="alarmMethod", value = "查询内容" ,dataTypeClass = String.class),
            @ApiImplicitParam(name="alarmType", value = "查询内容" ,dataTypeClass = String.class),
            @ApiImplicitParam(name="startTime", value = "查询内容" ,dataTypeClass = String.class),
            @ApiImplicitParam(name="endTime", value = "查询内容" ,dataTypeClass = String.class),
    })
    public ResponseEntity<PageInfo<DeviceAlarm>> getAll(
                                             int page, int count,
                                             @RequestParam(required = false)  String deviceId,
                                             @RequestParam(required = false) String alarmPriority,
                                             @RequestParam(required = false) String alarmMethod,
                                             @RequestParam(required = false) String alarmType,
                                             @RequestParam(required = false) String startTime,
                                             @RequestParam(required = false) String endTime
                                             ) {
        if (StringUtils.isEmpty(alarmPriority)) alarmPriority = null;
        if (StringUtils.isEmpty(alarmMethod)) alarmMethod = null;
        if (StringUtils.isEmpty(alarmType)) alarmType = null;
        if (StringUtils.isEmpty(startTime)) startTime = null;
        if (StringUtils.isEmpty(endTime)) endTime = null;


        try {
            format.parse(startTime);
            format.parse(endTime);
        } catch (ParseException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        PageInfo<DeviceAlarm> allAlarm = deviceAlarmService.getAllAlarm(page, count, deviceId, alarmPriority, alarmMethod,
                alarmType, startTime, endTime);
        return new ResponseEntity<>(allAlarm, HttpStatus.OK);
    }


    /**
     *  删除报警
     *
     * @param id 报警id
     * @param deviceIds 多个设备id,逗号分隔
     * @param time 结束时间(这个时间之前的报警会被删除)
     * @return
     */
    @ApiOperation("分页查询报警")
    @DeleteMapping("/delete")
    @ApiImplicitParams({
            @ApiImplicitParam(name="id", value = "ID", required = false ,dataTypeClass = Integer.class),
            @ApiImplicitParam(name="deviceIds", value = "多个设备id,逗号分隔", required = false ,dataTypeClass = String.class),
            @ApiImplicitParam(name="time", value = "结束时间", required = false ,dataTypeClass = String.class),
    })
    public ResponseEntity<WVPResult<String>> delete(
                                              @RequestParam(required = false) Integer id,
                                              @RequestParam(required = false) String deviceIds,
                                              @RequestParam(required = false) String time
    ) {
        if (StringUtils.isEmpty(id)) id = null;
        if (StringUtils.isEmpty(deviceIds)) deviceIds = null;
        if (StringUtils.isEmpty(time)) time = null;
        try {
            if (time != null) {
                format.parse(time);
            }
        } catch (ParseException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        String[] deviceIdArray = deviceIds.split(",");
        List<String> deviceIdList = Arrays.asList(deviceIdArray);
        deviceAlarmService.clearAlarmBeforeTime(id, deviceIdList, time);
        WVPResult wvpResult = new WVPResult();
        wvpResult.setCode(0);
        wvpResult.setMsg("success");
        return new ResponseEntity<WVPResult<String>>(wvpResult, HttpStatus.OK);
    }


}
