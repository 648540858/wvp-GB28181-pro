package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.service.bean.RecordPlan;
import com.genersoft.iot.vmp.service.bean.RecordPlanItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

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

    RecordPlan get(Integer planId);

    List<RecordPlan> query(String query);

    void update(RecordPlan plan);

    void delete(Integer planId);


    void batchAddItem(int planId, List<RecordPlanItem> planItemList);

}
