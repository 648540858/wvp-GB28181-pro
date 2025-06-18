package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.genersoft.iot.vmp.gat1400.fontend.domain.TollgateQuery;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.TollgateDevice;


public interface TollgateDeviceService extends IService<TollgateDevice> {

    Page<TollgateDevice> page(TollgateQuery request);

    String findTollgateIdByDeviceId(String serverFlag);

    boolean saveTollgate(TollgateDevice device);

    boolean updateTollgate(TollgateDevice device);
}
