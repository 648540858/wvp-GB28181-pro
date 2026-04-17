package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.dao.MobilePositionMapper;
import com.genersoft.iot.vmp.gb28181.event.subscribe.mobilePosition.MobilePositionEvent;
import com.genersoft.iot.vmp.gb28181.service.IMobilePositionService;
import com.genersoft.iot.vmp.gb28181.service.ISourceOtherService;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {

    }

    @Async
    @EventListener
    public void onApplicationEvent(MobilePositionEvent event) {
        if (event.getMobilePositionList() == null || event.getMobilePositionList().isEmpty()) {
            return;
        }
        for (ISourceOtherService sourceOtherService : sourceOtherServiceMap.values()) {
            try {
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
        // TODO 发送通知，方便国标级联转发给上级


        // 批量保存到数据库
        int batchSize = 1000;
        for (int i = 0; i < handlerCatchDataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, handlerCatchDataList.size());
            List<MobilePosition> batchList = handlerCatchDataList.subList(i, end);
            mobilePositionMapper.insertMobilePositions(batchList);
        }
    }

}
