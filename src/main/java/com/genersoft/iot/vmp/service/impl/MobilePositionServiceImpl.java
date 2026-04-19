package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.dao.MobilePositionMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformMapper;
import com.genersoft.iot.vmp.gb28181.event.subscribe.mobilePosition.MobilePositionEvent;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.service.ISourceOtherService;
import com.genersoft.iot.vmp.service.IMobilePositionService;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobilePositionServiceImpl implements IMobilePositionService {

    private final ConcurrentLinkedQueue<MobilePosition> mobilePositionQueue = new ConcurrentLinkedQueue<>();

    private final Map<String, ISourceOtherService> sourceOtherServiceMap;

    private final MobilePositionMapper mobilePositionMapper;

    private final IPlatformChannelService platformChannelService;

    private final PlatformMapper platformMapper;

    /**
     * 查询移动位置轨迹
     */
    @Override
    public synchronized List<MobilePosition> queryMobilePositions(Integer channelId, String startTime, String endTime) {
        Long startTimestamp = null;
        Long endTimestamp = null;
        if (startTime != null) {
            startTimestamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(startTime);
        }
        if (endTime != null) {
            endTimestamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(endTime);
        }
        return mobilePositionMapper.queryPositionByDeviceIdAndTime(channelId, startTimestamp, endTimestamp);
    }

    @Override
    public List<Platform> queryEnablePlatformListWithAsMessageChannel() {
        return platformMapper.queryEnablePlatformListWithAsMessageChannel();
    }

    /**
     * 查询最新移动位置
     */
    @Override
    public MobilePosition queryLatestPosition(Integer channelId) {
        return mobilePositionMapper.queryLatestPosition(channelId);
    }

    @Async
    @EventListener
    public void onApplicationEvent(MobilePositionEvent event) {
        if (event.getMobilePositionList() == null || event.getMobilePositionList().isEmpty()) {
            return;
        }
        if (event.getMobilePositionList().get(0).getChannelId() != null) {
            mobilePositionQueue.addAll(event.getMobilePositionList());
            return;
        }
        for (ISourceOtherService sourceOtherService : sourceOtherServiceMap.values()) {
            try {
                // 此时已经完成了通道ID的添加，以及坐标系的转换，后续只需要将数据保存到数据库即可
                Boolean addResult = sourceOtherService.addChannelIdForMobilePosition(event.getMobilePositionList());
                if (addResult != null && addResult) {
                    mobilePositionQueue.addAll(event.getMobilePositionList());
                }
            }catch (Exception e) {
                log.error("[移动位置事件] 处理移动位置事件失败", e);
            }
        }
    }

    @Scheduled(fixedDelay = 500)
    public void executeMobilePositionQueue() {
        if (mobilePositionQueue.isEmpty()) {
            return;
        }
        List<MobilePosition> handlerCatchDataList = new ArrayList<>();
        int size = mobilePositionQueue.size();
        for (int i = 0; i < size; i++) {
            MobilePosition poll = mobilePositionQueue.poll();
            if (poll != null) {
                handlerCatchDataList.add(poll);
            }
        }
        if (handlerCatchDataList.isEmpty()) {
            return;
        }
        List<MobilePosition> mobilePositionList = handlerCatchDataList.stream().filter(
                mobilePosition -> mobilePosition.getChannelId() != 0).toList();
        // 发送通知，方便国标级联转发给上级
        Thread.startVirtualThread(() -> platformChannelService.notifyMobilePosition(mobilePositionList));

        // 批量保存到数据库
        int batchSize = 1000;
        for (int i = 0; i < mobilePositionList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, mobilePositionList.size());
            List<MobilePosition> batchList = mobilePositionList.subList(i, end);
            mobilePositionMapper.batchAdd(batchList);
        }
    }

}
