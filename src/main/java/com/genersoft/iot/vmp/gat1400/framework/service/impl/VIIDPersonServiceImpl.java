package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

import cz.data.viid.framework.domain.entity.VIIDPerson;
import cz.data.viid.framework.mapper.VIIDPersonMapper;
import cz.data.viid.framework.service.VIIDPersonService;

@Service
public class VIIDPersonServiceImpl extends ServiceImpl<VIIDPersonMapper, VIIDPerson>
        implements VIIDPersonService {

}
