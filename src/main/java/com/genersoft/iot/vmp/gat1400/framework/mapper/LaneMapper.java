package com.genersoft.iot.vmp.gat1400.framework.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

import cz.data.viid.framework.domain.entity.Lane;

@Mapper
public interface LaneMapper extends BaseMapper<Lane> {

    default Lane getByPrimaryKey(String tollgate, Integer laneId) {
        QueryWrapper<Lane> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(Lane::getTollgateId, tollgate)
                .eq(Lane::getLaneId, laneId);
        return selectOne(wrapper);
    }

}
