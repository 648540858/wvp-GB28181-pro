package com.genersoft.iot.vmp.gat1400.framework.domain.core;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class SimpleDataResponse<T> extends BaseResponse {

    private T data;

    public SimpleDataResponse() {}

    public SimpleDataResponse(T data) {
        this();
        this.data = data;
    }
}
