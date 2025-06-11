package com.genersoft.iot.vmp.gat1400.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import cz.data.viid.framework.domain.entity.TollgateDevice;

@Mapper
public interface TollgateDeviceMapper extends BaseMapper<TollgateDevice> {

    List<TollgateDevice> findUnSubscribeDevice(@Param("nodeId") String nodeId,
                                               @Param("orgCode") String orgCode);
}
