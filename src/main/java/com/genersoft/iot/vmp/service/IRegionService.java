package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.service.bean.Region;
import com.github.pagehelper.PageInfo;


public interface IRegionService {

    void add(Region region);

    void deleteByDeviceId(String regionDeviceId);

    /**
     * 查询区划列表
     */
    PageInfo<Region> query(String query, int page, int count);

    /**
     * 查询子区划列表
     */
    PageInfo<Region> queryChildGroupList(String regionParentId, int page, int count);

    /**
     * 更新区域
     */
    void update(Region region);
}
