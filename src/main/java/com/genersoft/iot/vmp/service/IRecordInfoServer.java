package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.reponse.ResponseData;
import com.genersoft.iot.vmp.storager.dao.dto.RecordInfo;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;

public interface IRecordInfoServer {

    PageInfo<RecordInfo> getRecordList(int page, int count);

    ResponseData resetRecords(Map<String, Object> params);
}
