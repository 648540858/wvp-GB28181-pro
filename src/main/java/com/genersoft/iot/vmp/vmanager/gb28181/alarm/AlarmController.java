package com.genersoft.iot.vmp.vmanager.gb28181.alarm;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookListener;
import com.genersoft.iot.vmp.service.IDeviceAlarmService;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Tag(name = "报警信息管理")
@CrossOrigin
@RestController
@RequestMapping("/api/alarm")
public class AlarmController {

    private final static Logger logger = LoggerFactory.getLogger(AlarmController.class);

    @Autowired
    private IDeviceAlarmService deviceAlarmService;

    @Autowired
    private ISIPCommander commander;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private IVideoManagerStorage storage;


    /**
     *  删除报警
     *
     * @param id 报警id
     * @param deviceIds 多个设备id,逗号分隔
     * @param time 结束时间(这个时间之前的报警会被删除)
     * @return
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除报警")
    @Parameter(name = "id", description = "ID")
    @Parameter(name = "deviceIds", description = "多个设备id,逗号分隔")
    @Parameter(name = "time", description = "结束时间")
    public Integer delete(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String deviceIds,
            @RequestParam(required = false) String time
    ) {
        if (ObjectUtils.isEmpty(id)) {
            id = null;
        }
        if (ObjectUtils.isEmpty(deviceIds)) {
            deviceIds = null;
        }
        if (ObjectUtils.isEmpty(time)) {
            time = null;
        }
        if (!DateUtil.verification(time, DateUtil.formatter) ){
            return null;
        }
        List<String> deviceIdList = null;
        if (deviceIds != null) {
            String[] deviceIdArray = deviceIds.split(",");
            deviceIdList = Arrays.asList(deviceIdArray);
        }

        return deviceAlarmService.clearAlarmBeforeTime(id, deviceIdList, time);
    }

    /**
     *  测试向上级/设备发送模拟报警通知
     *
     * @param deviceId 报警id
     * @return
     */
    @GetMapping("/test/notify/alarm")
    @Operation(summary = "测试向上级/设备发送模拟报警通知")
    @Parameter(name = "deviceId", description = "设备国标编号")
    public void delete(@RequestParam String deviceId) {
        Device device = storage.queryVideoDevice(deviceId);
        ParentPlatform platform = storage.queryParentPlatByServerGBId(deviceId);
        DeviceAlarm deviceAlarm = new DeviceAlarm();
        deviceAlarm.setChannelId(deviceId);
        deviceAlarm.setAlarmDescription("test");
        deviceAlarm.setAlarmMethod("1");
        deviceAlarm.setAlarmPriority("1");
        deviceAlarm.setAlarmTime(DateUtil.formatterISO8601.format(LocalDateTime.now()));
        deviceAlarm.setAlarmType("1");
        deviceAlarm.setLongitude(115.33333);
        deviceAlarm.setLatitude(39.33333);

        if (device != null && platform == null) {

            try {
                commander.sendAlarmMessage(device, deviceAlarm);
            } catch (InvalidArgumentException | SipException | ParseException e) {

            }
        }else if (device == null && platform != null){
            try {
                commanderForPlatform.sendAlarmMessage(platform, deviceAlarm);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " +  e.getMessage());
            }
        }else {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"无法确定" + deviceId + "是平台还是设备");
        }

    }

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
    @Operation(summary = "分页查询报警")
    @Parameter(name = "page",description = "当前页",required = true)
    @Parameter(name = "count",description = "每页查询数量",required = true)
    @Parameter(name = "deviceId",description = "设备id")
    @Parameter(name = "alarmPriority",description = "查询内容")
    @Parameter(name = "alarmMethod",description = "查询内容")
    @Parameter(name = "alarmType",description = "每页查询数量")
    @Parameter(name = "startTime",description = "开始时间")
    @Parameter(name = "endTime",description = "结束时间")
    @GetMapping("/all")
    public PageInfo<DeviceAlarm> getAll(
            @RequestParam int page,
            @RequestParam int count,
            @RequestParam(required = false)  String deviceId,
            @RequestParam(required = false) String alarmPriority,
            @RequestParam(required = false) String alarmMethod,
            @RequestParam(required = false) String alarmType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        if (ObjectUtils.isEmpty(alarmPriority)) {
            alarmPriority = null;
        }
        if (ObjectUtils.isEmpty(alarmMethod)) {
            alarmMethod = null;
        }
        if (ObjectUtils.isEmpty(alarmType)) {
            alarmType = null;
        }
        if (ObjectUtils.isEmpty(startTime)) {
            startTime = null;
        }
        if (ObjectUtils.isEmpty(endTime)) {
            endTime = null;
        }


        if (!DateUtil.verification(startTime, DateUtil.formatter) || !DateUtil.verification(endTime, DateUtil.formatter)){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "开始时间或结束时间格式有误");
        }

        return deviceAlarmService.getAllAlarm(page, count, deviceId, alarmPriority, alarmMethod,
                alarmType, startTime, endTime);
    }
}
