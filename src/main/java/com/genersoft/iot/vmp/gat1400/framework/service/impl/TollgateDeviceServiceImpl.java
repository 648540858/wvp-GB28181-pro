package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.genersoft.iot.vmp.gat1400.backend.task.action.KeepaliveAction;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.TollgateDevice;
import com.genersoft.iot.vmp.gat1400.utils.JsonCommon;
import com.genersoft.iot.vmp.gat1400.utils.StructCodec;
import com.genersoft.iot.vmp.gat1400.fontend.domain.TollgateQuery;
import com.genersoft.iot.vmp.gat1400.framework.mapper.TollgateDeviceMapper;
import com.genersoft.iot.vmp.gat1400.framework.service.TollgateDeviceService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
public class TollgateDeviceServiceImpl extends ServiceImpl<TollgateDeviceMapper, TollgateDevice>
        implements TollgateDeviceService {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Page<TollgateDevice> page(TollgateQuery request) {
        QueryWrapper<TollgateDevice> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(StringUtils.isNotBlank(request.getTollgateId()), TollgateDevice::getTollgateId, request.getTollgateId());
        wrapper.lambda().like(StringUtils.isNotBlank(request.getName()), TollgateDevice::getName, request.getName());
        wrapper.lambda().likeRight(StringUtils.isNotBlank(request.getOrgCode()), TollgateDevice::getOrgCode, request.getOrgCode());
        return page(request.pageable(), wrapper);
    }

    @Cacheable(value = "findTollgateIdByDeviceId", key = "#deviceId", unless = "#result == null")
    @Override
    public String findTollgateIdByDeviceId(String deviceId) {
        QueryWrapper<TollgateDevice> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(TollgateDevice::getDeviceId, deviceId);
        return list(wrapper).stream().findFirst().map(TollgateDevice::getTollgateId).orElse("");
    }

    @CacheEvict(value = "findTollgateIdByDeviceId", key = "#device.tollgateId")
    @Override
    public boolean saveTollgate(TollgateDevice device) {
        boolean saved = save(device);
        if (saved) {
            this.pushKafka(device);
        }
        return saved;
    }

    @CacheEvict(value = "findTollgateIdByDeviceId", key = "#device.tollgateId")
    @Override
    public boolean updateTollgate(TollgateDevice device) {
        boolean updated = updateById(device);
        if (updated) {
            this.pushKafka(device);
        }
        return updated;
    }

    private void pushKafka(TollgateDevice tollgate) {
        try {
            String topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.TOLLGATE_DEVICE, KeepaliveAction.CURRENT_SERVER_ID);
            kafkaTemplate.send(topic, JsonCommon.toJson(StructCodec.castTollgateObject(tollgate)));
        } catch (Exception e) {
            log.error("卡口更新同步kafka错误:" + e.getMessage(), e);
        }
    }
}
