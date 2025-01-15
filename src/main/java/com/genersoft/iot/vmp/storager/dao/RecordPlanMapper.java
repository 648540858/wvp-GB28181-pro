package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.service.bean.RecordPlan;
import com.genersoft.iot.vmp.service.bean.RecordPlanItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecordPlanMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void add(RecordPlan plan);

    void batchAddItem(@Param("planId") int planId, List<RecordPlanItem> planItemList);

    RecordPlan get(@Param("planId") Integer planId);

    List<RecordPlan> query(@Param("query") String query);

    void update(RecordPlan plan);

    void delete(@Param("planId") Integer planId);

    List<RecordPlanItem> getItemList(@Param("planId") Integer planId);

    void cleanItems(@Param("planId") Integer planId);

    List<Integer> queryRecordIng(@Param("week") int week, @Param("index") int index);
}
