package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarmNotify;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.alarm.DeviceAlarmEvent;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.service.IAlarmService;
import com.genersoft.iot.vmp.service.bean.Alarm;
import com.genersoft.iot.vmp.service.bean.AlarmType;
import com.genersoft.iot.vmp.storager.dao.AlarmMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements IAlarmService {

    private final AlarmMapper alarmMapper;

    private final UserSetting userSetting;

    private final SipConfig sipConfig;

    private final IDeviceChannelService deviceChannelService;

    private final IGbChannelPlayService gbChannelPlayService;

    private final IGbChannelService gbChannelService;

    // 使用Caffeine缓存设备通道信息，避免频繁查询数据库，提升性能
    private Cache<String, DeviceChannel> channelCache = null;

    private final ConcurrentLinkedQueue<Alarm> alarmQueue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() {
        // 初始化Caffeine缓存，设置合理的过期时间和最大容量
        channelCache = Caffeine.newBuilder()
                .maximumSize(userSetting.getAlarmCatchSize()) // 固定容量
                .build();
    }

    @Async
    @EventListener
    public void onApplicationEvent(DeviceAlarmEvent event) {
        if (channelCache == null || !sipConfig.isAlarm()) {
            return;
        }
        // 处理国标的报警事件，转换为通用的Alarm对象后缓存，在定时任务中批量保存到数据库
        if (event.getDeviceAlarmList().isEmpty()) {
            return;
        }
        log.info("收到设备报警事件，数量：{}", event.getDeviceAlarmList().size());
        for (DeviceAlarmNotify notify : event.getDeviceAlarmList()) {
            Alarm alarm = Alarm.buildFromDeviceAlarmNotify(notify);
            String key = notify.getDeviceId() + notify.getChannelId();
            DeviceChannel deviceChannel = channelCache.get(key, k -> deviceChannelService.getOneForSource(notify.getDeviceId(), notify.getChannelId()));
            if (deviceChannel == null) {
                continue;
            }
            alarm.setChannelId(deviceChannel.getId());
            // 分配一个快照路径，后续在去补充快照文件
            alarm.setSnapPath("snap/alarm_" + notify.getChannelId() + "_" + System.currentTimeMillis() + ".jpg");
            alarmQueue.offer(alarm);
        }
    }

    @Scheduled(fixedDelay = 500)
    public void executeAlarmQueue() {
        if (alarmQueue.isEmpty()) {
            return;
        }
        List<Alarm> handlerCatchDataList = new ArrayList<>();
        int size = alarmQueue.size();
        for (int i = 0; i < size; i++) {
            Alarm poll = alarmQueue.poll();
            if (poll != null) {
                handlerCatchDataList.add(poll);
            }
        }
        if (handlerCatchDataList.isEmpty()) {
            return;
        }
        // 批量保存到数据库
        int batchSize = 1000;
        for (int i = 0; i < handlerCatchDataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, handlerCatchDataList.size());
            List<Alarm> batchList = handlerCatchDataList.subList(i, end);
            alarmMapper.insertAlarms(batchList);
        }
        // 按照通道ID分组，去补充快照文件
        handlerCatchDataList.forEach(this::getSnapByAlarm);
    }

    @Async
    public void getSnapByAlarm(Alarm alarm) {
        CommonGBChannel channel = gbChannelService.getOne(alarm.getChannelId());
        if (channel == null) {
            log.warn("未找到报警关联的通道信息，alarmId：{}，channelId：{}", alarm.getId(), alarm.getChannelId());
            return;
        }
        gbChannelPlayService.getSnap(channel, (code, msg, data) -> {
            if (data == null) {
                return;
            }
            File file = new File(alarm.getSnapPath());
            if(file.exists()) {
                file.delete();
            }
            try {
                FileUtils.writeByteArrayToFile(file, data);
            } catch (Exception e) {
                log.warn("保存报警快照失败，alarmId：{}，channelId：{}", alarm.getId(), alarm.getChannelId(), e);
            }
        });
    }

    @Override
    public void saveAlarmInfo(Alarm alarm) {

    }

    @Override
    public PageInfo<Alarm> getAlarms(int page, int count, List<AlarmType> alarmType, String beginTime, String endTime) {
        PageHelper.startPage(page, count);
        Long beginTimeLong = null;
        Long endTimeLong = null;
        if (beginTime != null) {
            beginTimeLong = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(beginTime);
        }
        if (endTime != null) {
            endTimeLong = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(endTime);
        }
        List<Alarm> alarmList = alarmMapper.getAlarms(alarmType, beginTimeLong, endTimeLong);
        return new PageInfo<>(alarmList);
    }

    @Override
    public void deleteAlarmInfo(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            alarmMapper.deleteAlarms(ids);
        }
    }

    @Override
    public String getAlarmSnapById(Long id) {
        return alarmMapper.getSnapPathById(id);
    }

    @Override
    public StreamInfo getAlarmRecordById(Long id) {
        return null;
    }
}
