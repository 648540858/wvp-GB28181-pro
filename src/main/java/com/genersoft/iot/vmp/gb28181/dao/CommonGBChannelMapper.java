package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.provider.ChannelProvider;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Mapper
@Repository
public interface CommonGBChannelMapper {


    @SelectProvider(type = ChannelProvider.class, method = "queryByDeviceId")
    CommonGBChannel queryByDeviceId(@Param("gbDeviceId") String gbDeviceId);

    @Insert(" <script>" +
            "INSERT INTO wvp_device_channel (" +
            "gb_device_id," +
            " <if test='streamProxyId != null' > stream_proxy_id,</if>" +
            " <if test='streamPushId != null' > stream_push_id,</if>" +
            "create_time," +
            "update_time," +
            "gb_name," +
            "gb_manufacturer," +
            "gb_model," +
            "gb_owner," +
            "gb_civil_code," +
            "gb_block," +
            "gb_address," +
            "gb_parental," +
            "gb_parent_id ," +
            "gb_safety_way," +
            "gb_register_way," +
            "gb_cert_num," +
            "gb_certifiable," +
            "gb_err_code," +
            "gb_end_time," +
            "gb_secrecy," +
            "gb_ip_address," +
            "gb_port," +
            "gb_password," +
            "gb_status," +
            "gb_longitude," +
            "gb_latitude," +
            "gb_ptz_type," +
            "gb_position_type," +
            "gb_room_type," +
            "gb_use_type," +
            "gb_supply_light_type," +
            "gb_direction_type," +
            "gb_resolution," +
            "gb_business_group_id," +
            "gb_download_speed," +
            "gb_svc_space_support_mod," +
            "gb_svc_time_support_mode ) " +
            "VALUES (" +
            "#{gbDeviceId}, " +
            " <if test='streamProxyId != null' > #{streamProxyId},</if>" +
            " <if test='streamPushId != null' > #{streamPushId},</if>" +
            "#{createTime}, " +
            "#{updateTime}, " +
            "#{gbName}, " +
            "#{gbManufacturer}, " +
            "#{gbModel}, " +
            "#{gbOwner}, " +
            "#{gbCivilCode}, " +
            "#{gbBlock}, " +
            "#{gbAddress}, " +
            "#{gbParental}, " +
            "#{gbParentId}, " +
            "#{gbSafetyWay}, " +
            "#{gbRegisterWay}, " +
            "#{gbCertNum}, " +
            "#{gbCertifiable}, " +
            "#{gbErrCode}, " +
            "#{gbEndTime}, " +
            "#{gbSecrecy},"+
            "#{gbIpAddress},"+
            "#{gbPort},"+
            "#{gbPassword},"+
            "#{gbStatus},"+
            "#{gbLongitude},"+
            "#{gbLatitude},"+
            "#{gbPtzType},"+
            "#{gbPositionType},"+
            "#{gbRoomType},"+
            "#{gbUseType},"+
            "#{gbSupplyLightType},"+
            "#{gbDirectionType},"+
            "#{gbResolution},"+
            "#{gbBusinessGroupId},"+
            "#{gbDownloadSpeed},"+
            "#{gbSvcSpaceSupportMod},"+
            "#{gbSvcTimeSupportMode}"+
            ")" +
            " </script>")
    @Options(useGeneratedKeys = true, keyProperty = "gbId", keyColumn = "id")
    int insert(CommonGBChannel commonGBChannel);

    @SelectProvider(type = ChannelProvider.class, method = "queryById")
    CommonGBChannel queryById(@Param("gbId") int gbId);

    @Delete(value = {"delete from wvp_device_channel where id = #{gbId} "})
    void delete(int gbId);

