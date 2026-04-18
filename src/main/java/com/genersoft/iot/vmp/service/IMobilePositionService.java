package com.genersoft.iot.vmp.service;


import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.bean.Platform;

import java.util.List;

public interface IMobilePositionService {

    List<MobilePosition> queryMobilePositions(Integer channelId, String startTime, String endTime);

    List<Platform> queryEnablePlatformListWithAsMessageChannel();

    MobilePosition queryLatestPosition(Integer channelId);

}
