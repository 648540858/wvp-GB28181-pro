package com.genersoft.iot.vmp.service.impl;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;

import java.util.ArrayList;
import java.util.List;

public class StreamPushUploadFileHandler extends AnalysisEventListener<StreamPushExcelDto> {

    private IStreamPushService pushService;
    private String defaultMediaServerId;
    private List<StreamPushItem> streamPushItems = new ArrayList<>();

    public StreamPushUploadFileHandler(IStreamPushService pushService, String defaultMediaServerId) {
        this.pushService = pushService;
        this.defaultMediaServerId = defaultMediaServerId;
    }

    @Override
    public void invoke(StreamPushExcelDto streamPushExcelDto, AnalysisContext analysisContext) {
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
    }
}
