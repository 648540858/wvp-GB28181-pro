package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.service.IRegionService;
import com.genersoft.iot.vmp.service.bean.Region;
import com.genersoft.iot.vmp.storager.dao.CommonGbChannelMapper;
import com.genersoft.iot.vmp.storager.dao.RegionMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 区域管理类
 */
@Service
public class RegionServiceImpl implements IRegionService {


    @Autowired
    private RegionMapper regionMapper;


    @Autowired
    private CommonGbChannelMapper commonGbChannelMapper;

    @Override
    public void add(Region region) {
        assert region.getCommonRegionName() != null;
        assert region.getCommonRegionDeviceId() != null;
        assert region.getCommonRegionParentId() != null;
        region.setCommonRegionCreateTime(DateUtil.getNow());
        region.setCommonRegionUpdateTime(DateUtil.getNow());
        regionMapper.add(region);
    }

    @Override
    public void deleteByDeviceId(String regionDeviceId) {
        regionMapper.deleteByDeviceId(regionDeviceId);
    }

    @Override
    public PageInfo<Region> query(String query, int page, int count) {
        PageHelper.startPage(page, count);
        List<Region> regionList =  regionMapper.query(query);
        return new PageInfo<>(regionList);
    }

    @Override
    public PageInfo<Region> queryChildGroupList(String regionParentId, int page, int count) {
        assert regionParentId != null;
        PageHelper.startPage(page, count);
        List<Region> all = regionMapper.getChildren(regionParentId);
        return new PageInfo<>(all);
    }

    @Override
    @Transactional
    public void update(Region region) {
        assert region.getCommonRegionId() > 0;
        assert region.getCommonRegionDeviceId() != null;
        assert region.getCommonRegionName() != null;
        region.setCommonRegionUpdateTime(DateUtil.getNow());
        Region regionInDb = regionMapper.queryRegion(region.getCommonRegionId());
        if (regionInDb == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到待更新的数据");
        }
        region.setCommonRegionCreateTime(regionInDb.getCommonRegionCreateTime());
        if (!region.getCommonRegionDeviceId().equals(regionInDb.getCommonRegionDeviceId())) {
            // 节点国标编号改变，
            // 修改所有子区域的父节点编号
            regionMapper.updateChild(regionInDb.getCommonRegionDeviceId(), region.getCommonRegionDeviceId());
            // 修改所有所属的通道的编号
            commonGbChannelMapper.updateChanelRegion(regionInDb.getCommonRegionDeviceId(),
                    region.getCommonRegionDeviceId());
        }else if (region.getCommonRegionName().equals(regionInDb.getCommonRegionName()) &&
         region.getCommonRegionParentId().equals(regionInDb.getCommonRegionParentId())) {
            // 数据没有变化
            return;
        }
        regionMapper.update(region);

    }
}
