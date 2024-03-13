package com.genersoft.iot.vmp.jt1078.dao;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import org.apache.ibatis.annotations.*;

@Mapper
public interface JTDeviceMapper {

    @Select("SELECT * FROM wvp_device de where device_id=${devId}")
    JTDevice getDevice(@Param("devId") String devId);

    @Update(value = {" <script>" +
            "UPDATE wvp_jt_device " +
            "SET update_time=#{updateTime}" +
            "<if test=\"provinceId != null\">, province_id=#{provinceId}</if>" +
            "<if test=\"cityId != null\">, city_id=#{cityId}</if>" +
            "<if test=\"makerId != null\">, maker_id=#{makerId}</if>" +
            "<if test=\"deviceModel != null\">, device_model=#{deviceModel}</if>" +
            "<if test=\"plateColor != null\">, plate_color=#{plateColor}</if>" +
            "<if test=\"plateNo != null\">, plate_no=#{plateNo}</if>" +
            "<if test=\"authenticationCode != null\">, authenticationCode=#{localIp}</if>" +
            "<if test=\"longitude != null\">, longitude=#{longitude}</if>" +
            "<if test=\"latitude != null\">, latitude=#{latitude}</if>" +
            "<if test=\"status != null\">, status=#{status}</if>" +
            "WHERE device_id=#{deviceId}"+
            " </script>"})
    void updateDevice(JTDevice device);
}
