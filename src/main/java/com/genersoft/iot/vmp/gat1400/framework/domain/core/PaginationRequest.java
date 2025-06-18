package com.genersoft.iot.vmp.gat1400.framework.domain.core;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.swagger.annotations.ApiModelProperty;

public class PaginationRequest {

    @ApiModelProperty(value = "分页-页数", required = true)
    private Integer pageNum;
    @ApiModelProperty(value = "分页-页大小", required = true)
    private Integer pageSize;

    public <T> Page<T> pageable() {
        return new Page<>(getPageNum() == null ? 1 : getPageNum(),
                getPageSize() == null ? 20: getPageSize());
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
