package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Region;
import com.genersoft.iot.vmp.gb28181.bean.RegionTree;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

@Mapper
public interface RegionMapper {

    @Insert("INSERT INTO wvp_common_region (device_id, name, parent_id, parent_device_id, create_time, update_time) " +
            "VALUES (#{deviceId}, #{name}, #{parentId}, #{parentDeviceId}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void add(Region region);

    @Delete("DELETE FROM wvp_common_region WHERE id=#{id}")
    int delete(@Param("id") int id);

    @Update(" UPDATE wvp_common_region " +
            " SET update_time=#{updateTime}, device_id=#{deviceId}, name=#{name}, parent_id=#{parentId}, parent_device_id=#{parentDeviceId}" +
            " WHERE id = #{id}")
    int update(Region region);

    @Select(value = {" <script>" +
            "SELECT *  from wvp_common_region WHERE 1=1 " +
            " <if test='query != null'> AND (device_id LIKE concat('%',#{query},'%') escape '/' OR name LIKE concat('%',#{query},'%') escape '/')</if> " +
            " <if test='parentId != null'> AND parent_device_id = #{parentId}</if> " +
            "ORDER BY id " +
            " </script>"})
    List<Region> query(@Param("query") String query, @Param("parentId") String parentId);

    @Select("SELECT * from wvp_common_region WHERE parent_id = #{parentId} ORDER BY id ")
    List<Region> getChildren(@Param("parentId") Integer parentId);

    @Select("SELECT * from wvp_common_region WHERE id = #{id} ")
    Region queryOne(@Param("id") int id);

    @Select(" select dc.civil_code as civil_code " +
            " from wvp_device_channel dc " +
            " where dc.civil_code not in " +
            " (select device_id from wvp_common_region)")
    List<String> getUninitializedCivilCode();

    @Select(" <script>" +
            " SELECT device_id from wvp_common_region " +
            " where device_id in " +
            " <foreach collection='codes'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            " </script>")
    List<String> queryInList(Set<String> codes);


    @Insert(" <script>" +
            " INSERT INTO wvp_common_region (" +
            " device_id," +
            " name, " +
            " parent_device_id," +
            " parent_id," +
            " create_time," +
            " update_time) " +
            " VALUES " +
            " <foreach collection='regionList' index='index' item='item' separator=','> " +
            " (#{item.deviceId}, #{item.name}, #{item.parentDeviceId},#{item.parentId},#{item.createTime},#{item.updateTime})" +
            " </foreach> " +
            " </script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int batchAdd(List<Region> regionList);

    @Select(" <script>" +
            " SELECT " +
            " *, " +
            " concat('region', id) as tree_id," +
            " 0 as type," +
            " 'ON' as status," +
            " false as is_leaf" +
            " from wvp_common_region " +
            " where " +
            " <if test='parentId != null'> parent_id = #{parentId} </if> " +
            " <if test='parentId == null'> parent_id is null </if> " +
            " <if test='query != null'> AND (device_id LIKE concat('%',#{query},'%') escape '/' OR name LIKE concat('%',#{query},'%') escape '/')</if> " +
            " </script>")
    List<RegionTree> queryForTree(@Param("query") String query, @Param("parentId") Integer parentId);

    @Delete("<script>" +
            " DELETE FROM wvp_common_region WHERE id in " +
            " <foreach collection='allChildren'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>")
    void batchDelete(List<Region> allChildren);

    @Select(" <script>" +
            " SELECT * from wvp_common_region " +
            " where device_id in " +
            " <foreach collection='regionList'  item='item'  open='(' separator=',' close=')' > #{item.deviceId}</foreach>" +
            " </script>")
    List<Region> queryInRegionListByDeviceId(List<Region> regionList);

    @Select(" <script>" +
            " SELECT " +
            " wcr.device_id as gb_device_id," +
            " wcr.name as gb_name" +
            " from wvp_common_region wcr" +
            " left join wvp_platform_region wpr on wcr.id = wpr.region_id" +
            " where wpr.platform_id  = #{platformId} " +
            " </script>")
    List<CommonGBChannel> queryByPlatform(@Param("platformId") Integer platformId);


    @Update(value = " <script>" +
            " update wvp_common_region w1 " +
            " inner join (select * from wvp_common_region ) w2 on w1.parent_device_id = w2.device_id " +
            " set w1.parent_id = w2.id" +
            " where w1.id in " +
            " <foreach collection='regionListForAdd'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>", databaseId = "mysql")
    @Update( value = " <script>" +
            " update wvp_common_region w1\n" +
            " set parent_id = w2.id\n" +
            " from wvp_common_region w2\n" +
            " where w1.parent_device_id = w2.device_id\n" +
            "  and w1.id in " +
            " <foreach collection='regionListForAdd'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>", databaseId = "kingbase")
    @Update( value = " <script>" +
            " update wvp_common_region w1\n" +
            " set parent_id = w2.id\n" +
            " from wvp_common_region w2\n" +
            " where w1.parent_device_id = w2.device_id\n" +
            "  and w1.id in " +
            " <foreach collection='regionListForAdd'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>", databaseId = "postgresql")
    void updateParentId(List<Region> regionListForAdd);

    @Update(" <script>" +
            " update wvp_common_region" +
            " set parent_device_id = #{parentDeviceId}" +
            " where parent_id = #{parentId} " +
            " </script>")
    void updateChild(@Param("parentId") int parentId, @Param("parentDeviceId") String parentDeviceId);

    @Select("SELECT * from wvp_common_region WHERE device_id = #{deviceId} ")
    Region queryByDeviceId(@Param("deviceId") String deviceId);

    @Select(" <script>" +
            " SELECT " +
            " * " +
            " from wvp_common_region " +
            " where id in " +
            " <foreach collection='regionSet'  item='item'  open='(' separator=',' close=')' > #{item.parentId}</foreach>" +
            " </script>")
    Set<Region> queryParentInChannelList(Set<Region> regionSet);

    @Select(" <script>" +
            " SELECT " +
            " * " +
            " from wvp_common_region " +
            " where device_id in " +
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.gbCivilCode}</foreach>" +
            " order by id " +
            "</script>")
    Set<Region> queryByChannelList(List<CommonGBChannel> channelList);

    @Select(" <script>" +
            " SELECT * " +
            " from wvp_common_region wcr" +
            " left join wvp_platform_region wpr on wpr.region_id = wcr.id and wpr.platform_id = #{platformId}" +
            " where wpr.platform_id is null and wcr.device_id in " +
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.gbCivilCode}</foreach>" +
            " </script>")
    Set<Region> queryNotShareRegionForPlatformByChannelList(List<CommonGBChannel> channelList, @Param("platformId") Integer platformId);

    @Select(" <script>" +
            " SELECT * " +
            " from wvp_common_region wcr" +
            " left join wvp_platform_region wpr on wpr.region_id = wcr.id and wpr.platform_id = #{platformId}" +
            " where wpr.platform_id IS NULL and wcr.id in " +
            " <foreach collection='allRegion'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            " </script>")
    Set<Region> queryNotShareRegionForPlatformByRegionList(Set<Region> allRegion, @Param("platformId") Integer platformId);


    @Select(" <script>" +
            " SELECT device_id " +
            " from wvp_common_region" +
            " where device_id in " +
            " <foreach collection='civilCodePoList'  item='item'  open='(' separator=',' close=')' > #{item.code}</foreach>" +
            " </script>")
    Set<String> queryInCivilCodePoList(List<CivilCodePo> civilCodePoList);
}
