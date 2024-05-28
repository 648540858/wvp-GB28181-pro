package com.genersoft.iot.vmp.service;


import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;

import java.util.List;

public interface IMobilePositionService {

    void add(List<MobilePosition> mobilePositionList);

    void add(MobilePosition mobilePosition);
}
