package com.genersoft.iot.vmp.common;

import java.util.ArrayList;
import java.util.List;

public class MyPageInfo<T> {
    //当前页
    private int pageNo;
    //每页的数量
    private int pageSize;
    //当前页的数量
    private int size;
    //总页数
    private int pages;
    //总数
    private int total;

    private List<T> resultData;

    private List<T> list;

    public MyPageInfo(List<T> resultData) {
        this.resultData = resultData;
    }

    public void startPage(int page, int count) {
        if (page <= 0) page = 1;
        this.pageNo = page;
        this.pageSize = count;
        this.total = resultData.size();

        this.pages = total%count == 0 ? total/count : total/count + 1;
        int fromIndx = (page - 1) * count;
        if ( fromIndx > this.total - 1) {
            this.list = new ArrayList<>();
            this.size = 0;
            return;
        }

        int toIndx = page * count;
        if (toIndx > this.total) {
            toIndx = this.total;
        }
        this.list = this.resultData.subList(fromIndx, toIndx);
        this.size = this.list.size();
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
