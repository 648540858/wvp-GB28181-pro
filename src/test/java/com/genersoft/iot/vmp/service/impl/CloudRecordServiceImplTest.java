package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.AssistRESTfulUtils;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.CloudRecordServiceMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudRecordServiceImplTest {

    private static final String APP = "synthetic-app";
    private static final String STREAM = "synthetic-stream";
    private static final String START_TIME = "2026-10-20 10:00:00";
    private static final String END_TIME = "2026-10-20 10:05:00";

    @Mock
    private CloudRecordServiceMapper cloudRecordServiceMapper;

    @Mock
    private IMediaServerService mediaServerService;

    @Mock
    private IRedisCatchStorage redisCatchStorage;

    @Mock
    private AssistRESTfulUtils assistRESTfulUtils;

    @Mock
    private UserSetting userSetting;

    @Mock
    private IRedisRpcPlayService redisRpcPlayService;

    @InjectMocks
    private CloudRecordServiceImpl cloudRecordService;

    @Test
    void addTask_shouldQueryStoredRecordTimesInMilliseconds() {
        MediaServer mediaServer = new MediaServer();
        mediaServer.setRecordAssistPort(19090);
        when(cloudRecordServiceMapper.queryRecordFilePathList(
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(List.of("synthetic/record.mp4"));

        JSONObject response = new JSONObject();
        response.put("code", 0);
        response.put("data", "synthetic-task");
        when(assistRESTfulUtils.addTask(eq(mediaServer), eq(APP), eq(STREAM), eq(START_TIME), eq(END_TIME),
                isNull(), anyList(), isNull())).thenReturn(response);

        assertEquals("synthetic-task", cloudRecordService.addTask(
                APP, STREAM, mediaServer, START_TIME, END_TIME, null, null, false));

        ArgumentCaptor<Long> startTime = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> endTime = ArgumentCaptor.forClass(Long.class);
        verify(cloudRecordServiceMapper).queryRecordFilePathList(
                eq(APP), eq(STREAM), startTime.capture(), endTime.capture(), isNull(), isNull());
        assertEquals(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(START_TIME), startTime.getValue());
        assertEquals(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(END_TIME), endTime.getValue());
        assertTrue(startTime.getValue() > 1_000_000_000_000L,
                "The query bounds must use the millisecond unit stored in cloud-record rows");
    }

    @Test
    void changeCollect_shouldQueryStoredRecordTimesInMilliseconds() {
        when(cloudRecordServiceMapper.getList(isNull(), eq(APP), eq(STREAM), any(Long.class), any(Long.class),
                isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(new CloudRecordItem()));
        when(cloudRecordServiceMapper.updateCollectList(eq(true), anyList())).thenReturn(1);

        assertEquals(1, cloudRecordService.changeCollect(true, APP, STREAM, null,
                START_TIME, END_TIME, null));

        ArgumentCaptor<Long> startTime = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> endTime = ArgumentCaptor.forClass(Long.class);
        verify(cloudRecordServiceMapper).getList(isNull(), eq(APP), eq(STREAM), startTime.capture(),
                endTime.capture(), isNull(), isNull(), isNull(), isNull());
        assertEquals(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(START_TIME), startTime.getValue());
        assertEquals(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(END_TIME), endTime.getValue());
    }
}
