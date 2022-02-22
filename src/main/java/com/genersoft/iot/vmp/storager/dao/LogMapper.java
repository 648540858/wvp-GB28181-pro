package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.storager.dao.dto.LogDto;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储设服务的日志
 */
@Mapper
@Repository
public interface LogMapper {

    @Insert("insert into log ( name, type, uri, address, result, timing, username, createTime) " +
            "values ('${name}', '${type}', '${uri}', '${address}', '${result}', ${timing}, '${username}', '${createTime}')")
    int add(LogDto logDto);

    @Select(value = {"<script>" +
            " SELECT * FROM log " +
            " WHERE 1=1 " +
            " <if test=\"query != null\"> AND (name LIKE '%${query}%')</if> " +
            " <if test=\"type != null\" >  AND type = '${type}'</if>" +
            " <if test=\"startTime != null\" >  AND createTime &gt;= '${startTime}' </if>" +
            " <if test=\"endTime != null\" >  AND createTime &lt;= '${endTime}' </if>" +
            " ORDER BY createTime DESC " +
            " </script>"})
    List<LogDto> query(String query, String type, String startTime, String endTime);

    @Delete("DELETE FROM log")
    int clear();
}
