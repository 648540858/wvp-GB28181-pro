package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.genersoft.iot.vmp.gat1400.backend.task.action.KeepaliveAction;
import com.genersoft.iot.vmp.gat1400.fontend.domain.LaneQuery;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.LaneObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.Lane;
import com.genersoft.iot.vmp.gat1400.framework.mapper.LaneMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.genersoft.iot.vmp.gat1400.fontend.domain.TollgateQuery;
import com.genersoft.iot.vmp.gat1400.framework.service.LaneService;
import com.genersoft.iot.vmp.gat1400.utils.JsonCommon;
import com.genersoft.iot.vmp.gat1400.utils.StructCodec;

import java.util.Arrays;
import java.util.Objects;


@Service
public class LaneServiceImpl extends ServiceImpl<LaneMapper, Lane>
        implements LaneService {
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Page<Lane> pageData(LaneQuery params) {
        QueryWrapper<Lane> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(StringUtils.isNotBlank(params.getTollgateId()), Lane::getTollgateId, params.getTollgateId());
        wrapper.lambda().eq(Objects.nonNull(params.getLaneId()), Lane::getLaneId, params.getLaneId());
        wrapper.lambda().like(StringUtils.isNotBlank(params.getName()), Lane::getName, params.getName());
        wrapper.lambda().like(StringUtils.isNotBlank(params.getDesc()), Lane::getDesc, params.getDesc());
        return getBaseMapper().selectPage(params.pageable(), wrapper);
    }

    @Override
    public Lane getData(String tollgate, Integer laneId) {
        return getBaseMapper().getByPrimaryKey(tollgate, laneId);
    }

    @Override
    public boolean saveData(Lane entity) {
        boolean saved = save(entity);
        if (saved) {
            LaneObject laneObject = StructCodec.toLaneObject(entity);
            laneObject.setId(null);
            String topic = Constants.DEFAULT_TOPIC_PREFIX.LANE + KeepaliveAction.CURRENT_SERVER_ID;
            kafkaTemplate.send(topic, JsonCommon.toJson(laneObject));
        }
        return saved;
    }

    @Override
    public boolean updateData(Lane entity) {
        boolean saved = updateById(entity);
        if (saved) {
            LaneObject laneObject = StructCodec.toLaneObject(entity);
            laneObject.setId(null);
            String topic = Constants.DEFAULT_TOPIC_PREFIX.LANE + KeepaliveAction.CURRENT_SERVER_ID;
            kafkaTemplate.send(topic, JsonCommon.toJson(laneObject));
        }
        return saved;
    }

    @Transactional
    @Override
    public void removeData(String... ids) {
        removeBatchByIds(Arrays.asList(ids));
    }
}
