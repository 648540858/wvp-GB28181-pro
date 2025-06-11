package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

import cz.data.viid.framework.domain.entity.VIIDMotorVehicle;
import cz.data.viid.framework.mapper.VIIDMotorVehicleMapper;
import cz.data.viid.framework.service.VIIDMotorVehicleService;

@Service
public class VIIDMotorVehicleServiceImpl extends ServiceImpl<VIIDMotorVehicleMapper, VIIDMotorVehicle>
        implements VIIDMotorVehicleService {
}
