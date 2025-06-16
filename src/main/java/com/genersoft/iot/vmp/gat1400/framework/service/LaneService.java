package com.genersoft.iot.vmp.gat1400.framework.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.genersoft.iot.vmp.gat1400.fontend.domain.LaneQuery;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.Lane;


public interface LaneService extends IService<Lane> {

    Page<Lane> pageData(LaneQuery request);

    Lane getData(String tollgate, Integer laneId);

    boolean saveData(Lane entity);

    boolean updateData(Lane entity);

    void removeData(String... ids);
}
