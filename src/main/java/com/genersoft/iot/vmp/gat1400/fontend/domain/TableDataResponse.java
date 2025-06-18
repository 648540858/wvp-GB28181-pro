package com.genersoft.iot.vmp.gat1400.fontend.domain;


import com.genersoft.iot.vmp.gat1400.framework.domain.core.BaseResponse;

import java.util.List;
import java.util.Optional;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class TableDataResponse<T> extends BaseResponse {

    private List<T> data;
    private Integer total;

    public TableDataResponse() {
    }

    public TableDataResponse(List<T> data) {
        this(data, data.size());
    }

    public TableDataResponse(List<T> data, Long total) {
        this(data, Optional.ofNullable(total).map(Long::intValue).orElse(data.size()));
    }

    public TableDataResponse(List<T> data, int total) {
        this.data = data;
        this.total = total;
    }
}
