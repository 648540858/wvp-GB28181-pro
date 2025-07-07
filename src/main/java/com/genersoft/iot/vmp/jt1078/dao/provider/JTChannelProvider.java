package com.genersoft.iot.vmp.jt1078.dao.provider;

import java.util.Map;

public class JTChannelProvider {

    public final static String BASE_SQL =
            "SELECT jc.*, jc.id as data_device_id, wdc.*,  wdc.id as gb_id " +
                    " from wvp_jt_channel jc " +
                    " LEFT join wvp_device_channel wdc " +
                    " on jc.id = wdc.data_device_id and wdc.data_type = 200 ";

    public String selectChannelByChannelId(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append(" WHERE jc.terminal_db_id = #{terminalDbId} and jc.channel_id = #{channelId} ");
        return sqlBuild.toString();
    }
    public String selectChannelById(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append(" WHERE jc.id = #{id}");
        return sqlBuild.toString();
    }
    public String selectAll(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(BASE_SQL);
        sqlBuild.append(" WHERE jc.terminal_db_id = #{terminalDbId} ");
        if (params.get("query") != null) {
            sqlBuild.append(" AND ")
                    .append(" jc.name LIKE ").append("'%").append(params.get("query")).append("%'")
            ;
        }
        sqlBuild.append(" ORDER BY jc.channel_id ");
        return sqlBuild.toString();
    }
}
