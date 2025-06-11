package com.genersoft.iot.vmp.gat1400.listener;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cz.data.viid.framework.SpringContextHolder;
import cz.data.viid.framework.mapper.MetricsMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(value = "VIID_DB_STORAGE", havingValue = "true")
public class ClearDataSchedule {

    @Value("${VIID_DATA_EXPIRE:7}")
    int expireDays;

    /**
     * 每天凌晨清理过期数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void clear() {
        final int day = expireDays;
        log.info("开始回收{}天之前的数据库记录...", day);
        MetricsMapper bean = SpringContextHolder.getBean(MetricsMapper.class);
        bean.releaseFace(day);
        bean.releasePerson(day);
        bean.releaseVehicle(day);
        bean.releaseNonVehicle(day);
    }
}
