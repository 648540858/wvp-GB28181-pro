package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDDisposition;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.VIIDResponseStatusObject;


public interface IVIIDDispositionService extends IService<VIIDDisposition> {

    VIIDDisposition getByDispositionId(String dispositionId);

    boolean revoke(String dispositionId);

    boolean updateDisposition(VIIDDisposition disposition);

    boolean deleteDisposition(String dispositionId);

    VIIDResponseStatusObject createRemote(VIIDDisposition disposition);

    VIIDResponseStatusObject updateRemote(VIIDDisposition disposition);

    VIIDResponseStatusObject revokeRemote(String dispositionId);

    VIIDResponseStatusObject deleteRemote(String dispositionId);
}
