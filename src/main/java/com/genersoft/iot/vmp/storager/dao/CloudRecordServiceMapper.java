package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CloudRecordServiceMapper {

    int add(CloudRecordItem cloudRecordItem);

    List<CloudRecordItem> getList(@Param("query") String query, @Param("app") String app, @Param("stream") String stream,
                                  @Param("startTimeStamp") Long startTimeStamp, @Param("endTimeStamp") Long endTimeStamp,
                                  @Param("callId") String callId, List<MediaServer> mediaServerItemList,
                                  List<Integer> ids);

    List<String> queryRecordFilePathList(@Param("app") String app, @Param("stream") String stream,
                                         @Param("startTimeStamp") Long startTimeStamp, @Param("endTimeStamp") Long endTimeStamp,
                                         @Param("callId") String callId, List<MediaServer> mediaServerItemList);

    int updateCollectList(@Param("collect") boolean collect, List<CloudRecordItem> cloudRecordItemList);

    void deleteByFileList(List<String> filePathList, @Param("mediaServerId") String mediaServerId);

    List<CloudRecordItem> queryRecordListForDelete(@Param("endTimeStamp") Long endTimeStamp, String mediaServerId);

    int changeCollectById(@Param("collect") boolean collect, @Param("recordId") Integer recordId);

    int deleteList(List<CloudRecordItem> cloudRecordItemIdList);

    List<CloudRecordItem> getListByCallId(@Param("callId") String callId);

    CloudRecordItem queryOne(@Param("id") Integer id);
}
