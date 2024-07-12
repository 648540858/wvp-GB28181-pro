package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.Region;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RegionMapper {
    void add(Region region);

    List<Region> query(String query);

    List<Region> getChildren(String regionParentId);

    Region queryRegion(int id);
}
