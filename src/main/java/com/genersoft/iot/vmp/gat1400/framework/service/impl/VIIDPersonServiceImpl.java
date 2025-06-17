package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPerson;
import com.genersoft.iot.vmp.gat1400.framework.mapper.VIIDPersonMapper;
import com.genersoft.iot.vmp.gat1400.framework.service.VIIDPersonService;

import org.springframework.stereotype.Service;


@Service
public class VIIDPersonServiceImpl extends ServiceImpl<VIIDPersonMapper, VIIDPerson>
        implements VIIDPersonService {

}
