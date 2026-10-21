package com.genersoft.iot.vmp.service;


import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;

import java.util.List;

public interface IMobilePositionService {

    List<MobilePosition> queryMobilePositions(Integer channelId, String startTime, String endTime);

    MobilePosition queryLatestPosition(Integer channelId);

}
