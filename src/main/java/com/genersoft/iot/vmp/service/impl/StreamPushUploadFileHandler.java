package com.genersoft.iot.vmp.service.impl;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.springframework.util.ObjectUtils;

import java.util.*;

public class StreamPushUploadFileHandler extends AnalysisEventListener<StreamPushExcelDto> {

    /**
     * 错误数据的回调，用于将错误数据发送给页面
     */
    private ErrorDataHandler errorDataHandler;

    /**
     * 推流的业务类用于存储数据
     */
    private IStreamPushService pushService;

    /**
     * 默认流媒体节点ID
     */
    private String defaultMediaServerId;

    /**
     * 用于存储不加过滤的所有数据
     */
    private final List<StreamPushItem> streamPushItems = new ArrayList<>();

    /**
     * 用于存储更具APP+Stream过滤后的数据，可以直接存入stream_push表与gb_stream表
     */
    private final Map<String,StreamPushItem> streamPushItemForSave = new HashMap<>();

    /**
     * 用于存储按照APP+Stream为KEY， 平台ID+目录Id 为value的数据，用于存储到gb_stream表后获取app+Stream对应的平台与目录信息，然后存入关联表
     */
    private final Map<String, List<String[]>> streamPushItemsForPlatform = new HashMap<>();

    /**
     * 用于判断文件是否存在重复的app+Stream+平台ID
     */
    private final Set<String> streamPushStreamSet = new HashSet<>();

    /**
     * 用于存储APP+Stream->国标ID 的数据结构, 数据一一对应，全局判断APP+Stream->国标ID是否存在不对应
     */
    private final BiMap<String,String> gBMap = HashBiMap.create();

    /**
     * 用于存储APP+Stream-> 在数据库中的数据
     */
    private final BiMap<String,String> pushMapInDb = HashBiMap.create();

    /**
     * 记录错误的APP+Stream
     */
    private final List<String> errorStreamList = new ArrayList<>();


    /**
     * 记录错误的国标ID
     */
    private final List<String> errorInfoList = new ArrayList<>();

    /**
     * 读取数量计数器
     */
    private int loadedSize = 0;

    public StreamPushUploadFileHandler(IStreamPushService pushService, String defaultMediaServerId, ErrorDataHandler errorDataHandler) {
        this.pushService = pushService;
        this.defaultMediaServerId = defaultMediaServerId;
        this.errorDataHandler = errorDataHandler;
        // 获取数据库已有的数据，已经存在的则忽略
        List<String> allAppAndStreams = pushService.getAllAppAndStream();
        if (allAppAndStreams.size() > 0) {
            for (String allAppAndStream : allAppAndStreams) {
                pushMapInDb.put(allAppAndStream, allAppAndStream);
            }
        }
    }

    public interface ErrorDataHandler{
        void handle(List<String> streams, List<String> gbId);
    }

    @Override
    public void invoke(StreamPushExcelDto streamPushExcelDto, AnalysisContext analysisContext) {
        if (ObjectUtils.isEmpty(streamPushExcelDto.getApp())
                || ObjectUtils.isEmpty(streamPushExcelDto.getStream())
                || ObjectUtils.isEmpty(streamPushExcelDto.getGbId())) {
            return;
        }
        Integer rowIndex = analysisContext.readRowHolder().getRowIndex();

        if (gBMap.get(streamPushExcelDto.getApp() + streamPushExcelDto.getStream()) == null) {
            try {
                gBMap.put(streamPushExcelDto.getApp() + streamPushExcelDto.getStream(), streamPushExcelDto.getGbId());
            }catch (IllegalArgumentException e) {
                errorInfoList.add("行：" + rowIndex + ", " + streamPushExcelDto.getGbId() + " 国标ID重复使用");
                return;
            }
        }else {
            if (!gBMap.get(streamPushExcelDto.getApp() + streamPushExcelDto.getStream()).equals(streamPushExcelDto.getGbId())) {
                errorInfoList.add("行：" + rowIndex + ", " + streamPushExcelDto.getGbId() + " 同样的应用名和流ID使用了不同的国标ID");
                return;
            }
        }

        if (streamPushStreamSet.contains(streamPushExcelDto.getApp() + streamPushExcelDto.getStream() + streamPushExcelDto.getPlatformId())) {
            errorStreamList.add("行：" + rowIndex + ", " +  streamPushExcelDto.getApp() + "/" + streamPushExcelDto.getStream()+  " 平台信息重复");
            return;
        }else {
            if (pushMapInDb.get(streamPushExcelDto.getApp()+streamPushExcelDto.getStream()) != null) {
                errorStreamList.add("行：" + rowIndex + ", " +  streamPushExcelDto.getApp() + "/" + streamPushExcelDto.getStream()+  " 数据已存在");
                return;
            }
            streamPushStreamSet.add(streamPushExcelDto.getApp()+streamPushExcelDto.getStream() + streamPushExcelDto.getPlatformId());
        }

        StreamPushItem streamPushItem = new StreamPushItem();
        streamPushItem.setApp(streamPushExcelDto.getApp());
        streamPushItem.setStream(streamPushExcelDto.getStream());
        streamPushItem.setGbId(streamPushExcelDto.getGbId());
        streamPushItem.setStatus(streamPushExcelDto.getStatus());
        streamPushItem.setStreamType("push");
        streamPushItem.setCreateTime(DateUtil.getNow());
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

        if (!ObjectUtils.isEmpty(streamPushExcelDto.getPlatformId())) {
            List<String[]> platformList = streamPushItemsForPlatform.get(streamPushItem.getApp() + streamPushItem.getStream());
            if (platformList == null) {
                platformList = new ArrayList<>();
                streamPushItemsForPlatform.put(streamPushItem.getApp() + streamPushItem.getStream(), platformList);
            }
            String platformId = streamPushExcelDto.getPlatformId();
            String catalogId = streamPushExcelDto.getCatalogId();
            if (ObjectUtils.isEmpty(streamPushExcelDto.getCatalogId())) {
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
        errorDataHandler.handle(errorStreamList, errorInfoList);
    }

    private void saveData(){
        if (streamPushItemForSave.size() > 0) {
            // 向数据库查询是否存在重复的app
            pushService.batchAddForUpload(new ArrayList<>(streamPushItemForSave.values()), streamPushItemsForPlatform);
        }
    }
}
