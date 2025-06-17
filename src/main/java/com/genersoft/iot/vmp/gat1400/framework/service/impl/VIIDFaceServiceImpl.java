package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDFace;
import com.genersoft.iot.vmp.gat1400.framework.mapper.VIIDFaceMapper;
import com.genersoft.iot.vmp.gat1400.framework.service.VIIDFaceService;

import org.springframework.stereotype.Service;


@Service
public class VIIDFaceServiceImpl extends ServiceImpl<VIIDFaceMapper, VIIDFace>
        implements VIIDFaceService {
}
