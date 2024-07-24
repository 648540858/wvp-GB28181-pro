package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.conf.CivilCodeFileConf;
import com.genersoft.iot.vmp.gb28181.bean.Region;
import com.genersoft.iot.vmp.gb28181.dao.RegionMapper;
import com.genersoft.iot.vmp.gb28181.service.IRegionService;
import com.genersoft.iot.vmp.utils.CivilCodeUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;

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
        List<Region> allChild = CivilCodeUtil.INSTANCE.getAllChild(parent);
        Collections.sort(allChild);
        return allChild;
    }

    @Override
    public Region queryRegionByDeviceId(String regionDeviceId) {
        return null;
    }

    @Override
    public List<Region> queryForTree(String query, String parent) {
        return regionMapper.queryForTree(query, parent);
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
}
