package com.genersoft.iot.vmp.streamPush.dao;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
@Repository
public interface StreamPushMapper {

    Integer dataType = ChannelDataType.GB28181.value;

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(StreamPush streamPushItem);

    int update(StreamPush streamPushItem);

    int del(@Param("id") int id);

    List<StreamPush> selectAll(@Param("query") String query, @Param("pushing") Boolean pushing, @Param("mediaServerId") String mediaServerId);

    StreamPush selectByAppAndStream(@Param("app") String app, @Param("stream") String stream);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int addAll(List<StreamPush> streamPushItems);

    List<StreamPush> selectAllByMediaServerId(String mediaServerId);

    List<StreamPush> selectAllByMediaServerIdWithOutGbID(String mediaServerId);

    int updatePushStatus(@Param("id") int id, @Param("pushing") boolean pushing);

    List<StreamPush> getListFromRedis(List<StreamPushItemFromRedis> offlineStreams);

    List<String> getAllAppAndStream();

    int getAllCount();

    int getAllPushing(Boolean usePushingAsStatus);

    @MapKey("uniqueKey")
    Map<String, StreamPush> getAllAppAndStreamMap();

    @MapKey("gbDeviceId")
    Map<String, StreamPush> getAllGBId();

    StreamPush queryOne(@Param("id") int id);

    List<StreamPush> selectInSet(Set<Integer> ids);

    void batchDel(List<StreamPush> streamPushList);

    int batchUpdate(List<StreamPush> streamPushItemForUpdate);
}
