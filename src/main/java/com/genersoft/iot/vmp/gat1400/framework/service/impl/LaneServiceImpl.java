package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;

import cz.data.viid.be.task.action.KeepaliveAction;
import cz.data.viid.fe.domain.LaneQuery;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.dto.LaneObject;
import cz.data.viid.framework.domain.entity.Lane;
import cz.data.viid.framework.mapper.LaneMapper;
import cz.data.viid.framework.service.LaneService;
import cz.data.viid.utils.JsonCommon;
import cz.data.viid.utils.StructCodec;

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
