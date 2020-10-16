package com.genersoft.iot.vmp.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    private int page;
    private int count;
    private int total;

    private List<T> data;

    public List<T> getData() {
        return data;
    }

}
