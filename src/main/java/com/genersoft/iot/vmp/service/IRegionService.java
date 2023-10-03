package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.service.bean.Region;

import java.util.List;

public interface IRegionService {

    List<Region> getChildren(String parentDeviceId);

    void add(Region region);

    void deleteByDeviceId(String regionDeviceId);

    void updateRegionName(Region region);
}
