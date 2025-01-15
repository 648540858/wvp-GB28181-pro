package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceMobilePositionMapper {

    int insertNewPosition(MobilePosition mobilePosition);

    List<MobilePosition> queryPositionByDeviceIdAndTime(@Param("deviceId") String deviceId, @Param("channelId") String channelId, @Param("startTime") String startTime, @Param("endTime") String endTime);

    MobilePosition queryLatestPositionByDevice(String deviceId);

    int clearMobilePositionsByDeviceId(String deviceId);

    void batchadd(List<MobilePosition> mobilePositions);

}
