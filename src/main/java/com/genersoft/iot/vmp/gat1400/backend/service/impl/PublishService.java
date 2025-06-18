package com.genersoft.iot.vmp.gat1400.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.genersoft.iot.vmp.gat1400.backend.service.IPublishService;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;
import com.genersoft.iot.vmp.gat1400.framework.mapper.VIIDPublishMapper;

import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PublishService extends ServiceImpl<VIIDPublishMapper, VIIDPublish>
        implements IPublishService {

    @Override
    public List<VIIDPublish> findListByServerId(String serverId, Integer status) {
        QueryWrapper<VIIDPublish> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(VIIDPublish::getServerId, serverId);
        if (status != null) {
            wrapper.lambda().eq(VIIDPublish::getSubscribeStatus, status);
        }
        return list(wrapper);
    }

    @Override
    public boolean updateSubscribe(String id, Constants.SubscribeStatus status) {
        UpdateWrapper<VIIDPublish> wrapper = new UpdateWrapper<>();
        wrapper.lambda().eq(VIIDPublish::getSubscribeId, id);
        wrapper.lambda().set(VIIDPublish::getSubscribeStatus, status.getValue());
        return update(wrapper);
    }
}
