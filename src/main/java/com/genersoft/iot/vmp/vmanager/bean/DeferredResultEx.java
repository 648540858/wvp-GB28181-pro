package com.genersoft.iot.vmp.vmanager.bean;

import org.springframework.web.context.request.async.DeferredResult;

public class DeferredResultEx<T> {

    private DeferredResult<T> deferredResult;

    private DeferredResultFilter filter;

    public DeferredResultEx(DeferredResult<T> result) {
        this.deferredResult = result;
    }


    public DeferredResult<T> getDeferredResult() {
        return deferredResult;
    }

    public void setDeferredResult(DeferredResult<T> deferredResult) {
        this.deferredResult = deferredResult;
    }

    public DeferredResultFilter getFilter() {
        return filter;
    }

    public void setFilter(DeferredResultFilter filter) {
        this.filter = filter;
    }
}
