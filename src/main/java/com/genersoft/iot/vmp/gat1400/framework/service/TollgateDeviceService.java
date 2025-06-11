package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import cz.data.viid.fe.domain.TollgateQuery;
import cz.data.viid.framework.domain.entity.TollgateDevice;

public interface TollgateDeviceService extends IService<TollgateDevice> {

    Page<TollgateDevice> page(TollgateQuery request);

    String findTollgateIdByDeviceId(String serverFlag);

    boolean saveTollgate(TollgateDevice device);

    boolean updateTollgate(TollgateDevice device);
}
