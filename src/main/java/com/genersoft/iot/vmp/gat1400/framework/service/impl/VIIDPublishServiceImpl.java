package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

import javax.annotation.Resource;

import cz.data.viid.fe.domain.PublishQuery;
import cz.data.viid.fe.domain.VIIDPublishRequest;
import cz.data.viid.framework.SpringContextHolder;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.entity.VIIDPublish;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.mapper.VIIDPublishMapper;
import cz.data.viid.framework.service.VIIDPublishService;
import cz.data.viid.framework.service.VIIDServerService;
import cz.data.viid.listener.event.VIIDPublishActiveEvent;
import cz.data.viid.listener.event.VIIDPublishInactiveEvent;
import cz.data.viid.utils.StructCodec;

@Service
public class VIIDPublishServiceImpl extends ServiceImpl<VIIDPublishMapper, VIIDPublish>
        implements VIIDPublishService {

    @Resource
    VIIDServerService serverService;

    @Override
    public Page<VIIDPublish> page(PublishQuery request) {
        QueryWrapper<VIIDPublish> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(request.getServerId())) {
            queryWrapper.lambda().eq(VIIDPublish::getServerId, request.getServerId());
        }
        if (StringUtils.isNotBlank(request.getSubscribeDetail())) {
            queryWrapper.lambda().eq(VIIDPublish::getSubscribeDetail, request.getSubscribeDetail());
        }
        if (StringUtils.isNotBlank(request.getTitle())) {
            queryWrapper.lambda().likeRight(VIIDPublish::getTitle, request.getTitle());
        }
        queryWrapper.lambda().orderByDesc(VIIDPublish::getCreateTime);
        return super.page(request.pageable(), queryWrapper);
    }

    @Transactional
    @Override
    public boolean addPublish(VIIDPublishRequest request) {
        VIIDServer domain = serverService.getByIdAndEnabled(request.getServerId());
        request.setResourceClass(Constants.ResourceClass.Instance.getValue());
        VIIDPublish publish = StructCodec.publishBuilder(request, domain);
        publish.setServerId(request.getServerId());
        publish.setCreateTime(new Date());
        boolean save = save(publish);
        SpringContextHolder.publishEvent(new VIIDPublishActiveEvent(publish));
        return save;
    }

    @Transactional
    @Override
    public boolean refreshPublish(VIIDPublishRequest request) {
        String subscribeId = request.getSubscribeId();
        VIIDPublish publish = getById(subscribeId);
        if (Objects.nonNull(publish)) {
            boolean remove = removeById(subscribeId);
            if (remove) {
                SpringContextHolder.publishEvent(new VIIDPublishInactiveEvent(publish));
                return this.addPublish(request);
            }
        }
        return false;
    }
}
