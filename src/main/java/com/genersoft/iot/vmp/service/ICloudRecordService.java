package com.genersoft.iot.vmp.service;

import com.alibaba.fastjson2.JSONArray;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnRecordMp4HookParam;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
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
    PageInfo<CloudRecordItem> getList(int page, int count, String query,  String app, String stream, String startTime, String endTime, List<MediaServerItem> mediaServerItems);

    /**
     * 根据hook消息增加一条记录
     */
    void addRecord(OnRecordMp4HookParam param);

    /**
     * 获取所有的日期
     */
    List<String> getDateList(String app, String stream, int year, int month, List<MediaServerItem> mediaServerItems);

    /**
     * 添加合并任务
     */
    String addTask(String app, String stream, String mediaServerId, String startTime, String endTime, String callId, String remoteHost);


    /**
     * 查询合并任务列表
     */
    JSONArray queryTask(String app, String stream, String callId, String taskId, String mediaServerId, Boolean isEnd);

    /**
     * 收藏视频，收藏的视频过期不会删除
     */
    int changeCollect(boolean result, String app, String stream, String mediaServerId, String startTime, String endTime, String callId);

    /**
     * 添加指定录像收藏
     */
    int changeCollectById(Integer recordId, boolean result);

    /**
     * 获取播放地址
     */
    DownloadFileInfo getPlayUrlPath(Integer recordId);
}
