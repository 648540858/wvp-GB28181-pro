package com.genersoft.iot.vmp.gat1400.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.genersoft.iot.vmp.gat1400.backend.domain.container.DispositionListObject;
import com.genersoft.iot.vmp.gat1400.backend.domain.dto.DispositionObject;
import com.genersoft.iot.vmp.gat1400.backend.domain.vo.DispositionRequest;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDDisposition;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDServer;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.VIIDResponseStatusObject;
import com.genersoft.iot.vmp.gat1400.framework.exception.VIIDRuntimeException;
import com.genersoft.iot.vmp.gat1400.framework.mapper.VIIDDispositionMapper;
import com.genersoft.iot.vmp.gat1400.framework.service.IVIIDDispositionService;
import com.genersoft.iot.vmp.gat1400.framework.service.VIIDServerService;
import com.genersoft.iot.vmp.gat1400.rpc.DispositionClient;
import com.genersoft.iot.vmp.gat1400.utils.StructCodec;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Collections;

import javax.annotation.Resource;


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
