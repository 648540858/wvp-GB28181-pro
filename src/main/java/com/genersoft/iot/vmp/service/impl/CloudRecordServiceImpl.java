package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.media.zlm.AssistRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnRecordMp4HookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.ICloudRecordService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.CloudRecordServiceMapper;
import com.genersoft.iot.vmp.utils.CloudRecordUtils;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.time.*;
import java.util.*;

@Service
public class CloudRecordServiceImpl implements ICloudRecordService {

    private final static Logger logger = LoggerFactory.getLogger(CloudRecordServiceImpl.class);

    @Autowired
    private CloudRecordServiceMapper cloudRecordServiceMapper;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private AssistRESTfulUtils assistRESTfulUtils;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private ZlmHttpHookSubscribe subscribe;

    @Autowired
    private DynamicTask dynamicTask;

    @Override
    public PageInfo<CloudRecordItem> getList(int page, int count, String query, String app, String stream, String startTime, String endTime, List<MediaServerItem> mediaServerItems) {
        // 开始时间和结束时间在数据库中都是以秒为单位的
        Long startTimeStamp = null;
        Long endTimeStamp = null;
        if (startTime != null ) {
            if (!DateUtil.verification(startTime, DateUtil.formatter)) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "开始时间格式错误，正确格式为： " + DateUtil.formatter);
            }
            startTimeStamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime) * 1000;

        }
        if (endTime != null ) {
            if (!DateUtil.verification(endTime, DateUtil.formatter)) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "结束时间格式错误，正确格式为： " + DateUtil.formatter);
            }
            endTimeStamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) * 1000;

        }
        PageHelper.startPage(page, count);
        List<CloudRecordItem> all = cloudRecordServiceMapper.getList(query, app, stream, startTimeStamp, endTimeStamp,
                null, mediaServerItems);
        return new PageInfo<>(all);
    }

    @Override
    public List<String> getDateList(String app, String stream, int year, int month, List<MediaServerItem> mediaServerItems) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate;
        if (month == 12) {
            endDate = LocalDate.of(year + 1, 1, 1);
        }else {
            endDate = LocalDate.of(year, month + 1, 1);
        }
        long startTimeStamp = startDate.atStartOfDay().toInstant(ZoneOffset.ofHours(8)).getEpochSecond() * 1000;
        long endTimeStamp = endDate.atStartOfDay().toInstant(ZoneOffset.ofHours(8)).getEpochSecond() * 1000;
        List<CloudRecordItem> cloudRecordItemList = cloudRecordServiceMapper.getList(null, app, stream, startTimeStamp,
                endTimeStamp, null, mediaServerItems);
        if (cloudRecordItemList.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> resultSet = new HashSet<>();
        cloudRecordItemList.stream().forEach(cloudRecordItem -> {
            String date = DateUtil.timestampTo_yyyy_MM_dd(cloudRecordItem.getStartTime());
            resultSet.add(date);
        });
        return new ArrayList<>(resultSet);
    }

    @Override
    public void addRecord(OnRecordMp4HookParam param) {
        CloudRecordItem cloudRecordItem = CloudRecordItem.getInstance(param);
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(param.getApp(), param.getStream());
        if (streamAuthorityInfo != null) {
            cloudRecordItem.setCallId(streamAuthorityInfo.getCallId());
        }
        logger.info("[添加录像记录] {}/{} 文件大小：{}, 时长： {}秒", param.getApp(), param.getStream(), param.getFile_size(),param.getTime_len());
        cloudRecordServiceMapper.add(cloudRecordItem);
    }

    @Override
    public String addTask(String app, String stream, String mediaServerId, String startTime, String endTime, String callId, String remoteHost) {
        // 参数校验
        assert app != null;
        assert stream != null;
        MediaServerItem mediaServerItem = null;
        if (mediaServerId == null) {
            mediaServerItem = mediaServerService.getDefaultMediaServer();
        }else {
            mediaServerItem = mediaServerService.getOne(mediaServerId);
        }
        if (mediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到可用的流媒体");
        }else {
            if (remoteHost == null) {
                remoteHost = "http://" + mediaServerItem.getStreamIp() + ":" + mediaServerItem.getRecordAssistPort();
            }
        }
        if (mediaServerItem.getRecordAssistPort() == 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "为配置Assist服务");
        }
        Long startTimeStamp = null;
        Long endTimeStamp = null;
        if (startTime != null) {
            startTimeStamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime) * 1000;
        }
        if (endTime != null) {
            endTimeStamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) * 1000;
        }

        List<MediaServerItem> mediaServers = new ArrayList<>();
        mediaServers.add(mediaServerItem);
        // 检索相关的录像文件
        List<String> filePathList = cloudRecordServiceMapper.queryRecordFilePathList(app, stream, startTimeStamp, endTimeStamp, callId, mediaServers);
        if (filePathList == null || filePathList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未检索到视频文件");
        }
        JSONObject result =  assistRESTfulUtils.addTask(mediaServerItem, app, stream, startTime, endTime, callId, filePathList, remoteHost);
        if (result.getInteger("code") != 0) {
            throw new ControllerException(result.getInteger("code"), result.getString("msg"));
        }
        return result.getString("data");
    }

    @Override
    public JSONArray queryTask(String app, String stream, String callId, String taskId, String mediaServerId, Boolean isEnd) {
        MediaServerItem mediaServerItem = null;
        if (mediaServerId == null) {
            mediaServerItem = mediaServerService.getDefaultMediaServer();
        }else {
            mediaServerItem = mediaServerService.getOne(mediaServerId);
        }
        if (mediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到可用的流媒体");
        }
        JSONObject result =  assistRESTfulUtils.queryTaskList(mediaServerItem, app, stream, callId, taskId, isEnd);
        if (result == null || result.getInteger("code") != 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), result.getString("msg"));
        }
        return result.getJSONArray("data");
    }

    @Override
    public int changeCollect(boolean result, String app, String stream, String mediaServerId, String startTime, String endTime, String callId) {
        // 开始时间和结束时间在数据库中都是以秒为单位的
        Long startTimeStamp = null;
        Long endTimeStamp = null;
        if (startTime != null ) {
            if (!DateUtil.verification(startTime, DateUtil.formatter)) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "开始时间格式错误，正确格式为： " + DateUtil.formatter);
            }
            startTimeStamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime) * 1000;

        }
        if (endTime != null ) {
            if (!DateUtil.verification(endTime, DateUtil.formatter)) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "结束时间格式错误，正确格式为： " + DateUtil.formatter);
            }
            endTimeStamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) * 1000;

        }

        List<MediaServerItem> mediaServerItems;
        if (!ObjectUtils.isEmpty(mediaServerId)) {
            mediaServerItems = new ArrayList<>();
            MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
            if (mediaServerItem == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到流媒体: " + mediaServerId);
            }
            mediaServerItems.add(mediaServerItem);
        } else {
            mediaServerItems = null;
        }

        List<CloudRecordItem> all = cloudRecordServiceMapper.getList(null, app, stream, startTimeStamp, endTimeStamp,
                callId, mediaServerItems);
        if (all.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到待收藏的视频");
        }
        int limitCount = 50;
        int resultCount = 0;
        if (all.size() > limitCount) {
            for (int i = 0; i < all.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > all.size()) {
                    toIndex = all.size();
                }
                resultCount += cloudRecordServiceMapper.updateCollectList(result, all.subList(i, toIndex));

            }
        }else {
            resultCount = cloudRecordServiceMapper.updateCollectList(result, all);
        }
        return resultCount;
    }

    @Override
    public int changeCollectById(Integer recordId, boolean result) {
       return cloudRecordServiceMapper.changeCollectById(result, recordId);
    }

    @Override
    public DownloadFileInfo getPlayUrlPath(Integer recordId) {
        CloudRecordItem recordItem = cloudRecordServiceMapper.queryOne(recordId);
        if (recordItem == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "资源不存在");
        }
        String filePath = recordItem.getFilePath();
        MediaServerItem mediaServerItem = mediaServerService.getOne(recordItem.getMediaServerId());
        return CloudRecordUtils.getDownloadFilePath(mediaServerItem, filePath);
    }

    @Override
    public void getLivePath(Integer recordId, ErrorCallback<StreamInfo> callback) {
        CloudRecordItem recordItem = cloudRecordServiceMapper.queryOne(recordId);
        if (recordItem == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "资源不存在");
        }
        // 监听流上线
        String app = "record-live";
        String stream = recordItem.getId() + "";
        MediaServerItem mediaServerItem = mediaServerService.getOne(recordItem.getMediaServerId());
        if (mediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "录像记录使用的流媒体节点不在线");
        }
        StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, recordItem.getMediaServerId(), false);
        if (streamInfo != null) {
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            return;
        }
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            // 取消订阅消息监听
            HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed(app, stream, true, "rtsp", mediaServerItem.getId());
            subscribe.removeSubscribe(hookSubscribe);
            callback.run(ErrorCode.ERROR100.getCode(), "加载视频文件为视频流超时", null);
        }, 10000);
        HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed(app, stream, true, "rtsp", mediaServerItem.getId());
        subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, HookParam hookParam) -> {
            dynamicTask.stop(timeOutTaskKey);
            OnStreamChangedHookParam streamChangedHookParam = (OnStreamChangedHookParam)hookParam;
            StreamInfo streamInfoForHook = mediaService.getStreamInfoByAppAndStream(mediaServerItem, app, stream, streamChangedHookParam.getTracks(), null);
            subscribe.removeSubscribe(hookSubscribe);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfoForHook);
        });
        JSONObject jsonObject = zlmresTfulUtils.loadMP4File(mediaServerItem, app, stream, recordItem.getFilePath());
        if (jsonObject == null || jsonObject.getInteger("code") != 0) {
            subscribe.removeSubscribe(hookSubscribe);
            dynamicTask.stop(timeOutTaskKey);
            callback.run(ErrorCode.SUCCESS.getCode(),
                    jsonObject != null ? jsonObject.getString("msg"): "加载视频文件为视频流失败", null);
        }
    }
}
