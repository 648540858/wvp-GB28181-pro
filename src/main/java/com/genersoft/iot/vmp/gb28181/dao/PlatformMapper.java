package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.Platform;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储上级平台
 */
@Mapper
@Repository
public interface PlatformMapper {

    int add(Platform parentPlatform);

    int update(Platform parentPlatform);

    int delete(@Param("id") Integer id);

    List<Platform> queryList(@Param("query") String query);

    List<Platform> getEnableParentPlatformList(boolean enable);

    List<Platform> queryEnablePlatformListWithAsMessageChannel();

    Platform getParentPlatByServerGBId(String platformGbId);

    Platform query(int id);

    int updateStatus(@Param("platformGbID") String platformGbID, @Param("online") boolean online);

    List<Platform> queryEnablePlatformList();

}
