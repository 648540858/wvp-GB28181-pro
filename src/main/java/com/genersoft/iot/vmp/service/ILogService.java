package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.storager.dao.dto.LogDto;
import com.github.pagehelper.PageInfo;

/**
 * 系统日志
 */
public interface ILogService {

    /**
     * 查询日志
     * @param page 当前页
     * @param count 每页数量
     * @param query 搜索内容
     * @param type 类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    PageInfo<LogDto> getAll(int page, int count, String query, String type, String startTime, String endTime);

    /**
     * 添加日志
     * @param logDto 日志
     */
    void add(LogDto logDto);

    /**
     * 清空
     */
    int clear();

}
