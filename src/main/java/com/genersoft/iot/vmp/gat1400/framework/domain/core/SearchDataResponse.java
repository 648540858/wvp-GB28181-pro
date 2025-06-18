package com.genersoft.iot.vmp.gat1400.framework.domain.core;

import java.util.List;
import java.util.Objects;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchDataResponse<T> extends BaseResponse {

    private List<T> data;
    private Integer total;

    public SearchDataResponse() {}

    public SearchDataResponse(List<T> data) {
        this.data = data;
        this.total = data.size();
    }

    public SearchDataResponse(List<T> data, Long total) {
        this.data = data;
        this.total = Objects.isNull(total) ? 0 : total.intValue();
    }
}
