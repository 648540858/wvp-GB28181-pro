package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnRecordMp4HookParam;
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
    PageInfo<CloudRecordItem> getList(int page, int count, String app, String stream, String startTime, String endTime, List<MediaServerItem> mediaServerItems);

    /**
     * 根据hook消息增加一条记录
     */
    void addRecord(OnRecordMp4HookParam param);

    /**
     * 获取所有的日期
     */
    List<String> getDateList(String app, String stream, int year, int month, List<MediaServerItem> mediaServerItems);

}
