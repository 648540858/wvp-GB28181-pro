package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.vmanager.bean.MapConfig;
import com.genersoft.iot.vmp.vmanager.bean.MapModelIcon;

import java.util.List;

public interface IMapService {

    List<MapConfig> getConfig();

    List<MapModelIcon> getModelList();
}
