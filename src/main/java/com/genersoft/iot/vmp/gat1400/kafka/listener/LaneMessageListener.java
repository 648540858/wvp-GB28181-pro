package com.genersoft.iot.vmp.gat1400.kafka.listener;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.genersoft.iot.vmp.gat1400.fontend.domain.LaneQuery;
import com.genersoft.iot.vmp.gat1400.framework.SpringContextHolder;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.LaneObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.LaneObjectList;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotificationObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotifications;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.Lane;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.SubscribeNotificationRequest;
import com.genersoft.iot.vmp.gat1400.framework.service.LaneService;
import com.genersoft.iot.vmp.gat1400.utils.StructCodec;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
