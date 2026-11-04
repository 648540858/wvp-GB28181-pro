package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.service.download.SegmentedDownloadService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 在既有HTTP路由上证明可选恢复参数与默认下载互不串路，不启动真实设备。 */
@SuppressWarnings("unchecked")
class GBRecordMergeResumeControllerTest {
    private static final String DEVICE="synthetic-device", CHANNEL="synthetic-channel";
    private static final String START="2026-10-27 10:00:00", END="2026-10-27 10:01:00";
    private static final String OLD="11111111-2222-3333-4444-555555555555";
    private final IPlayService plays=mock(IPlayService.class);
    private final IDeviceService devices=mock(IDeviceService.class);
    private final IDeviceChannelService channels=mock(IDeviceChannelService.class);
    private final ObjectProvider<SegmentedDownloadService> provider=mock(ObjectProvider.class);
    private final SegmentedDownloadService segmented=mock(SegmentedDownloadService.class);
    private final Device device=new Device();
    private final DeviceChannel channel=new DeviceChannel();
    private final GBRecordController controller=new GBRecordController();

    /** 真实DeferredResultHolder保证回调实际写入HTTP结果，不以mock调用次数代替业务返回。 */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller,"playService",plays);ReflectionTestUtils.setField(controller,"deviceService",devices);
        ReflectionTestUtils.setField(controller,"channelService",channels);ReflectionTestUtils.setField(controller,"segmentedDownloads",provider);
        ReflectionTestUtils.setField(controller,"resultHolder",new DeferredResultHolder());ReflectionTestUtils.setField(controller,"userSetting",new UserSetting());
        when(devices.getDeviceByDeviceId(DEVICE)).thenReturn(device);when(channels.getOne(DEVICE,CHANNEL)).thenReturn(channel);
    }

    /** 构造不含文件地址的逻辑结果，用于验证沿用原StreamContent响应包装。 */
    private StreamInfo result() { StreamInfo info=new StreamInfo();info.setStream("synthetic-parent");return info; }

    /** 不传参数保持原始download调用与回调结果，连可选Service provider也不解析。 */
    @Test
    void download_defaultPathKeepsOriginalServiceAndResponse() {
        doAnswer(call->{((ErrorCallback<StreamInfo>)call.getArgument(5)).run(0,"success",result());return null;})
                .when(plays).download(eq(device),eq(channel),eq(START),eq(END),eq(4),any());
        Object body=controller.download(new MockHttpServletRequest(),DEVICE,CHANNEL,START,END,"4").getResult();
        assertInstanceOf(WVPResult.class,body);assertEquals(0,((WVPResult<?>)body).getCode());
        verifyNoInteractions(provider,segmented);verify(plays).download(eq(device),eq(channel),eq(START),eq(END),eq(4),any());
    }

    /** 真实HTTP参数绑定只在显式旧UUID下进入合并恢复，绝不调用普通下载。 */
    @Test
    void download_httpExplicitResumeUsesSameRouteWithoutNativeFallback() throws Exception {
        when(provider.getIfAvailable()).thenReturn(segmented);
        doAnswer(call->{((ErrorCallback<StreamInfo>)call.getArgument(5)).run(0,"success",result());return null;})
                .when(segmented).resumeMerge(eq(device),eq(channel),eq(START),eq(END),eq(OLD),any());
        MockMvc mvc=MockMvcBuilders.standaloneSetup(controller).build();
        MvcResult response=mvc.perform(get("/api/gb_record/download/start/"+DEVICE+"/"+CHANNEL)
                .param("startTime",START).param("endTime",END).param("downloadSpeed","4").param("resumeTaskId",OLD))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(response)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        verifyNoInteractions(plays);
    }

    /** 空字符串仍是显式恢复意图，拒绝后不能悄悄发起新的设备下载。 */
    @Test
    void download_emptyResumeOrMissingCapabilityNeverFallsBack() {
        when(provider.getIfAvailable()).thenReturn(segmented);
        doAnswer(call->{((ErrorCallback<StreamInfo>)call.getArgument(5)).run(-1,"invalid",null);return null;})
                .when(segmented).resumeMerge(eq(device),eq(channel),eq(START),eq(END),eq(""),any());
        Object invalid=controller.download(new MockHttpServletRequest(),DEVICE,CHANNEL,START,END,"4","").getResult();
        assertEquals(-1,((WVPResult<?>)invalid).getCode());
        when(provider.getIfAvailable()).thenReturn(null);
        Object unsupported=controller.download(new MockHttpServletRequest(),DEVICE,CHANNEL,START,END,"4",OLD).getResult();
        assertEquals(-1,((WVPResult<?>)unsupported).getCode());verifyNoInteractions(plays);
    }

    /** 真实HTTP未传resumeTaskId时，绑定结果仍为null并完全保留原下载回调。 */
    @Test
    void download_httpMissingResumeParamRemainsNativePath() throws Exception {
        doAnswer(call->{((ErrorCallback<StreamInfo>)call.getArgument(5)).run(0,"success",result());return null;})
                .when(plays).download(eq(device),eq(channel),eq(START),eq(END),eq(4),any());
        MockMvc mvc=MockMvcBuilders.standaloneSetup(controller).build();
        MvcResult response=mvc.perform(get("/api/gb_record/download/start/"+DEVICE+"/"+CHANNEL)
                .param("startTime",START).param("endTime",END).param("downloadSpeed","4"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(response)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        verifyNoInteractions(provider,segmented);
    }
}
