package com.genersoft.iot.vmp.service.impl;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.springframework.util.StringUtils;

import java.util.*;

public class StreamPushUploadFileHandler extends AnalysisEventListener<StreamPushExcelDto> {

    // 错误数据的回调，用于将错误数据发送给页面
    private ErrorDataHandler errorDataHandler;

    // 推流的业务类用于存储数据
    private IStreamPushService pushService;

    // 默认流媒体节点ID
    private String defaultMediaServerId;

    // 用于存储不加过滤的所有数据
    private List<StreamPushItem> streamPushItems = new ArrayList<>();

    // 用于存储更具APP+Stream过滤后的数据，可以直接存入stream_push表与gb_stream表
    private Map<String,StreamPushItem> streamPushItemForSave = new HashMap<>();

    // 用于存储按照APP+Stream为KEY， 平台ID+目录Id 为value的数据，用于存储到gb_stream表后获取app+Stream对应的平台与目录信息，然后存入关联表
    private Map<String, List<String[]>> streamPushItemsForPlatform = new HashMap<>();

    // 用于判断文件是否存在重复的app+Stream+平台ID
    private Set<String> streamPushStreamSet = new HashSet<>();

    // 用于存储APP+Stream->国标ID 的数据结构, 数据一一对应，全局判断APP+Stream->国标ID是否存在不对应
    private BiMap<String,String> gBMap = HashBiMap.create();

    // 记录错误的APP+Stream
    private List<String> errorStreamList = new ArrayList<>();


    // 记录错误的国标ID
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

    @Override
    public void invoke(StreamPushExcelDto streamPushExcelDto, AnalysisContext analysisContext) {
        if (StringUtils.isEmpty(streamPushExcelDto.getApp())
                || StringUtils.isEmpty(streamPushExcelDto.getStream())
                || StringUtils.isEmpty(streamPushExcelDto.getGbId())) {
            return;
        }

        if (gBMap.get(streamPushExcelDto.getApp() + streamPushExcelDto.getStream()) == null) {
            try {
                gBMap.put(streamPushExcelDto.getApp() + streamPushExcelDto.getStream(), streamPushExcelDto.getGbId());
            }catch (IllegalArgumentException e) {
                e.printStackTrace();
                errorGBList.add(streamPushExcelDto.getGbId() + "(不同的app+stream使用了相同的国标ID)");
                return;
            }
        }else {
            if (!gBMap.get(streamPushExcelDto.getApp() + streamPushExcelDto.getStream()).equals(streamPushExcelDto.getGbId())) {
                errorGBList.add(streamPushExcelDto.getGbId() + "(同一组app+stream使用了不同的国标ID)");
                return;
            }
        }

        if (streamPushStreamSet.contains(streamPushExcelDto.getApp() + streamPushExcelDto.getStream() + streamPushExcelDto.getPlatformId())) {
            errorStreamList.add(streamPushExcelDto.getApp() + "/" + streamPushExcelDto.getStream()+ "/" +
                    streamPushExcelDto.getPlatformId() + "(同一组app+stream添加在了同一个平台下)");
            return;
        }else {
            streamPushStreamSet.add(streamPushExcelDto.getApp()+streamPushExcelDto.getStream() + streamPushExcelDto.getPlatformId());
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

        // 存入所有的通道信息
        streamPushItems.add(streamPushItem);
        streamPushItemForSave.put(streamPushItem.getApp() + streamPushItem.getStream(), streamPushItem);

        if (!StringUtils.isEmpty(streamPushExcelDto.getPlatformId())) {
            List<String[]> platformList = streamPushItemsForPlatform.get(streamPushItem.getApp() + streamPushItem.getStream());
            if (platformList == null) {
                platformList = new ArrayList<>();
                streamPushItemsForPlatform.put(streamPushItem.getApp() + streamPushItem.getStream(), platformList);
            }
            String platformId = streamPushExcelDto.getPlatformId();
            String catalogId = streamPushExcelDto.getCatalogId();
            if (StringUtils.isEmpty(streamPushExcelDto.getCatalogId())) {
                catalogId = null;
            }
            String[] platFormInfoArray = new String[]{platformId, catalogId};
            platformList.add(platFormInfoArray);
        }

        loadedSize ++;
        if (loadedSize > 1000) {
            saveData();
            streamPushItems.clear();
            streamPushItemForSave.clear();
            streamPushItemsForPlatform.clear();
            loadedSize = 0;
        }

    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        saveData();
        streamPushItems.clear();
        streamPushItemForSave.clear();
        gBMap.clear();
        streamPushStreamSet.clear();
        streamPushItemsForPlatform.clear();
        errorDataHandler.handle(errorStreamList, errorGBList);
    }

    private void saveData(){
        if (streamPushItemForSave.size() > 0) {
            // 向数据库查询是否存在重复的app
            pushService.batchAddForUpload(new ArrayList<>(streamPushItemForSave.values()), streamPushItemsForPlatform);
        }
    }
}
