package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

import cz.data.viid.framework.domain.entity.VIIDNonMotorVehicle;
import cz.data.viid.framework.mapper.VIIDNonMotorVehicleMapper;
import cz.data.viid.framework.service.VIIDNonMotorVehicleService;

@Service
public class VIIDNonMotorVehicleServiceImpl extends ServiceImpl<VIIDNonMotorVehicleMapper, VIIDNonMotorVehicle>
        implements VIIDNonMotorVehicleService {
}
