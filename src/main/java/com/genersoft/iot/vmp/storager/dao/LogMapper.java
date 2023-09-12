package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.storager.dao.dto.LogDto;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储设服务的日志
 */
@Mapper
@Repository
public interface LogMapper {

    @Insert("insert into wvp_log ( name,type,uri,address,result,timing,username,create_time) " +
            "values (#{name}, #{type}, #{uri}, #{address}, #{result}, #{timing}, #{username}, #{createTime})")
    int add(LogDto logDto);

    @Select(value = {"<script>" +
            " SELECT * FROM wvp_log " +
            " WHERE 1=1 " +
            " <if test=\"query != null\"> AND (name LIKE concat('%',#{query},'%'))</if> " +
            " <if test=\"type != null\" >  AND type = #{type}</if>" +
            " <if test=\"startTime != null\" >  AND create_time &gt;= #{startTime} </if>" +
            " <if test=\"endTime != null\" >  AND create_time &lt;= #{endTime} </if>" +
            " ORDER BY create_time DESC " +
            " </script>"})
    List<LogDto> query(@Param("query") String query, @Param("type") String type, @Param("startTime") String startTime, @Param("endTime") String endTime);

    @Delete("DELETE FROM wvp_log")
    int clear();
}
