package com.genersoft.iot.vmp.gat1400.fontend.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.genersoft.iot.vmp.gat1400.fontend.domain.DataSelectOption;
import com.genersoft.iot.vmp.gat1400.fontend.domain.SubscribeQuery;
import com.genersoft.iot.vmp.gat1400.fontend.domain.VIIDSubscribeRequest;
import com.genersoft.iot.vmp.gat1400.fontend.domain.VIIDSubscribeVo;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.SearchDataResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.SimpleDataResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDSubscribe;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.VIIDResponseStatusObject;
import com.genersoft.iot.vmp.gat1400.framework.service.VIIDSubscribeService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import static com.genersoft.iot.vmp.gat1400.framework.config.Constants.SubscribeDetail.DEVICE;
import static com.genersoft.iot.vmp.gat1400.framework.config.Constants.SubscribeDetail.FACE_INFO;
import static com.genersoft.iot.vmp.gat1400.framework.config.Constants.SubscribeDetail.PERSON_INFO;
import static com.genersoft.iot.vmp.gat1400.framework.config.Constants.SubscribeDetail.PLATE_INFO;
import static com.genersoft.iot.vmp.gat1400.framework.config.Constants.SubscribeDetail.PLATE_MIRCO_INFO;
import static com.genersoft.iot.vmp.gat1400.framework.config.Constants.SubscribeDetail.RAW;
import static com.genersoft.iot.vmp.gat1400.framework.config.Constants.SubscribeDetail.TOLLGATE;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusListObject;

@Api(tags = {"视图库订阅"})
@RestController
public class VIIDSubscribeController {

    @Resource
    VIIDSubscribeService service;

    @ApiOperation(value = "视图库订阅-分页列表")
    @GetMapping("/api/viid/subscribe/page")
    public SearchDataResponse<VIIDSubscribeVo> list(SubscribeQuery request) {
        Page<VIIDSubscribe> page = service.list(request);
        List<VIIDSubscribeVo> collect = page.getRecords().stream().map(VIIDSubscribeVo::formList).collect(Collectors.toList());
        return new SearchDataResponse<>(collect, page.getTotal());
    }

    @ApiOperation(value = "视图库订阅-详情")
    @GetMapping("/api/viid/subscribe/{id}")
    public SimpleDataResponse<VIIDSubscribeVo> getInfo(@PathVariable("id") String id) {
        return new SimpleDataResponse<>(VIIDSubscribeVo.formInfo(service.getById(id)));
    }

    @ApiOperation(value = "视图库订阅-下拉框选项")
    @GetMapping("/api/viid/subscribe/detail/options")
    public SearchDataResponse<DataSelectOption> subscribeDetailOptions() {
        List<DataSelectOption> options = Arrays.stream(Constants.SubscribeDetail.values())
                .map(ele -> DataSelectOption.from(ele.getValue(), ele.getDescribe()))
                .collect(Collectors.toList());
        return new SearchDataResponse<>(options);
    }

    @ApiOperation(value = "视图库订阅-新增")
    @PostMapping("/api/viid/subscribe")
    public VIIDResponseStatusObject subscribe(@RequestBody VIIDSubscribeRequest request) {
        VIIDResponseStatusObject response = new VIIDResponseStatusObject();
        Constants.SubscribeDetail detail = Constants.SubscribeDetail.match(request.getSubscribeDetail());
        if (Objects.isNull(detail)) {
            return VIIDResponseStatusObject.from(new ResponseStatusObject(null, null, "1", "无法识别的订阅类别"));
        }
        switch (detail) {
            case DEVICE:
            case TOLLGATE:
            case PERSON_INFO:
            case FACE_INFO:
            case PLATE_INFO:
            case PLATE_MIRCO_INFO:
            case RAW:
                request.setResourceClass(Constants.ResourceClass.Instance.getValue());
                response = service.subscribes(request);
                break;
            default:
                break;
        }
        return response;
    }

    @ApiOperation(value = "视图库订阅-单个删除")
    @DeleteMapping("/api/viid/subscribe/cancel")
    public VIIDResponseStatusObject subscribesCancel(@RequestBody VIIDSubscribeRequest request) {
        return service.unSubscribes(request.getSubscribeId());
    }

    @ApiOperation(value = "视图库订阅-批量删除")
    @DeleteMapping("/api/viid/subscribe/{id}")
    public VIIDResponseStatusObject removeSubscribe(@PathVariable("id") String[] ids) {
        List<ResponseStatusObject> statusObjectList = Stream.of(ids)
                .map(service::unSubscribes)
                .map(VIIDResponseStatusObject::getResponseStatusListObject)
                .map(ResponseStatusListObject::getResponseStatusObject)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return VIIDResponseStatusObject.from(null, statusObjectList);
    }
}
