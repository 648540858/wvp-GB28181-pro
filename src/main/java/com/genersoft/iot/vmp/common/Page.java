package com.genersoft.iot.vmp.common;

import com.github.pagehelper.PageInfo;

import java.util.List;

public class Page<T> {

    private Integer pageSize;

    private Integer pageNo;

    private Integer totalPage;

    private Long totalCount;

    private List<T> data;

    public Page() {
    }

    public Page(Integer pageSize, Integer pageNo, Integer totalPage, Long totalCount, List<T> data) {
        this.pageSize = pageSize;
        this.pageNo = pageNo;
        this.totalPage = totalPage;
        this.totalCount = totalCount;
        this.data = data;
    }

    public Page(PageInfo<T> pageInfo) {
        Integer pageNo = pageInfo.getPageNum();
        Integer pageSize = pageInfo.getPageSize();
        Integer totalPage = pageInfo.getPages();
        Long totalCount = pageInfo.getTotal();
        List<T> data = pageInfo.getList();
        setPageNo(pageNo);
        setPageSize(pageSize);
        setTotalPage(totalPage);
        setTotalCount(totalCount);
        setData(data);
    }

    public Page(MyPageInfo<T> myPageInfo){
        Integer pageNo = myPageInfo.getPageNo();
        Integer pageSize = myPageInfo.getPageSize();
        Integer totalPage = myPageInfo.getPages();
        int totalCount = myPageInfo.getTotal();
        List<T> data = myPageInfo.getList();
        setPageNo(pageNo);
        setPageSize(pageSize);
        setTotalPage(totalPage);
        setTotalCount((long) totalCount);
        setData(data);
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
