package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 云端录像管理
 * @author lin
 */
public interface ICloudRecordService {

    /**
     * 分页回去云端录像列表
     */
    PageInfo<CloudRecordItem> getList(int page, int count, String startTime, String endTime);

    /**
     * 获取所有的日期
     */
    List<String> getDateList(Integer year, Integer month, String app, String stream);




}
