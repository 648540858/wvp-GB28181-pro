package com.genersoft.iot.vmp.jt1078.dao;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import org.apache.ibatis.annotations.*;

import java.util.List;

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
            "<if test=\"authenticationCode != null\">, authentication_code=#{localIp}</if>" +
            "<if test=\"longitude != null\">, longitude=#{longitude}</if>" +
            "<if test=\"latitude != null\">, latitude=#{latitude}</if>" +
            "<if test=\"status != null\">, status=#{status}</if>" +
            "WHERE device_id=#{deviceId}"+
            " </script>"})
    void updateDevice(JTDevice device);
    @Select(value = {" <script>" +
            "SELECT * " +
            "from " +
            "wvp_jt_device jd " +
            "WHERE " +
            "1=1" +
            " <if test='query != null'> AND (" +
            "jd.province_id LIKE concat('%',#{query},'%') " +
            "OR jd.city_id LIKE concat('%',#{query},'%') " +
            "OR jd.maker_id LIKE concat('%',#{query},'%') " +
            "OR jd.device_model LIKE concat('%',#{query},'%') " +
            "OR jd.device_id LIKE concat('%',#{query},'%') " +
            "OR jd.plate_no LIKE concat('%',#{query},'%')" +
            ")</if> " +
            " <if test='online == true' > AND jd.status= true</if>" +
            " <if test='online == false' > AND jd.status= false</if>" +
            "ORDER BY jd.update_time " +
            " </script>"})
    List<JTDevice> getDeviceList(@Param("query") String query, @Param("online") Boolean online);

    @Insert("INSERT INTO wvp_jt_device (" +
            "province_id,"+
            "city_id,"+
            "maker_id,"+
            "device_id,"+
            "device_model,"+
            "plate_color,"+
            "plate_no,"+
            "authentication_code,"+
            "longitude,"+
            "latitude,"+
            "create_time,"+
            "update_time"+
            ") VALUES (" +
            "#{provinceId}," +
            "#{cityId}," +
            "#{makerId}," +
            "#{deviceId}," +
            "#{deviceModel}," +
            "#{plateColor}," +
            "#{plateNo}," +
            "#{authenticationCode}," +
            "#{longitude}," +
            "#{latitude}," +
            "#{createTime}," +
            "#{updateTime}" +
            ")")
    void addDevice(JTDevice device);

    @Delete("delete from wvp_jt_device where device_id = #{deviceId}")
    void deleteDeviceByDeviceId(@Param("deviceId") String deviceId);
}
