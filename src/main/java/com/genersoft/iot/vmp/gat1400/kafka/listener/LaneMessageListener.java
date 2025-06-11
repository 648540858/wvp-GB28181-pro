package com.genersoft.iot.vmp.gat1400.kafka.listener;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import cz.data.viid.fe.domain.LaneQuery;
import cz.data.viid.framework.SpringContextHolder;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.dto.*;
import cz.data.viid.framework.domain.entity.Lane;
import cz.data.viid.framework.domain.entity.VIIDPublish;
import cz.data.viid.framework.domain.vo.SubscribeNotificationRequest;
import cz.data.viid.framework.service.LaneService;
import cz.data.viid.utils.StructCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LaneMessageListener extends AbstractMessageListener<LaneObject> {

    public LaneMessageListener(VIIDPublish publish) {
        super(publish, Constants.SubscribeDetail.Lanes.getValue());
    }

    @Override
    public LaneObject messageConverter(String value) {
        return Optional.of(value)
                .map(ele -> JSONObject.parseObject(ele, LaneObject.class))
                .filter(ele -> StringUtils.isNotBlank(ele.getTollgateId()))
                .filter(ele -> Objects.isNull(ele.getLaneId()))
                .map(ele -> {
                    ele.setId(null);
                    return ele;
                }).orElse(null);
    }

    @Override
    public SubscribeNotificationRequest packHandler(List<LaneObject> partition) {
        LaneObjectList container = new LaneObjectList();
        container.setLaneObject(partition);

        SubscribeNotificationObject subscribeNotificationObject = new SubscribeNotificationObject();
        subscribeNotificationObject.setNotificationID(StructCodec.randomNotificationID(publish.getSubscribeId()));
        subscribeNotificationObject.setSubscribeID(publish.getSubscribeId());
        subscribeNotificationObject.setTitle(publish.getTitle());
        subscribeNotificationObject.setTriggerTime(DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
        subscribeNotificationObject.setInfoIDs(partition
                .stream()
                .map(laneObject -> String.join("-", laneObject.getTollgateId(), String.valueOf(laneObject.getLaneId())))
                .collect(Collectors.joining(","))
        );
        subscribeNotificationObject.setLaneObjectList(container);
        SubscribeNotifications notifications = new SubscribeNotifications();
        notifications.setSubscribeNotificationObject(Collections.singletonList(subscribeNotificationObject));
        SubscribeNotificationRequest request = new SubscribeNotificationRequest();
        request.setSubscribeNotificationListObject(notifications);
        return request;
    }

    @Override
    public void scheduler() {
        LaneService service = SpringContextHolder.getBean(LaneService.class);
        int page = 1;
        final int size = 200;
        boolean flag = true;
        while (flag) {
            LaneQuery request = new LaneQuery();
            request.setPageNum(page);
            request.setPageSize(size);
            Page<Lane> pair = service.pageData(request);
            List<LaneObject> laneObjects = pair.getRecords().stream().map(StructCodec::toLaneObject).collect(Collectors.toList());
            try {
                super.notificationRequest(laneObjects);
            } catch (IOException e) {
                log.warn("推送车道目录出错: {}", e.getMessage());
                break;
            }
            long total = pair.getTotal();
            if (total <= ((long) page * size)) {
                flag = false;
            }
            page++;
        }
    }
}
