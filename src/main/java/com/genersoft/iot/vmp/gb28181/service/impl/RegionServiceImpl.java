package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.conf.CivilCodeFileConf;
import com.genersoft.iot.vmp.gb28181.bean.Region;
import com.genersoft.iot.vmp.gb28181.dao.RegionMapper;
import com.genersoft.iot.vmp.gb28181.service.IRegionService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;

/**
 * 区域管理类
 */
@Service
public class RegionServiceImpl implements IRegionService {


    @Autowired
    private RegionMapper regionMapper;


    @Autowired
    private GbChannelServiceImpl gbChannelService;


    @Autowired
    private CivilCodeFileConf civilCodeFileConf;

    @Override
    public void add(Region region) {
        assert region.getName() != null;
        assert region.getDeviceId() != null;
        if (ObjectUtils.isEmpty(region.getParentDeviceId().trim())) {
            region.setParentDeviceId(null);
        }
        region.setCreateTime(DateUtil.getNow());
        region.setUpdateTime(DateUtil.getNow());
        regionMapper.add(region);
    }

    @Override
    @Transactional
    public boolean deleteByDeviceId(String regionDeviceId) {

        return true;
    }

    @Override
    public PageInfo<Region> query(String query, int page, int count) {
        PageHelper.startPage(page, count);
        List<Region> regionList =  regionMapper.query(query, null);
        return new PageInfo<>(regionList);
    }

    @Override
    public PageInfo<Region> queryChildRegionList(String regionParentId, int page, int count) {
        assert regionParentId != null;
        PageHelper.startPage(page, count);
        List<Region> all = regionMapper.getChildren(regionParentId);
        return new PageInfo<>(all);
    }

    @Override
    @Transactional
    public void update(Region region) {

    }

    @Override
    public List<Region> getAllChild(String parent) {
        List<Region> allChild = civilCodeFileConf.getAllChild(parent);
        Collections.sort(allChild);
        return allChild;
    }

    @Override
    public Region queryRegionByDeviceId(String regionDeviceId) {
        return null;
    }
}
