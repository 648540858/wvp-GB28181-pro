package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface VideoSquareMapper {

    @Select("select * FROM device")
    List<Device> selectDevices();

    @Select("select * from device_channel")
    List<DeviceChannel> selectDeviceChannels();
}
