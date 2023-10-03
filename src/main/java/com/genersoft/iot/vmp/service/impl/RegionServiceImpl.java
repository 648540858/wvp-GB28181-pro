package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.service.IRegionService;
import com.genersoft.iot.vmp.service.bean.Region;
import com.genersoft.iot.vmp.storager.dao.RegionMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 区域管理类
 */
@Service
public class RegionServiceImpl implements IRegionService {


    @Autowired
    private RegionMapper regionMapper;

    @Override
    public List<Region> getChildren(String parentDeviceId) {
        return regionMapper.getChildren(parentDeviceId);
    }

    @Override
    public void add(Region region) {
        regionMapper.add(region);
    }

    @Override
    public void deleteByDeviceId(String regionDeviceId) {
        regionMapper.deleteByDeviceId(regionDeviceId);
    }

    @Override
    public void updateRegionName(Region region) {
        regionMapper.updateRegionName(region.getCommonRegionName(), DateUtil.getNow(), region.getCommonRegionDeviceId());
    }
}
