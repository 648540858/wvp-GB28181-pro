package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.BatchLimit;
import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.conf.CivilCodeFileConf;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
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
import org.springframework.util.ObjectUtils;

import java.util.Collection;
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
    private CommonGbChannelMapper commonGbChannelMapper;


    @Autowired
    private CivilCodeFileConf civilCodeFileConf;

    @Override
    public void add(Region region) {
        assert region.getCommonRegionName() != null;
        assert region.getCommonRegionDeviceId() != null;
        if (ObjectUtils.isEmpty(region.getCommonRegionParentId().trim())) {
            region.setCommonRegionParentId(null);
        }
        region.setCommonRegionCreateTime(DateUtil.getNow());
        region.setCommonRegionUpdateTime(DateUtil.getNow());
        regionMapper.add(region);
    }

    @Override
    @Transactional
    public boolean deleteByDeviceId(String regionDeviceId) {
        // 查询所有从属的地区，从属地区的编号一定是父节点编号开头的，基于这个获取所有的子节点
        List<Region> regionList =  regionMapper.queryAllChildByDeviceId(regionDeviceId);

        if (regionList.size() > BatchLimit.count) {
            for (int i = 0; i < regionList.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > regionList.size()) {
                    toIndex = regionList.size();
                }
                List<Region> subList = regionList.subList(i, toIndex);
                // 移除所有关联当前节点和子节点的通道
                commonGbChannelMapper.removeRegionInfo(subList);
                // 移除所有节点
                regionMapper.removeRegionByList(subList);
            }
        }else {
            // 移除所有关联当前节点和子节点的通道
            commonGbChannelMapper.removeRegionInfo(regionList);
            // 移除所有节点
            regionMapper.removeRegionByList(regionList);
        }
        return true;
    }

    @Override
    public PageInfo<Region> query(String query, int page, int count) {
        PageHelper.startPage(page, count);
        List<Region> regionList =  regionMapper.query(query);
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
        }else if (
                ((regionInDb.getCommonRegionParentId() == null && region.getCommonRegionParentId() == null)
                        || regionInDb.getCommonRegionParentId().equals(region.getCommonRegionParentId()))
                        && regionInDb.getCommonRegionName().equals(region.getCommonRegionName())) {
            // 数据没有变化
            return;
        }
        regionMapper.update(region);

    }

    @Override
    public List<Region> getAllChild(String parent) {
        List<Region> allChild = civilCodeFileConf.getAllChild(parent);
        Collections.sort(allChild);
        return allChild;
    }

    @Override
    public Region queryRegionByDeviceId(String regionDeviceId) {
        return regionMapper.queryRegionByDeviceId(regionDeviceId);
    }
}
