package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Region;
import com.genersoft.iot.vmp.gb28181.bean.RegionTree;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.RegionMapper;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.service.IRegionService;
import com.genersoft.iot.vmp.utils.CivilCodeUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.*;

/**
 * 区域管理类
 */
@Service
public class RegionServiceImpl implements IRegionService {


    private static final Logger log = LoggerFactory.getLogger(RegionServiceImpl.class);
    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;

    @Autowired
    private IGbChannelService gbChannelService;

    @Override
    public void add(Region region) {
        Assert.hasLength(region.getName(), "名称必须存在");
        Assert.hasLength(region.getDeviceId(), "国标编号必须存在");
        if (ObjectUtils.isEmpty(region.getParentDeviceId().trim())) {
            region.setParentDeviceId(null);
        }
        region.setCreateTime(DateUtil.getNow());
        region.setUpdateTime(DateUtil.getNow());
        try {
            regionMapper.add(region);
        }catch (DuplicateKeyException e){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "此行政区划已存在");
        }

    }

    @Override
    @Transactional
    public boolean deleteByDeviceId(String regionDeviceId) {
        Region region = regionMapper.queryOneByDeviceId(regionDeviceId);
        // 获取所有子节点
        List<Region> allChildren = getAllChildren(regionDeviceId);
        allChildren.add(region);
        // 设置使用这些节点的通道的civilCode为null,
        gbChannelService.removeCivilCode(allChildren);
        regionMapper.batchDelete(allChildren);
        return true;
    }

    private List<Region> getAllChildren(String deviceId) {
        if (deviceId == null || deviceId.length() >= 8) {
            return new ArrayList<>();
        }
        List<Region> children = regionMapper.getChildren(deviceId);
        if (ObjectUtils.isEmpty(children)) {
            return children;
        }
        List<Region> regions = new ArrayList<>(children);
        for (Region region : children) {
            if (region.getDeviceId().length() < 8) {
                regions.addAll(getAllChildren(region.getDeviceId()));
            }
        }
        return regions;
    }

    @Override
    public PageInfo<Region> query(String query, int page, int count) {
        PageHelper.startPage(page, count);
        List<Region> regionList =  regionMapper.query(query, null);
        return new PageInfo<>(regionList);
    }

    @Override
    public PageInfo<Region> queryChildRegionList(String regionParentId, int page, int count) {
        Assert.hasLength(regionParentId, "上级行政区划编号必须存在");
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
        List<Region> allChild = CivilCodeUtil.INSTANCE.getAllChild(parent);
        Collections.sort(allChild);
        return allChild;
    }

    @Override
    public Region queryRegionByDeviceId(String regionDeviceId) {
        return null;
    }

    @Override
    public List<RegionTree> queryForTree(String query, String parent) {
        List<RegionTree> regionList = regionMapper.queryForTree(query, parent);
        if (parent != null) {
            List<RegionTree> channelList = commonGBChannelMapper.queryForRegionTreeByCivilCode(query, parent);
            regionList.addAll(channelList);
        }
        return regionList;
    }

    @Override
    public void syncFromChannel() {
        // 获取未初始化的行政区划节点
        List<String> civilCodeList = regionMapper.getUninitializedCivilCode();
        if (civilCodeList.isEmpty()) {
            return;
        }
        List<Region> regionList = new ArrayList<>();
        // 收集节点的父节点,用于验证哪些节点的父节点不存在,方便一并存入
        Map<String, Region> regionMapForVerification = new HashMap<>();
        civilCodeList.forEach(civilCode->{
            CivilCodePo civilCodePo = CivilCodeUtil.INSTANCE.getCivilCodePo(civilCode);
            if (civilCodePo != null) {
                Region region = Region.getInstance(civilCodePo);
                regionList.add(region);
                // 获取全部的父节点
                List<CivilCodePo> civilCodePoList = CivilCodeUtil.INSTANCE.getAllParentCode(civilCode);
                if (!civilCodePoList.isEmpty()) {
                    for (CivilCodePo codePo : civilCodePoList) {
                        regionMapForVerification.put(codePo.getCode(), Region.getInstance(codePo));
                    }
                }
            }
        });
        if (regionList.isEmpty()){
            return;
        }
        if (!regionMapForVerification.isEmpty()) {
            // 查询数据库中已经存在的.
            List<String> civilCodesInDb = regionMapper.queryInList(regionMapForVerification.keySet());
            if (!civilCodesInDb.isEmpty()) {
                for (String code : civilCodesInDb) {
                    regionMapForVerification.remove(code);
                }
            }
        }
        for (Region region : regionList) {
            regionMapForVerification.put(region.getDeviceId(), region);
        }

        regionMapper.batchAdd(new ArrayList<>(regionMapForVerification.values()));
    }

    @Override
    public boolean delete(int id) {
        return regionMapper.delete(id) > 0;
    }

    @Override
    public boolean batchAdd(List<Region> regionList) {
        if (regionList== null || regionList.isEmpty()) {
            return false;
        }
        Map<String, Region> regionMapForVerification = new HashMap<>();
        for (Region region : regionList) {
            regionMapForVerification.put(region.getDeviceId(), region);
        }
        // 查询数据库中已经存在的.
        List<Region> regionListInDb = regionMapper.queryInRegionList(regionList);
        if (!regionListInDb.isEmpty()) {
            for (Region region : regionListInDb) {
                regionMapForVerification.remove(region.getDeviceId());
            }
        }
        regionMapper.batchAdd(new ArrayList<>(regionMapForVerification.values()));
        return false;
    }
}