    @Update(value = {" <script>" +
            "UPDATE wvp_device_channel " +
            "SET update_time=#{updateTime}" +
            ", gb_device_id = #{gbDeviceId}" +
            ", gb_name = #{gbName}" +
            ", gb_manufacturer = #{gbManufacturer}" +
            ", gb_model = #{gbModel}" +
            ", gb_owner = #{gbOwner}" +
            ", gb_civil_code = #{gbCivilCode}" +
            ", gb_block = #{gbBlock}" +
            ", gb_address = #{gbAddress}" +
            ", gb_parental = #{gbParental}" +
            ", gb_safety_way = #{gbSafetyWay}" +
            ", gb_register_way = #{gbRegisterWay}" +
            ", gb_cert_num = #{gbCertNum}" +
            ", gb_certifiable = #{gbCertifiable}" +
            ", gb_err_code = #{gbErrCode}" +
            ", gb_end_time = #{gbEndTime}" +
            ", gb_ip_address = #{gbIpAddress}" +
            ", gb_port = #{gbPort}" +
            ", gb_password = #{gbPassword}" +
            ", gb_status = #{gbStatus}" +
            ", gb_longitude = #{gbLongitude}" +
            ", gb_latitude = #{gbLatitude}" +
            ", gb_ptz_type = #{gbPtzType}" +
            ", gb_position_type = #{gbPositionType}" +
            ", gb_room_type = #{gbRoomType}" +
            ", gb_use_type = #{gbUseType}" +
            ", gb_supply_light_type = #{gbSupplyLightType}" +
            ", gb_direction_type = #{gbDirectionType}" +
            ", gb_resolution = #{gbResolution}" +
            ", gb_business_group_id = #{gbBusinessGroupId}" +
            ", gb_download_speed = #{gbDownloadSpeed}" +
            ", gb_svc_space_support_mod = #{gbSvcSpaceSupportMod}" +
            ", gb_svc_time_support_mode = #{gbSvcTimeSupportMode}" +
            " WHERE id = #{gbId}"+
            " </script>"})
    int update(CommonGBChannel commonGBChannel);

    @Update(value = {" <script>" +
            " UPDATE wvp_device_channel " +
            " SET gb_status = #{status}" +
            " WHERE id = #{gbId}"+
            " </script>"})
    int updateStatusById(@Param("gbId") int gbId, @Param("status") int status);

    @Update("<script> " +
            "<foreach collection='commonGBChannels' index='index' item='item' separator=';'> " +
            "UPDATE wvp_device_channel SET gb_status = #{status} WHERE id = #{item.gbId}" +
            "</foreach> " +
            "</script>")
    int updateStatusForListById(List<CommonGBChannel> commonGBChannels, @Param("status") String status);

    @SelectProvider(type = ChannelProvider.class, method = "queryInListByStatus")
    List<CommonGBChannel> queryInListByStatus(List<CommonGBChannel> commonGBChannelList, @Param("status") String status);


    @Insert(" <script>" +
            "INSERT INTO wvp_device_channel (" +
            "gb_device_id," +
            "stream_proxy_id, " +
            "stream_push_id," +
            "create_time," +
            "update_time," +
            "gb_name," +
            "gb_manufacturer," +
            "gb_model," +
            "gb_owner," +
            "gb_civil_code," +
            "gb_block," +
            "gb_address," +
            "gb_parental," +
            "gb_parent_id ," +
            "gb_safety_way," +
            "gb_register_way," +
            "gb_cert_num," +
            "gb_certifiable," +
            "gb_err_code," +
            "gb_end_time," +
            "gb_secrecy," +
            "gb_ip_address," +
            "gb_port," +
            "gb_password," +
            "gb_status," +
            "gb_longitude," +
            "gb_latitude," +
            "gb_ptz_type," +
            "gb_position_type," +
            "gb_room_type," +
            "gb_use_type," +
            "gb_supply_light_type," +
            "gb_direction_type," +
            "gb_resolution," +
            "gb_business_group_id," +
            "gb_download_speed," +
            "gb_svc_space_support_mod," +
            "gb_svc_time_support_mode ) " +
            "VALUES" +
            "<foreach collection='commonGBChannels' index='index' item='item' separator=','> " +
            "(#{item.gbDeviceId}, #{item.streamProxyId}, #{item.streamPushId},#{item.createTime},#{item.updateTime}," +
            "#{item.gbName},#{item.gbManufacturer}, #{item.gbModel}," +
            "#{item.gbOwner},#{item.gbCivilCode},#{item.gbBlock}, #{item.gbAddress}, #{item.gbParental}, #{item.gbParentId},#{item.gbSafetyWay}, " +
            "#{item.gbRegisterWay},#{item.gbCertNum},#{item.gbCertifiable},#{item.gbErrCode},#{item.gbEndTime}, #{item.gbSecrecy},#{item.gbIpAddress}," +
            "#{item.gbPort},#{item.gbPassword},#{item.gbStatus},#{item.gbLongitude}, #{item.gbLatitude},#{item.gbPtzType},#{item.gbPositionType},#{item.gbRoomType}," +
            "#{item.gbUseType},#{item.gbSupplyLightType},#{item.gbDirectionType},#{item.gbResolution},#{item.gbBusinessGroupId},#{item.gbDownloadSpeed}," +
            "#{item.gbSvcSpaceSupportMod},#{item.gbSvcTimeSupportMode})" +
            "</foreach> " +
            " </script>")
    int batchAdd(List<CommonGBChannel> commonGBChannels);

