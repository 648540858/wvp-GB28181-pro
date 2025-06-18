package com.genersoft.iot.vmp.gat1400.framework.mapper;


import com.alibaba.fastjson2.JSONObject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Mapper
public interface MetricsMapper {

    List<JSONObject> histogramOfHour(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    void releaseFace(@Param("day") int day);

    void releasePerson(@Param("day") int day);

    void releaseVehicle(@Param("day") int day);

    void releaseNonVehicle(@Param("day") int day);

    default Pair<List<String>, List<Long>> toDayHistogramOfHour() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.plusDays(-1);
        List<JSONObject> histogram = histogramOfHour(startTime, endTime);
        List<String> hour = new ArrayList<>();
        List<Long> value = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hour.add(String.valueOf(i));
            for (JSONObject data : histogram) {
                if (data.getIntValue("hour") == i) {
                    value.add(data.getLongValue("count"));
                    break;
                }
            }
            if (value.size() <= i) {
                value.add(0L);
            }
        }
        return Pair.of(hour, value);
    }
}
