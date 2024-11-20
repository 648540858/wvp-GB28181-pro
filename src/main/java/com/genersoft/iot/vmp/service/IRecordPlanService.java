package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.service.bean.RecordPlan;
import com.genersoft.iot.vmp.service.bean.RecordPlanItem;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface IRecordPlanService {


    RecordPlan get(Integer planId);

    void update(RecordPlan plan);

    void delete(Integer planId);

    PageInfo<RecordPlan> query(Integer page, Integer count, String query);

    void add(RecordPlan plan);

    void linke(List<Integer> channelIds, Integer planId);
}
