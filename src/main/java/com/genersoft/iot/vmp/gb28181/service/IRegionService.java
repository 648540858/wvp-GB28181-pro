package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.Region;
import com.genersoft.iot.vmp.gb28181.bean.RegionTree;
import com.github.pagehelper.PageInfo;

import java.util.List;


public interface IRegionService {

    void add(Region region);

    boolean deleteByDeviceId(Integer regionDeviceId);

    /**
     * 查询区划列表
     */
    PageInfo<Region> query(String query, int page, int count);

    /**
     * 更新区域
     */
    void update(Region region);

    List<Region> getAllChild(String parent);

    Region queryRegionByDeviceId(String regionDeviceId);

    List<RegionTree> queryForTree(String query, Integer parent, Boolean hasChannel);

    void syncFromChannel();

    boolean delete(int id);

    boolean batchAdd(List<Region> regionList);

    List<Region> getPath(String deviceId);

    String getDescription(String civilCode);

    void addByCivilCode(String civilCode);
}
