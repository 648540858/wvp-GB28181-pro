package com.genersoft.iot.vmp.service.impl;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import org.springframework.util.StringUtils;

import java.util.*;

public class StreamPushUploadFileHandler extends AnalysisEventListener<StreamPushExcelDto> {

    private ErrorDataHandler errorDataHandler;
    private IStreamPushService pushService;
    private String defaultMediaServerId;
    private List<StreamPushItem> streamPushItems = new ArrayList<>();
    private Map<String, UploadData> streamPushItemsForPlatform = new HashMap<>();
    private Set<String> streamPushStreamSet = new HashSet<>();
    private Set<String> streamPushGBSet = new HashSet<>();
    private List<String> errorStreamList = new ArrayList<>();
    private List<String> errorGBList = new ArrayList<>();
    // 读取数量计数器
    private int loadedSize = 0;

    public StreamPushUploadFileHandler(IStreamPushService pushService, String defaultMediaServerId, ErrorDataHandler errorDataHandler) {
        this.pushService = pushService;
        this.defaultMediaServerId = defaultMediaServerId;
        this.errorDataHandler = errorDataHandler;
    }

    public interface ErrorDataHandler{
        void handle(List<String> streams, List<String> gbId);
    }

    private class UploadData{
        public String platformId;
        public Map<String, List<StreamPushItem>> catalogData = new HashMap<>();
        public List<StreamPushItem> streamPushItems = new ArrayList<>();

        public UploadData(String platformId) {
            this.platformId = platformId;
        }
    }

    @Override
    public void invoke(StreamPushExcelDto streamPushExcelDto, AnalysisContext analysisContext) {
        if (StringUtils.isEmpty(streamPushExcelDto.getApp())
                || StringUtils.isEmpty(streamPushExcelDto.getStream())
                || StringUtils.isEmpty(streamPushExcelDto.getGbId())) {
            return;
        }
        if (streamPushGBSet.contains(streamPushExcelDto.getGbId())) {
            errorGBList.add(streamPushExcelDto.getGbId());
        }
        if (streamPushStreamSet.contains(streamPushExcelDto.getApp() + streamPushExcelDto.getStream() + streamPushExcelDto.getPlatformId())) {
            errorStreamList.add(streamPushExcelDto.getApp() + "/" + streamPushExcelDto.getStream());
        }
        if (streamPushGBSet.contains(streamPushExcelDto.getGbId()) || streamPushStreamSet.contains(streamPushExcelDto.getApp() + streamPushExcelDto.getStream() + streamPushExcelDto.getPlatformId())) {
            return;
        }

        StreamPushItem streamPushItem = new StreamPushItem();
        streamPushItem.setApp(streamPushExcelDto.getApp());
        streamPushItem.setStream(streamPushExcelDto.getStream());
        streamPushItem.setGbId(streamPushExcelDto.getGbId());
        streamPushItem.setStatus(false);
        streamPushItem.setStreamType("push");
        streamPushItem.setCreateStamp(System.currentTimeMillis());
        streamPushItem.setMediaServerId(defaultMediaServerId);
        streamPushItem.setName(streamPushExcelDto.getName());
        streamPushItem.setOriginType(2);
        streamPushItem.setOriginTypeStr("rtsp_push");
        streamPushItem.setTotalReaderCount("0");
        streamPushItem.setPlatformId(streamPushExcelDto.getPlatformId());
        streamPushItem.setCatalogId(streamPushExcelDto.getCatalogId());
        if (StringUtils.isEmpty(streamPushExcelDto.getPlatformId())) {
            streamPushItems.add(streamPushItem);
        }else {
            UploadData uploadData = streamPushItemsForPlatform.get(streamPushExcelDto.getPlatformId());
            if (uploadData == null) {
                uploadData = new UploadData(streamPushExcelDto.getPlatformId());
                streamPushItemsForPlatform.put(streamPushExcelDto.getPlatformId(), uploadData);
            }
            if (!StringUtils.isEmpty(streamPushExcelDto.getCatalogId())) {
                List<StreamPushItem> streamPushItems = uploadData.catalogData.get(streamPushExcelDto.getCatalogId());
                if (streamPushItems == null) {
                    streamPushItems = new ArrayList<>();
                    uploadData.catalogData.put(streamPushExcelDto.getCatalogId(), streamPushItems);
                }
                streamPushItems.add(streamPushItem);
            }else {
                uploadData.streamPushItems.add(streamPushItem);
            }

        }

        streamPushGBSet.add(streamPushExcelDto.getGbId());
        streamPushStreamSet.add(streamPushExcelDto.getApp()+streamPushExcelDto.getStream());
        loadedSize ++;
        if (loadedSize > 1000) {
            saveData();
            streamPushItems.clear();
            streamPushItemsForPlatform.clear();
            loadedSize = 0;
        }

    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        saveData();
        streamPushGBSet.clear();
        streamPushStreamSet.clear();
        errorDataHandler.handle(errorStreamList, errorGBList);
    }

    private void saveData(){
        if (streamPushItems.size() > 0) {
            pushService.batchAddForUpload(null, null, streamPushItems);
        }
        // 处理已分配到平台的流
        if (streamPushItemsForPlatform.size() > 0){
            for (String platformId : streamPushItemsForPlatform.keySet()) {
                UploadData uploadData = streamPushItemsForPlatform.get(platformId);
                if (uploadData.streamPushItems.size() > 0) {
                    pushService.batchAddForUpload(platformId, null, uploadData.streamPushItems);
                }
                if (uploadData.catalogData.size() > 0) {
                    for (String catalogId : uploadData.catalogData.keySet()) {
                        if (uploadData.catalogData.get(catalogId).size() > 0) {
                            pushService.batchAddForUpload(platformId, catalogId, uploadData.catalogData.get(catalogId));
                        }
                    }
                }
            }
        }
    }
}
