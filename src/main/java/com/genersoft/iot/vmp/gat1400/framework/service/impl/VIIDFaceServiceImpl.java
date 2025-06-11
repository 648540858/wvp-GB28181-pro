package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

import cz.data.viid.framework.domain.entity.VIIDFace;
import cz.data.viid.framework.mapper.VIIDFaceMapper;
import cz.data.viid.framework.service.VIIDFaceService;

@Service
public class VIIDFaceServiceImpl extends ServiceImpl<VIIDFaceMapper, VIIDFace>
        implements VIIDFaceService {
}
