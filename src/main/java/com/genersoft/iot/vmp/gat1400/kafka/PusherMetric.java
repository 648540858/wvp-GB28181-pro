package com.genersoft.iot.vmp.gat1400.kafka;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PusherMetric {

    @ApiModelProperty("订阅标识符")
    private String subscribeId;
    @ApiModelProperty("消费主题")
    private Set<String> topics = new HashSet<>();
    @ApiModelProperty("最大数据量")
    private long maxOffset;
    @ApiModelProperty("消费数据量")
    private long curOffset;
    @ApiModelProperty("百分比进度")
    private double percentage;
    @ApiModelProperty("指标详情")
    private List<PusherMetric> children;

    public PusherMetric copyObject() {
        PusherMetric metric = new PusherMetric();
        metric.setSubscribeId(subscribeId);
        metric.setTopics(topics);
        metric.setMaxOffset(maxOffset);
        metric.setCurOffset(curOffset);
        if (children != null) {
            metric.setChildren(new ArrayList<>(children));
        }
        return metric;
    }

    public static PusherMetric create(String topic) {
        PusherMetric metric = new PusherMetric();
        metric.topics.add(topic);
        return metric;
    }

    public static PusherMetric sum(PusherMetric m1, PusherMetric m2) {
        m1.topics.addAll(m2.topics);
        m1.maxOffset = m1.maxOffset + m2.maxOffset;
        m1.curOffset = m1.curOffset + m2.curOffset;
        return m1;
    }

    public double getPercentage() {
        if (maxOffset == 0 || curOffset == 0)
            return 0.0;
        return BigDecimal.valueOf(curOffset)
                .divide(BigDecimal.valueOf(maxOffset), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
