package com.genersoft.iot.vmp.service.impl;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StreamPushUploadFileHandler extends AnalysisEventListener<StreamPushExcelDto> {

    private IStreamPushService pushService;
    private String defaultMediaServerId;
    private List<StreamPushItem> streamPushItems = new ArrayList<>();
    private Set<String> streamPushStreamSet = new HashSet<>();
    private Set<String> streamPushGBSet = new HashSet<>();

    public StreamPushUploadFileHandler(IStreamPushService pushService, String defaultMediaServerId) {
        this.pushService = pushService;
        this.defaultMediaServerId = defaultMediaServerId;
    }

    @Override
    public void invoke(StreamPushExcelDto streamPushExcelDto, AnalysisContext analysisContext) {
        if (StringUtils.isEmpty(streamPushExcelDto.getApp())
                || StringUtils.isEmpty(streamPushExcelDto.getStream())
                || StringUtils.isEmpty(streamPushExcelDto.getGbId())) {
            return;
        }
        if (streamPushGBSet.contains(streamPushExcelDto.getGbId()) || streamPushStreamSet.contains(streamPushExcelDto.getApp() + streamPushExcelDto.getStream())) {
            return;
        }
        StreamPushItem streamPushItem = new StreamPushItem();
        streamPushItem.setApp(streamPushExcelDto.getApp());
        streamPushItem.setStream(streamPushExcelDto.getStream());
        streamPushItem.setGbId(streamPushExcelDto.getGbId());
        streamPushItem.setStatus(false);
        streamPushItem.setStreamType("push");
        streamPushItem.setCreateStamp(System.currentTimeMillis()/1000);
        streamPushItem.setMediaServerId(defaultMediaServerId);
        streamPushItem.setName(streamPushExcelDto.getName());
        streamPushItem.setOriginType(2);
        streamPushItem.setOriginTypeStr("rtsp_push");
        streamPushItem.setTotalReaderCount("0");
        streamPushItems.add(streamPushItem);
        streamPushGBSet.add(streamPushExcelDto.getGbId());
        streamPushStreamSet.add(streamPushExcelDto.getApp()+streamPushExcelDto.getStream());
        if (streamPushItems.size() > 300) {
            pushService.batchAdd(streamPushItems);
            // 存储完成清理 list
            streamPushItems.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        pushService.batchAdd(streamPushItems);
        streamPushGBSet.clear();
        streamPushStreamSet.clear();
    }
}
