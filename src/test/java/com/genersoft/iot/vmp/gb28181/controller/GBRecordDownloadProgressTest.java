package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GBRecordDownloadProgressTest {
    @Mock private IPlayService playService;
    @Mock private IDeviceService deviceService;
    @Mock private IDeviceChannelService channelService;
    @InjectMocks private GBRecordController controller;

    /** 真实HTTP DTO出口必须保留短文件的中断状态与实际时长，不能只透出文件地址。 */
    @Test
    void progressHttpResponsePreservesIncompleteResult() throws Exception {
        Device device = new Device();
        DeviceChannel channel = new DeviceChannel();
        when(deviceService.getDeviceByDeviceId("synthetic-device")).thenReturn(device);
        when(channelService.getOne("synthetic-device", "synthetic-channel")).thenReturn(channel);
        StreamInfo info = new StreamInfo();
        info.setProgress(0.192);
        info.setDownloadStatus("interrupted");
        info.setDownloadedDuration(115.263);
        DownloadFileInfo file = new DownloadFileInfo();
        file.setHttpsPath("https://example.invalid/partial.mp4");
        info.setDownLoadFilePath(file);
        when(playService.getDownLoadInfo(device, channel, "synthetic-stream")).thenReturn(info);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        for (String language : new String[]{"zh", "en"}) {
            // API只返回稳定状态与数值，不下发内部诊断文本；文案由现有管理页负责。
            mvc.perform(get("/api/gb_record/download/progress/synthetic-device/synthetic-channel/synthetic-stream")
                            .header("Accept-Language", language))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.downloadStatus").value("interrupted"))
                    .andExpect(jsonPath("$.downloadedDuration").value(115.263))
                    .andExpect(jsonPath("$.progress").value(0.192))
                    .andExpect(jsonPath("$.downLoadFilePath.httpsPath").value("https://example.invalid/partial.mp4"))
                    .andExpect(jsonPath("$.downloadMessage").doesNotExist());
        }
    }
}
