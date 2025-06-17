package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.genersoft.iot.vmp.gat1400.backend.task.action.KeepaliveAction;
import com.genersoft.iot.vmp.gat1400.fontend.domain.APEDeviceQuery;
import com.genersoft.iot.vmp.gat1400.framework.SpringContextHolder;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.APEDevice;
import com.genersoft.iot.vmp.gat1400.framework.mapper.APEDeviceMapper;
import com.genersoft.iot.vmp.gat1400.framework.service.APEDeviceService;
import com.genersoft.iot.vmp.gat1400.listener.event.DeviceChangeEvent;
import com.genersoft.iot.vmp.gat1400.utils.JsonCommon;
import com.genersoft.iot.vmp.gat1400.utils.StructCodec;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
public class APEDeviceServiceImpl extends ServiceImpl<APEDeviceMapper, APEDevice>
        implements APEDeviceService {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Page<APEDevice> page(APEDeviceQuery request) {
        QueryWrapper<APEDevice> wrapper = new QueryWrapper<>();
        wrapper.lambda().like(StringUtils.isNotBlank(request.getApeId()), APEDevice::getApeId, request.getApeId());
        wrapper.lambda().like(StringUtils.isNotBlank(request.getName()), APEDevice::getName, request.getName());
        wrapper.lambda().like(StringUtils.isNotBlank(request.getPlaceCode()), APEDevice::getPlaceCode, request.getPlaceCode());
        wrapper.lambda().like(StringUtils.isNotBlank(request.getIsOnline()), APEDevice::getIsOnline, request.getIsOnline());
        return page(request.pageable(), wrapper);
    }

    @Override
    public boolean saveDevice(APEDevice device) {
        boolean saved = save(device);
        if (saved) {
            this.pushKafka(device);
        }
        return saved;
    }

    @Override
    public boolean updateDevice(APEDevice device) {
        boolean update = updateById(device);
        if (update) {
            SpringContextHolder.publishEvent(new DeviceChangeEvent(device.getApeId()));
            this.pushKafka(device);
        }
        return update;
    }

    @Override
    public void deviceStatus(String deviceId, Constants.DeviceStatus status) {
        UpdateWrapper<APEDevice> update = new UpdateWrapper<>();
        update.lambda().eq(APEDevice::getApeId, deviceId);
        update.lambda().set(APEDevice::getIsOnline, status.getValue());
        this.update(update);
    }

    private void pushKafka(APEDevice device) {
        try {
            String topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.APE_DEVICE, KeepaliveAction.CURRENT_SERVER_ID);
            kafkaTemplate.send(topic, JsonCommon.toJson(StructCodec.castApeObject(device)));
        } catch (Exception e) {
            log.error("设备更新同步kafka错误:" + e.getMessage(), e);
        }
    }

}
