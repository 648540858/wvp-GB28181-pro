package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.genersoft.iot.vmp.gat1400.fontend.domain.ServerQuery;
import com.genersoft.iot.vmp.gat1400.framework.SpringContextHolder;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDServer;
import com.genersoft.iot.vmp.gat1400.framework.mapper.VIIDServerMapper;
import com.genersoft.iot.vmp.gat1400.framework.service.VIIDServerService;
import com.genersoft.iot.vmp.gat1400.listener.event.ServerChangeEvent;
import com.genersoft.iot.vmp.gat1400.utils.StructCodec;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VIIDServerServiceImpl extends ServiceImpl<VIIDServerMapper, VIIDServer>
        implements VIIDServerService {

    @Override
    public void afterPropertiesSet() {
        List<VIIDServer> list = list();
        boolean existsCurrentServer = list.stream()
                .anyMatch(ele -> Constants.InstanceCategory.THIS.getValue().equals(ele.getCategory()));
        if (!existsCurrentServer) {
            save(StructCodec.createDefaultVIIDServer());
        }
    }

    @Override
    public VIIDServer getByIdAndEnabled(String id) {
        QueryWrapper<VIIDServer> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VIIDServer::getServerId, id);
        queryWrapper.lambda().eq(VIIDServer::getEnabled, true);
        return super.getOne(queryWrapper);
    }


    @Override
    public VIIDServer getCurrentServer() {
        VIIDServerService current = SpringContextHolder.getBean(VIIDServerService.class);
        return current.getCurrentServer("1");
    }

    @Cacheable(value = "CACHE_SERVER_ME", key = "'me'", condition = "#useCache == '1'")
    @Override
    public VIIDServer getCurrentServer(String useCache) {
        QueryWrapper<VIIDServer> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VIIDServer::getCategory, Constants.InstanceCategory.THIS.getValue());
        return super.getOne(queryWrapper);
    }

    @Override
    public Page<VIIDServer> page(ServerQuery request) {
        QueryWrapper<VIIDServer> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(StringUtils.isNotBlank(request.getServerId()),
                VIIDServer::getServerId, request.getServerId());
        queryWrapper.lambda().like(StringUtils.isNotBlank(request.getServerName()),
                VIIDServer::getServerName, request.getServerName());
        queryWrapper.lambda().eq(StringUtils.isNotBlank(request.getCategory()),
                VIIDServer::getCategory, request.getCategory());
        queryWrapper.lambda().eq(StringUtils.isNotBlank(request.getIsOnline()),
                VIIDServer::getOnline, request.getIsOnline());
        if (request.isExcludeSelf()) {
            queryWrapper.lambda().ne(VIIDServer::getCategory, Constants.InstanceCategory.THIS.getValue());
        }
        queryWrapper.lambda().orderByDesc(VIIDServer::getCreateTime);
        return super.page(request.pageable(), queryWrapper);
    }

    @Override
    public Page<VIIDServer> list(ServerQuery request) {
        QueryWrapper<VIIDServer> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(StringUtils.isNotBlank(request.getServerId()),
                VIIDServer::getServerId, request.getServerId());
        queryWrapper.lambda().like(StringUtils.isNotBlank(request.getServerName()),
                VIIDServer::getServerName, request.getServerName());
        queryWrapper.lambda().eq(StringUtils.isNotBlank(request.getCategory()),
                VIIDServer::getCategory, request.getCategory());
        queryWrapper.lambda().eq(StringUtils.isNotBlank(request.getIsOnline()),
                VIIDServer::getOnline, request.getIsOnline());
        if (request.isExcludeSelf()) {
            queryWrapper.lambda().ne(VIIDServer::getCategory, Constants.InstanceCategory.THIS.getValue());
        }
        queryWrapper.lambda().orderByDesc(VIIDServer::getCreateTime);
        return super.page(request.pageable(), queryWrapper);
    }

    @Override
    public boolean upsert(VIIDServer request) {
        VIIDServer old = getById(request.getServerId());
        boolean res;
        if (Objects.nonNull(old)) {
            res = super.updateById(request);
            this.changeDomain(request.getServerId());
        } else {
            request.setEnabled(false);
            request.setKeepalive(false);
            if (StringUtils.isBlank(request.getCategory())) {
                request.setCategory(Constants.InstanceCategory.DOWN.getValue());
            }
            request.setCreateTime(new Date());
            res = this.save(request);
        }
        return res;
    }

    @Override
    public void changeDomain(String source) {
        SpringContextHolder.publishEvent(new ServerChangeEvent(source));
    }

    @CacheEvict(value = "CACHE_SERVER_ME", key = "'me'")
    @Override
    public boolean maintenance(VIIDServer viidServer) {
        VIIDServer me = this.getCurrentServer();
        if (Objects.nonNull(me)) {
            viidServer.setServerId(me.getServerId());
        }
        viidServer.setCategory(Constants.InstanceCategory.THIS.getValue());
        return saveOrUpdate(viidServer);
    }

    @Override
    public List<VIIDServer> activeKeepaliveServer() {
        QueryWrapper<VIIDServer> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(VIIDServer::getKeepalive, Boolean.TRUE);
        wrapper.lambda().eq(VIIDServer::getEnabled, Boolean.TRUE);
        return list(wrapper);
    }

    @Override
    public void instanceStatus(String serverId, Constants.DeviceStatus status) {
        UpdateWrapper<VIIDServer> update = new UpdateWrapper<>();
        update.lambda().eq(VIIDServer::getServerId, serverId);
        update.lambda().set(VIIDServer::getOnline, status.getValue());
        this.update(update);
    }
}
