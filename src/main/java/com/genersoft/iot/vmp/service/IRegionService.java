package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.service.bean.Region;
import com.github.pagehelper.PageInfo;

import java.util.List;


public interface IRegionService {

    void add(Region region);

    boolean deleteByDeviceId(String regionDeviceId);

    /**
     * 查询区划列表
     */
    PageInfo<Region> query(String query, int page, int count);

    /**
     * 查询子区划列表
     */
    PageInfo<Region> queryChildRegionList(String regionParentId, int page, int count);

    /**
     * 更新区域
     */
    void update(Region region);

    List<Region> getAllChild(String parent);

    Region queryRegionByDeviceId(String regionDeviceId);
}
