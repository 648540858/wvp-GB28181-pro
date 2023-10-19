package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import org.apache.ibatis.annotations.*;

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
            " from wvp_cloud_record " +
            " where 0 = 0" +
            " <if test='query != null'> AND (app LIKE concat('%',#{query},'%') OR stream LIKE concat('%',#{query},'%') )</if> " +
            " <if test= 'app != null '> and app=#{app}</if>" +
            " <if test= 'stream != null '> and stream=#{stream}</if>" +
            " <if test= 'startTimeStamp != null '> and end_time &gt;= #{startTimeStamp}</if>" +
            " <if test= 'endTimeStamp != null '> and start_time &lt;= #{endTimeStamp}</if>" +
            " <if test= 'callId != null '> and call_id = #{callId}</if>" +
            " <if test= 'mediaServerItemList != null  ' > and media_server_id in " +
            " <foreach collection='mediaServerItemList'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </if>" +
            " order by start_time DESC" +

            " </script>")
    List<CloudRecordItem> getList(@Param("query") String query, @Param("app") String app, @Param("stream") String stream,
                                  @Param("startTimeStamp")Long startTimeStamp, @Param("endTimeStamp")Long endTimeStamp,
                                  @Param("callId")String callId, List<MediaServerItem> mediaServerItemList);


    @Select(" <script>" +
            "select file_path" +
            " from wvp_cloud_record " +
            " where 0 = 0" +
            " <if test= 'app != null '> and app=#{app}</if>" +
            " <if test= 'stream != null '> and stream=#{stream}</if>" +
            " <if test= 'startTimeStamp != null '> and end_time &gt;= #{startTimeStamp}</if>" +
            " <if test= 'endTimeStamp != null '> and start_time &lt;= #{endTimeStamp}</if>" +
            " <if test= 'callId != null '> and call_id = #{callId}</if>" +
            " <if test= 'mediaServerItemList != null  ' > and media_server_id in " +
            " <foreach collection='mediaServerItemList'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </if>" +
            " </script>")
    List<String> queryRecordFilePathList(@Param("app") String app, @Param("stream") String stream,
                                  @Param("startTimeStamp")Long startTimeStamp, @Param("endTimeStamp")Long endTimeStamp,
                                  @Param("callId")String callId, List<MediaServerItem> mediaServerItemList);

    @Update(" <script>" +
            "update wvp_cloud_record set collect = #{collect} where file_path in " +
            " <foreach collection='cloudRecordItemList'  item='item'  open='(' separator=',' close=')' > #{item.filePath}</foreach>" +
            " </script>")
    void updateCollectList(@Param("collect") boolean collect, List<CloudRecordItem> cloudRecordItemList);

    @Delete(" <script>" +
            "delete from wvp_cloud_record where media_server_id=#{mediaServerId} file_path in " +
            " <foreach collection='filePathList'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            " </script>")
    void deleteByFileList(List<String> filePathList, @Param("mediaServerId") String mediaServerId);


    @Select(" <script>" +
            "select file_path" +
            " from wvp_cloud_record " +
            " where collect = false and reserve = false " +
            " <if test= 'endTimeStamp != null '> and start_time &lt;= #{endTimeStamp}</if>" +
            " <if test= 'callId != null '> and call_id = #{callId}</if>" +
            " <if test= 'mediaServerId != null  ' > and media_server_id  = #{mediaServerId} </if>" +
            " </script>")
    List<String> queryRecordFilePathListForDelete(@Param("endTimeStamp")Long endTimeStamp, String mediaServerId);

    @Update(" <script>" +
            "update wvp_cloud_record set reserve = #{reserve} where file_path in " +
            " <foreach collection='cloudRecordItems'  item='item'  open='(' separator=',' close=')' > #{item.filePath}</foreach>" +
            " </script>")
    void updateReserveList(@Param("reserve") boolean reserve,List<CloudRecordItem> cloudRecordItems);

    @Update(" <script>" +
            "update wvp_cloud_record set collect = #{collect} where id = #{recordId} " +
            " </script>")
    void changeCollectById(@Param("collect") boolean collect, @Param("recordId") Integer recordId);

    @Update(" <script>" +
            "update wvp_cloud_record set reserve = #{reserve} where id = #{recordId} " +
            " </script>")
    void changeReserveById(@Param("reserve") boolean reserve, Integer recordId);
}