    @Update("<script> " +
            "<foreach collection='commonGBChannels' index='index' item='item' separator=';'> " +
            "UPDATE wvp_device_channel SET gb_status = #{item.gbStatus} WHERE id = #{item.gbId}" +
            "</foreach> " +
            "</script>")
    int updateStatus(List<CommonGBChannel> commonGBChannels);

    @Update(value = {" <script>" +
            " UPDATE wvp_device_channel " +
            " SET update_time=#{updateTime}, gb_device_id = null, gb_name = null, gb_manufacturer = null," +
            " gb_model = null, gb_owner = null, gb_block = null, gb_address = null," +
            " gb_parental = null, gb_parent_id = null, gb_safety_way = null, gb_register_way = null, gb_cert_num = null," +
            " gb_certifiable = null, gb_err_code = null, gb_end_time = null, gb_secrecy = null, gb_ip_address = null, " +
            " gb_port = null, gb_password = null, gb_status = null, gb_longitude = null, gb_latitude = null, " +
            " gb_ptz_type = null, gb_position_type = null, gb_room_type = null, gb_use_type = null, gb_supply_light_type = null, " +
            " gb_direction_type = null, gb_resolution = null, gb_business_group_id = null, gb_download_speed = null, gb_svc_space_support_mod = null, " +
            " gb_svc_time_support_mode = null" +
            " WHERE id = #{id} and device_db_id = #{gbDeviceDbId}"+
            " </script>"})
    void reset(@Param("id") int id, @Param("gbDeviceDbId") int gbDeviceDbId, @Param("updateTime") String updateTime);


    @SelectProvider(type = ChannelProvider.class, method = "queryByIds")
    List<CommonGBChannel> queryByIds(Collection<Integer> ids);

    @Delete(value = {" <script>" +
            " delete from wvp_device_channel" +
            " where 1 = 1 and id in " +
            " <foreach collection='channelListInDb'  item='item'  open='(' separator=',' close=')' > #{item.gbId}</foreach>" +
            "</script>"})
    void batchDelete(List<CommonGBChannel> channelListInDb);

    @SelectProvider(type = ChannelProvider.class, method = "queryByStreamPushId")
    CommonGBChannel queryByStreamPushId(@Param("streamPushId") Integer streamPushId);

    @SelectProvider(type = ChannelProvider.class, method = "queryByStreamProxyId")
    CommonGBChannel queryByStreamProxyId(@Param("streamProxyId") Integer streamProxyId);

    @SelectProvider(type = ChannelProvider.class, method = "queryList")
    List<CommonGBChannel> queryList(@Param("query") String query, @Param("online") Boolean online,
                                    @Param("hasCivilCode") Boolean hasCivilCode,
                                    @Param("hasGroup") Boolean hasGroup);

    @Select("<script>" +
            " select " +
            "    id," +
            "    concat('channel', id) as tree_id," +
            "    coalesce(gb_device_id, device_id) as device_id," +
            "    coalesce(gb_name, name) as name, " +
            "    coalesce(gb_parent_id, parent_id) as parent_device_id, " +
            "    1 as type, " +
            "    true as is_leaf " +
            " from wvp_device_channel " +
            " where coalesce(gb_civil_code, civil_code) = #{parentDeviceId} " +
            " <if test='query != null'> AND (coalesce(gb_device_id, device_id) LIKE concat('%',#{query},'%') " +
            " OR coalesce(gb_name, name) LIKE concat('%',#{query},'%'))</if> " +
            " </script>")
    List<RegionTree> queryForRegionTreeByCivilCode(@Param("query") String query, @Param("parentDeviceId") String parentDeviceId);

