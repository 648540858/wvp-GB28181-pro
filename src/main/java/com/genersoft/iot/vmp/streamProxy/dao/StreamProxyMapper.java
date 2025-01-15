package com.genersoft.iot.vmp.streamProxy.dao;

import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface StreamProxyMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(StreamProxy streamProxyDto);

    int update(StreamProxy streamProxyDto);

    int delByAppAndStream(String app, String stream);

    List<StreamProxy> selectAll(@Param("query") String query, @Param("pulling") Boolean pulling, @Param("mediaServerId") String mediaServerId);

    StreamProxy selectOneByAppAndStream(@Param("app") String app, @Param("stream") String stream);

    List<StreamProxy> selectForPushingInMediaServer(@Param("mediaServerId") String mediaServerId, @Param("enable") boolean enable);

    int getAllCount();

    int getOnline();

    int delete(@Param("id") int id);

    void deleteByList(List<StreamProxy> streamProxiesForRemove);

    int online(@Param("id") int id);

    int offline(@Param("id") int id);

    StreamProxy select(@Param("id") int id);

    void removeStream(@Param("id") int id);

    void addStream(StreamProxy streamProxy);
}
