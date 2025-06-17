package com.genersoft.iot.vmp.gat1400.fontend.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.genersoft.iot.vmp.gat1400.fontend.domain.PublishQuery;
import com.genersoft.iot.vmp.gat1400.fontend.domain.TableDataResponse;
import com.genersoft.iot.vmp.gat1400.fontend.domain.VIIDPublishRequest;
import com.genersoft.iot.vmp.gat1400.fontend.domain.VIIDPublishVo;
import com.genersoft.iot.vmp.gat1400.framework.SpringContextHolder;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.BaseResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.SimpleDataResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;
import com.genersoft.iot.vmp.gat1400.framework.service.VIIDPublishService;
import com.genersoft.iot.vmp.gat1400.kafka.KafkaStartupService;
import com.genersoft.iot.vmp.gat1400.kafka.PusherMetric;
import com.genersoft.iot.vmp.gat1400.listener.event.VIIDPublishInactiveEvent;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"视图库推送"})
@RestController
public class VIIDPublishController {

    @Resource
    VIIDPublishService service;
    @Resource
    KafkaStartupService kafkaStartupService;

    @ApiOperation(value = "视图库推送-分页列表")
    @GetMapping("/api/viid/publish/page")
    public TableDataResponse<VIIDPublishVo> page(PublishQuery request) {
        Page<VIIDPublish> page = service.page(request);
        List<VIIDPublishVo> collect = new ArrayList<>();
        for (VIIDPublish record : page.getRecords()) {
            VIIDPublishVo vo = VIIDPublishVo.formList(record);
            if (Constants.SubscribeStatus.In.equalsValue(vo.getSubscribeStatus())) {
                String description = kafkaStartupService.description(record);
                vo.setDescription(description);
            } else if (Constants.SubscribeStatus.Expire.equalsValue(vo.getSubscribeStatus())) {
                vo.setDescription("订阅已过期");
            }
            Map<String, PusherMetric> metric = kafkaStartupService.metric(record);
            metric.values().stream().reduce(PusherMetric::sum).ifPresent(vo::setMetric);
            collect.add(vo);
        }
        return new TableDataResponse<>(collect, page.getTotal());
    }

    @ApiOperation(value = "视图库推送-详情")
    @GetMapping("/api/viid/publish/{id}")
    public SimpleDataResponse<VIIDPublishVo> getInfo(@PathVariable("id") String id) {
        return new SimpleDataResponse<>(VIIDPublishVo.formInfo(service.getById(id)));
    }

    @ApiOperation(value = "视图库推送-新增")
    @PostMapping("/api/viid/publish")
    public BaseResponse addPublish(@RequestBody VIIDPublishRequest request) {
        return BaseResponse.withBoolean(service.addPublish(request));
    }

    @ApiOperation(value = "视图库推送-更新")
    @PutMapping("/api/viid/publish")
    public BaseResponse refreshPublish(@RequestBody VIIDPublishRequest request) {
        return BaseResponse.withBoolean(service.refreshPublish(request));
    }

    @ApiOperation(value = "视图库推送-删除")
    @DeleteMapping("/api/viid/publish/{ids}")
    public BaseResponse delPublish(@PathVariable("ids") String[] ids) {
        List<String> idList = Arrays.asList(ids);
        idList.stream().map(service::getById)
                .filter(Objects::nonNull)
                .forEach(publish -> {
                    service.removeById(publish);
                    SpringContextHolder.publishEvent(new VIIDPublishInactiveEvent(publish));
                });
        return BaseResponse.success();
    }
}
