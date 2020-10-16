package com.genersoft.iot.vmp.common;


import java.util.List;

public class PageResult<T> {

    private int page;
    private int count;
    private int total;

    private List<T> data;

    public List<T> getData() {
        return data;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
