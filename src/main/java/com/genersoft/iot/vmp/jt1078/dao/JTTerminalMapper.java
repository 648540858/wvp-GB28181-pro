package com.genersoft.iot.vmp.jt1078.dao;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface JTTerminalMapper {

    @Select("SELECT * FROM wvp_jt_terminal where phone_number=#{phoneNumber}")
    JTDevice getDevice(@Param("phoneNumber") String phoneNumber);

    @Update(value = {" <script>" +
            "UPDATE wvp_jt_terminal " +
            "SET update_time=#{updateTime}" +
            "<if test=\"terminalId != null\">, terminal_id=#{terminalId}</if>" +
            "<if test=\"provinceId != null\">, province_id=#{provinceId}</if>" +
            "<if test=\"provinceText != null\">, province_text=#{provinceText}</if>" +
            "<if test=\"cityId != null\">, city_id=#{cityId}</if>" +
            "<if test=\"cityText != null\">, city_text=#{cityText}</if>" +
            "<if test=\"makerId != null\">, maker_id=#{makerId}</if>" +
            "<if test=\"deviceModel != null\">, device_model=#{deviceModel}</if>" +
            "<if test=\"plateColor != null\">, plate_color=#{plateColor}</if>" +
            "<if test=\"plateNo != null\">, plate_no=#{plateNo}</if>" +
            "<if test=\"authenticationCode != null\">, authentication_code=#{authenticationCode}</if>" +
            "<if test=\"longitude != null\">, longitude=#{longitude}</if>" +
            "<if test=\"latitude != null\">, latitude=#{latitude}</if>" +
            "<if test=\"status != null\">, status=#{status}</if>" +
            "WHERE phone_number=#{phoneNumber}"+
            " </script>"})
    void updateDevice(JTDevice device);
    @Select(value = {" <script>" +
            "SELECT * " +
            "from " +
            "wvp_jt_terminal jd " +
            "WHERE " +
            "1=1" +
            " <if test='query != null'> AND (" +
            "jd.phone_number LIKE concat('%',#{query},'%') " +
            "jd.province_id LIKE concat('%',#{query},'%') " +
            "OR jd.city_id LIKE concat('%',#{query},'%') " +
            "OR jd.maker_id LIKE concat('%',#{query},'%') " +
            "OR jd.device_model LIKE concat('%',#{query},'%') " +
            "OR jd.terminal_id LIKE concat('%',#{query},'%') " +
            "OR jd.plate_no LIKE concat('%',#{query},'%')" +
            ")</if> " +
            " <if test='online == true' > AND jd.status= true</if>" +
            " <if test='online == false' > AND jd.status= false</if>" +
            "ORDER BY jd.update_time " +
            " </script>"})
    List<JTDevice> getDeviceList(@Param("query") String query, @Param("online") Boolean online);

    @Insert("INSERT INTO wvp_jt_terminal (" +
            "phone_number,"+
            "province_id,"+
            "province_text,"+
            "city_id,"+
            "city_text,"+
            "maker_id,"+
            "terminal_id,"+
            "device_model,"+
            "plate_color,"+
            "plate_no,"+
            "authentication_code,"+
            "longitude,"+
            "latitude,"+
            "create_time,"+
            "update_time"+
            ") VALUES (" +
            "#{phoneNumber}," +
            "#{provinceId}," +
            "#{provinceText}," +
            "#{cityId}," +
            "#{cityText}," +
            "#{makerId}," +
            "#{terminalId}," +
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

    @Delete("delete from wvp_jt_terminal where phone_number = #{phoneNumber}")
    void deleteDeviceByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Update(value = {" <script>" +
            "UPDATE wvp_jt_terminal " +
            "SET status=#{connected} " +
            "WHERE phone_number=#{phoneNumber}"+
            " </script>"})
    void updateDeviceStatus(@Param("connected") boolean connected, @Param("phoneNumber") String phoneNumber);
}