    @Update(value = {" <script>" +
            " UPDATE wvp_device_channel " +
            " SET gb_civil_code = null, civil_code = null" +
            " WHERE gb_civil_code in "+
            " <foreach collection='allChildren'  item='item'  open='(' separator=',' close=')' > #{item.deviceId}</foreach>" +
            " </script>"})
    int removeCivilCode(List<Region> allChildren);


    @Update(value = {" <script>" +
            " UPDATE wvp_device_channel " +
            " SET gb_civil_code = #{civilCode}" +
            " WHERE id in "+
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.gbId}</foreach>" +
            " </script>"})
    int updateRegion(@Param("civilCode") String civilCode, @Param("channelList") List<CommonGBChannel> channelList);

    @SelectProvider(type = ChannelProvider.class, method = "queryByIdsOrCivilCode")
    List<CommonGBChannel> queryByIdsOrCivilCode(@Param("civilCode") String civilCode, @Param("ids") List<Integer> ids);

    @Update(value = {" <script>" +
            " UPDATE wvp_device_channel " +
            " SET gb_civil_code = null, civil_code = null" +
            " WHERE id in "+
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.gbId}</foreach>" +
            " </script>"})
    int removeCivilCodeByChannels(List<CommonGBChannel> channelList);

    @SelectProvider(type = ChannelProvider.class, method = "queryByCivilCode")
    List<CommonGBChannel> queryByCivilCode(@Param("civilCode") String civilCode);

    @SelectProvider(type = ChannelProvider.class, method = "queryByGbDeviceIds")
    List<CommonGBChannel> queryByGbDeviceIds(List<Integer> deviceIds);

    @Select(value = {" <script>" +
            " select id from wvp_device_channel " +
            " where channel_type = 0 and device_db_id in  "+
            " <foreach collection='deviceIds'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            " </script>"})
    List<Integer> queryByGbDeviceIdsForIds(List<Integer> deviceIds);

    @SelectProvider(type = ChannelProvider.class, method = "queryByGroupList")
    List<CommonGBChannel> queryByGroupList(List<Group> groupList);

    @Update(value = {" <script>" +
            " UPDATE wvp_device_channel " +
            " SET gb_parent_id = null, gb_business_group_id = null, parent_id = null, business_group_id = null" +
            " WHERE id in "+
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.gbId}</foreach>" +
            " </script>"})
    int removeParentIdByChannels(List<CommonGBChannel> channelList);

    @SelectProvider(type = ChannelProvider.class, method = "queryByBusinessGroup")
    List<CommonGBChannel> queryByBusinessGroup(@Param("businessGroup") String businessGroup);

    @SelectProvider(type = ChannelProvider.class, method = "queryByParentId")
    List<CommonGBChannel> queryByParentId(@Param("parentId") String parentId);

    @Update(value = {" <script>" +
            " UPDATE wvp_device_channel " +
            " SET gb_business_group_id = #{businessGroup}" +
            " WHERE id in "+
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.gbId}</foreach>" +
            " </script>"})
    int updateBusinessGroupByChannelList(@Param("businessGroup") String businessGroup, List<CommonGBChannel> channelList);

    @Update(value = {" <script>" +
            " UPDATE wvp_device_channel " +
            " SET gb_parent_id = #{parentId}" +
            " WHERE id in "+
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.gbId}</foreach>" +
            " </script>"})
    int updateParentIdByChannelList(@Param("parentId") String parentId, List<CommonGBChannel> channelList);

    @Select("<script>" +
            " select " +
            "    id," +
            "    concat('channel', id) as tree_id," +
            "    coalesce(gb_device_id, device_id) as device_id," +
            "    coalesce(gb_name, name) as name, " +
            "    coalesce(gb_parent_id, parent_id) as parent_device_id, " +
            "    coalesce(gb_business_group_id, business_group_id) as business_group, " +
            "    1 as type, " +
            "    true as is_leaf " +
            " from wvp_device_channel " +
            " where channel_type = 0 and coalesce(gb_parent_id, parent_id) = #{parent} " +
            " <if test='query != null'> AND (coalesce(gb_device_id, device_id) LIKE concat('%',#{query},'%') " +
            " OR coalesce(gb_name, name) LIKE concat('%',#{query},'%'))</if> " +
            " </script>")
    List<GroupTree> queryForGroupTreeByParentId(@Param("query") String query, @Param("parent") String parent);

    @Update(value = {" <script>" +
            " UPDATE wvp_device_channel " +
            " SET gb_parent_id = #{parentId}, gb_business_group_id = #{businessGroup}" +
            " WHERE id in "+
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.gbId}</foreach>" +
            " </script>"})
    int updateGroup(@Param("parentId") String parentId, @Param("businessGroup") String businessGroup,
                    List<CommonGBChannel> channelList);

    @Update({"<script>" +
            "<foreach collection='commonGBChannels' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_device_channel" +
            " SET update_time=#{item.updateTime}" +
            ", gb_device_id=#{item.gbDeviceId}" +
            ", gb_name=#{item.gbName}" +
            ", gb_manufacturer=#{item.gbManufacturer}" +
            ", gb_model=#{item.gbModel}" +
            ", gb_owner=#{item.gbOwner}" +
            ", gb_civil_code=#{item.gbCivilCode}" +
            ", gb_block=#{item.gbBlock}" +
            ", gb_address=#{item.gbAddress}" +
            ", gb_parental=#{item.gbParental}" +
            ", gb_safety_way=#{item.gbSafetyWay}" +
            ", gb_register_way=#{item.gbRegisterWay}" +
            ", gb_cert_num=#{item.gbCertNum}" +
            ", gb_certifiable=#{item.gbCertifiable}" +
            ", gb_err_code=#{item.gbErrCode}" +
            ", gb_end_time=#{item.gbEndTime}" +
            ", gb_ip_address=#{item.gbIpAddress}" +
            ", gb_port=#{item.gbPort}" +
            ", gb_password=#{item.gbPassword}" +
            ", gb_status=#{item.gbStatus}" +
            ", gb_longitude=#{item.gbLongitude}" +
            ", gb_latitude=#{item.gbLatitude}" +
            ", gb_ptz_type=#{item.gbPtzType}" +
            ", gb_position_type=#{item.gbPositionType}" +
            ", gb_room_type=#{item.gbRoomType}" +
            ", gb_use_type=#{item.gbUseType}" +
            ", gb_supply_light_type=#{item.gbSupplyLightType}" +
            ", gb_direction_type=#{item.gbDirectionType}" +
            ", gb_resolution=#{item.gbResolution}" +
            ", gb_business_group_id=#{item.gbBusinessGroupId}" +
            ", gb_download_speed=#{item.gbDownloadSpeed}" +
            ", gb_svc_space_support_mod=#{item.gbSvcSpaceSupportMod}" +
            ", gb_svc_time_support_mode=#{item.gbSvcTimeSupportMode}" +
            " WHERE id=#{item.gbId}" +
            "</foreach>" +
            "</script>"})
    int batchUpdate(List<CommonGBChannel> commonGBChannels);

    @SelectProvider(type = ChannelProvider.class, method = "queryWithPlatform")
    List<CommonGBChannel> queryWithPlatform(@Param("platformId") Integer platformId);

    @SelectProvider(type = ChannelProvider.class, method = "queryShareChannelByParentId")
    List<CommonGBChannel> queryShareChannelByParentId(@Param("parentId") String parentId, @Param("platformId") Integer platformId);

    @SelectProvider(type = ChannelProvider.class, method = "queryShareChannelByCivilCode")
    List<CommonGBChannel> queryShareChannelByCivilCode(@Param("civilCode") String civilCode, @Param("platformId") Integer platformId);

    @Update(value = {" <script>" +
            " UPDATE wvp_device_channel " +
            " SET gb_civil_code = #{civilCode}" +
            " WHERE id in "+
            " <foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.gbId}</foreach>" +
            " </script>"})
    int updateCivilCodeByChannelList(@Param("civilCode") String civilCode, List<CommonGBChannel> channelList);

    @SelectProvider(type = ChannelProvider.class, method = "queryListByStreamPushList")
    List<CommonGBChannel> queryListByStreamPushList(List<StreamPush> streamPushList);

}
