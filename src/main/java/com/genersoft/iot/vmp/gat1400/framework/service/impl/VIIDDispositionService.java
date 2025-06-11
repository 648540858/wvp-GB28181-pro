package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Collections;

import javax.annotation.Resource;

import cz.data.viid.be.domain.container.DispositionListObject;
import cz.data.viid.be.domain.dto.DispositionObject;
import cz.data.viid.be.domain.vo.DispositionRequest;
import cz.data.viid.framework.domain.entity.VIIDDisposition;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.domain.vo.VIIDResponseStatusObject;
import cz.data.viid.framework.exception.VIIDRuntimeException;
import cz.data.viid.framework.mapper.VIIDDispositionMapper;
import cz.data.viid.framework.service.IVIIDDispositionService;
import cz.data.viid.framework.service.VIIDServerService;
import cz.data.viid.rpc.DispositionClient;
import cz.data.viid.utils.StructCodec;

@Service
public class VIIDDispositionService extends ServiceImpl<VIIDDispositionMapper, VIIDDisposition>
        implements IVIIDDispositionService {

    @Resource
    DispositionClient dispositionClient;
    @Resource
    VIIDServerService serverService;

    @Override
    public VIIDDisposition getByDispositionId(String dispositionId) {
        QueryWrapper<VIIDDisposition> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(VIIDDisposition::getDispositionId, dispositionId);
        return getOne(wrapper);
    }

    @Override
    public boolean revoke(String dispositionId) {
        UpdateWrapper<VIIDDisposition> update = new UpdateWrapper<>();
        update.lambda().eq(VIIDDisposition::getDispositionId, dispositionId);
        update.lambda().set(VIIDDisposition::getDispositionStatus, "1");
        return update(update);
    }

    @Override
    public boolean updateDisposition(VIIDDisposition disposition) {
        VIIDDisposition dbDisposition = getByDispositionId(disposition.getDispositionId());
        if (dbDisposition == null) {
            return false;
        }
        disposition.setId(dbDisposition.getId());
        return updateById(disposition);
    }

    @Override
    public boolean deleteDisposition(String dispositionId) {
        VIIDDisposition dbDisposition = getByDispositionId(dispositionId);
        if (dbDisposition == null) {
            return false;
        }
        return removeById(dispositionId);
    }

    @Transactional
    @Override
    public VIIDResponseStatusObject createRemote(VIIDDisposition disposition) {
        this.save(disposition);
        VIIDServer server = serverService.getByIdAndEnabled(disposition.getServerId());
        DispositionObject dispositionObject = StructCodec.castDisposition(disposition);
        DispositionRequest request = new DispositionRequest(
                new DispositionListObject(Collections.singletonList(dispositionObject))
        );
        return dispositionClient.createDisposition(URI.create(server.httpUrlBuilder()), request);
    }

    @Transactional
    @Override
    public VIIDResponseStatusObject updateRemote(VIIDDisposition disposition) {
        boolean updated = this.updateDisposition(disposition);
        if (!updated) {
            throw new VIIDRuntimeException("更新布控失败");
        }
        VIIDServer server = serverService.getByIdAndEnabled(disposition.getServerId());
        DispositionObject dispositionObject = StructCodec.castDisposition(disposition);
        DispositionRequest request = new DispositionRequest(
                new DispositionListObject(Collections.singletonList(dispositionObject))
        );
        return dispositionClient.updateDisposition(URI.create(server.httpUrlBuilder()), request);
    }

    @Transactional
    @Override
    public VIIDResponseStatusObject revokeRemote(String dispositionId) {
        VIIDDisposition disposition = getByDispositionId(dispositionId);
        if (disposition == null)
            throw new VIIDRuntimeException("布控不存在");
        boolean revoked = revoke(disposition.getDispositionId());
        if (!revoked)
            throw new VIIDRuntimeException("布控撤控失败");
        VIIDServer server = serverService.getByIdAndEnabled(disposition.getServerId());
        dispositionId = disposition.getDispositionId();
        return dispositionClient.revokeDisposition(URI.create(server.httpUrlBuilder()), dispositionId);
    }

    @Transactional
    @Override
    public VIIDResponseStatusObject deleteRemote(String dispositionId) {
        VIIDDisposition disposition = getByDispositionId(dispositionId);
        if (disposition == null)
            return null;
        boolean revoked = revoke(disposition.getDispositionId());
        if (!revoked)
            return null;
        VIIDServer server = serverService.getByIdAndEnabled(disposition.getServerId());
        dispositionId = disposition.getDispositionId();
        return dispositionClient.deleteDisposition(URI.create(server.httpUrlBuilder()), dispositionId);
    }
}
