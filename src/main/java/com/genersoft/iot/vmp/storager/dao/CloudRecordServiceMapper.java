package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CloudRecordServiceMapper {

    @Insert(" <script>" +
            "INSERT INTO wvp_cloud_record (" +
            " app," +
            " stream," +
            "<if test=\"callId != null\"> call_id,</if>" +
            " start_time," +
            " end_time," +
            " media_server_id," +
            " file_name," +
            " folder," +
            " file_path," +
            " file_size," +
            " time_len ) " +
            "VALUES (" +
            " #{app}," +
            " #{stream}," +
            " <if test=\"callId != null\"> #{callId},</if>" +
            " #{startTime}," +
            " #{endTime}," +
            " #{mediaServerId}," +
            " #{fileName}," +
            " #{folder}," +
            " #{filePath}," +
            " #{fileSize}," +
            " #{timeLen})" +
            " </script>")
    int add(CloudRecordItem cloudRecordItem);

    @Select(" <script>" +
            "select * " +
            "from wvp_cloud_record " +
            "where 0 = 0" +
            " <if test= 'app != null '> and app=#{app}</if>" +
            " <if test= 'stream != null '> and stream=#{stream}</if>" +
            " <if test= 'startTimeStamp != null '> and start_time &gt;= #{startTimeStamp}</if>" +
            " <if test= 'endTimeStamp != null '> and end_time &lt;= #{endTimeStamp}</if>" +
            " <if test= 'mediaServerItemList != null  ' > and media_server_id in " +
            " <foreach collection='mediaServerItemList'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </if>" +
            " </script>")
    List<CloudRecordItem> getList(@Param("app") String app, @Param("stream") String stream,
                                  @Param("startTimeStamp")Long startTimeStamp, @Param("endTimeStamp")Long endTimeStamp,
                                  List<MediaServerItem> mediaServerItemList);
}
