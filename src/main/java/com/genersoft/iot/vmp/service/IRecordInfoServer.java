package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.storager.dao.dto.RecordInfo;
import com.github.pagehelper.PageInfo;

public interface IRecordInfoServer {
    PageInfo<RecordInfo> getRecordList(int page, int count);
}
