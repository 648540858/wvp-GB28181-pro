package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import cz.data.viid.be.task.action.KeepaliveAction;
import cz.data.viid.fe.domain.SubscribeQuery;
import cz.data.viid.fe.domain.VIIDSubscribeRequest;
import cz.data.viid.framework.SpringContextHolder;
import cz.data.viid.framework.domain.dto.ResponseStatusObject;
import cz.data.viid.framework.domain.dto.SubscribeObject;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.domain.entity.VIIDSubscribe;
import cz.data.viid.framework.domain.vo.SubscribesRequest;
import cz.data.viid.framework.domain.vo.VIIDResponseStatusObject;
import cz.data.viid.framework.exception.VIIDRuntimeException;
import cz.data.viid.framework.mapper.VIIDSubscribeMapper;
import cz.data.viid.framework.service.VIIDServerService;
import cz.data.viid.framework.service.VIIDSubscribeService;
import cz.data.viid.rpc.SubscribeClient;
import cz.data.viid.utils.ResponseUtil;
import cz.data.viid.utils.StructCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VIIDSubscribeServiceImpl extends ServiceImpl<VIIDSubscribeMapper, VIIDSubscribe>
        implements VIIDSubscribeService {

    @Resource
    VIIDServerService viidServerService;
    @Resource
    SubscribeClient subscribeClient;

    @Cacheable(value = "VIIDSubscribe", key = "#subscribeId", unless = "#result == null")
    @Override
    public VIIDSubscribe getCacheById(String subscribeId) {
        return getById(subscribeId);
    }

    @Override
    public VIIDResponseStatusObject subscribes(VIIDSubscribeRequest request) {
        VIIDServer setting = viidServerService.getCurrentServer();
        VIIDServer domain = viidServerService.getById(request.getServerId());
        boolean online = SpringContextHolder.getBean(KeepaliveAction.class).online(domain.getServerId());
        if (Boolean.FALSE.equals(online)) {
            throw new VIIDRuntimeException(String.format("视图库[%s]不在线请上线后订阅", domain.getServerName()));
        }
        SubscribeObject subscribe = StructCodec.inputSubscribeBuilder(request, domain, setting);
        //发送订阅请求
        URI uri = URI.create(domain.httpUrlBuilder());
        VIIDResponseStatusObject response = subscribeClient.addSubscribes(uri, SubscribesRequest.create(subscribe));
        if (ResponseUtil.validAllResponse(response)) {
            VIIDSubscribe entity = StructCodec.castSubscribe(subscribe);
            entity.setSubscribeDetail(request.getSubscribeDetail());
            entity.setServerId(request.getServerId());
            entity.setCreateTime(new Date());
            this.save(entity);
        }
        return response;
    }

    @Override
    public VIIDResponseStatusObject unSubscribes(String subscribeId) {
        VIIDSubscribe subscribe = this.getById(subscribeId);
        if (Objects.isNull(subscribe))
            return VIIDResponseStatusObject.from(new ResponseStatusObject("", "500", "订阅不存在"));
        VIIDServer domain = viidServerService.getById(subscribe.getServerId());
        boolean online = SpringContextHolder.getBean(KeepaliveAction.class).online(domain.getServerId());
        if (Boolean.FALSE.equals(online)) {
            throw new VIIDRuntimeException(String.format("视图库[%s]不在线请上线后取消订阅", domain.getServerName()));
        }
        URI uri = URI.create(domain.httpUrlBuilder());
        List<String> subscribeIds = Collections.singletonList(subscribe.getSubscribeId());
        VIIDResponseStatusObject response = subscribeClient.cancelSubscribes(uri, subscribeIds);
        if (ResponseUtil.validAllResponse(response)) {
            this.removeByIds(subscribeIds);
        }
        return response;
    }

    @Override
    public Page<VIIDSubscribe> list(SubscribeQuery request) {
        QueryWrapper<VIIDSubscribe> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(request.getServerId())) {
            queryWrapper.lambda().eq(VIIDSubscribe::getServerId, request.getServerId());
        }
        if (StringUtils.isNotBlank(request.getSubscribeDetail())) {
            queryWrapper.lambda().eq(VIIDSubscribe::getSubscribeDetail, request.getSubscribeDetail());
        }
        if (StringUtils.isNotBlank(request.getTitle())) {
            queryWrapper.lambda().likeRight(VIIDSubscribe::getTitle, request.getTitle());
        }
        queryWrapper.lambda().orderByDesc(VIIDSubscribe::getCreateTime);
        return super.page(request.pageable(), queryWrapper);
    }
}
