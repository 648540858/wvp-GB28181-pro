package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.service.bean.RecordPlan;
import com.genersoft.iot.vmp.service.bean.RecordPlanItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RecordPlanMapper {

    @Insert(" <script>" +
            "INSERT INTO wvp_record_plan (" +
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
            "INSERT INTO wvp_record_plan_item (" +
            "start," +
            "stop, " +
            "week_day," +
            "plan_id) " +
            "VALUES" +
            "<foreach collection='planItemList' index='index' item='item' separator=','> " +
            "(#{item.start}, #{item.stop}, #{item.weekDay},#{planId})" +
            "</foreach> " +
            " </script>")
    void batchAddItem(@Param("planId") int planId, List<RecordPlanItem> planItemList);

    @Select("select * from wvp_record_plan where  id = #{planId}")
    RecordPlan get(@Param("planId") Integer planId);

    @Select(" <script>" +
            " SELECT wrp.*, (select count(1) from wvp_device_channel where record_plan_id = wrp.id) AS channelCount\n" +
            " FROM wvp_record_plan wrp where  1=1" +
            " <if test='query != null'> AND (name LIKE concat('%',#{query},'%') escape '/' )</if> " +
            " </script>")
    List<RecordPlan> query(@Param("query") String query);

    @Update("UPDATE wvp_record_plan SET update_time=#{updateTime}, name=#{name}, snap=#{snap} WHERE id=#{id}")
    void update(RecordPlan plan);

    @Delete("DELETE FROM wvp_record_plan WHERE id=#{planId}")
    void delete(@Param("planId") Integer planId);

    @Select("select * from wvp_record_plan_item where  plan_id = #{planId}")
    List<RecordPlanItem> getItemList(@Param("planId") Integer planId);

    @Delete("DELETE FROM wvp_record_plan_item WHERE plan_id = #{planId}")
    void cleanItems(@Param("planId") Integer planId);

    @Select("select plan_id from wvp_record_plan_item where  week_day = #{week} and start &gt;= #{index} and stop &lt;= #{index} group by plan_id")
    List<Integer> queryStart(@Param("week") int week, @Param("index") int index);
}
