package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.service.bean.Region;
import com.genersoft.iot.vmp.vmanager.bean.UpdateCommonChannelToGroup;
import com.genersoft.iot.vmp.vmanager.bean.UpdateCommonChannelToRegion;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface CommonChannelPlatformMapper {


    @Select(" <script>" +
            "select * from wvp_common_channel_platform where platform_id = #{platformId} and common_gb_channel_id in " +
            "<foreach collection='commonGbChannelIds'  item='item'  open='(' separator=',' close=')' >#{item}</foreach>" +
            " </script>")
    List<Integer> findChannelsInDb(@Param("platformId") Integer platformId,
                                   @Param("commonGbChannelIds") List<Integer> commonGbChannelIds);

    @Insert("<script> " +
            "INSERT into wvp_common_channel_platform " +
            "(platform_id, common_gb_channel_id)" +
            "values " +
            "<foreach collection='commonGbChannelIds' index='index' item='item' separator=','> " +
            "(#{platformId}, #{item}) "+
            "</foreach> " +
            "</script>")
    int addChannels(@Param("platformId") Integer platformId,
                    @Param("commonGbChannelIds") List<Integer> commonGbChannelIds);

    @Delete("<script> " +
            "delete from wvp_common_channel_platform " +
            "where platform_id = #{platformId} and common_gb_channel_id in " +
            "<foreach collection='commonGbChannelIds'  item='item'  open='(' separator=',' close=')' >#{item}</foreach>" +
            "</script>")
    int removeChannels(@Param("platformId") Integer platformId,
                       @Param("commonGbChannelIds") List<Integer> commonGbChannelIds);
}
