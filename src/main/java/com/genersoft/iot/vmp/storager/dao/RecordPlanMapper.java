package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.service.bean.RecordPlan;
import com.genersoft.iot.vmp.service.bean.RecordPlanItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RecordPlanMapper {

    @Insert(" <script>" +
            "INSERT INTO wvp_cloud_record (" +
            " name," +
            " snap," +
            " create_time," +
            " update_time) " +
            "VALUES (" +
            " #{name}," +
            " #{snap}," +
            " #{createTime}," +
            " #{updateTime})" +
            " </script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void add(RecordPlan plan);

    @Insert(" <script>" +
            "INSERT INTO wvp_device_channel (" +
            "start_time," +
            "stop_time, " +
            "week_day," +
            "create_time," +
            "update_time) " +
            "VALUES" +
            "<foreach collection='commonGBChannels' index='index' item='item' separator=','> " +
            "(#{item.startTime}, #{item.stopTime}, #{item.weekDay},#{item.planId},#{item.createTime},#{item.updateTime})" +
            "</foreach> " +
            " </script>")
    void batchAddItem(@Param("planId") int planId, List<RecordPlanItem> planItemList);

    @Select("select * from wvp_record_plan where  id = #{planId}")
    RecordPlan get(@Param("planId") Integer planId);

    @Select(" <script>" +
            "select * from wvp_record_plan where  1=1" +
            " <if test='query != null'> AND (name LIKE concat('%',#{query},'%') escape '/' )</if> " +
            " </script>")
    List<RecordPlan> query(@Param("query") String query);

    @Update("UPDATE wvp_record_plan SET update_time=#{updateTime}, name=#{name}, snap=#{snap} WHERE id=#{id}")
    void update(RecordPlan plan);

    @Delete("DELETE FROM wvp_record_plan WHERE id=#{id}")
    void delete(@Param("planId") Integer planId);


    List<RecordPlanItem> getItemList(Integer planId);
}
