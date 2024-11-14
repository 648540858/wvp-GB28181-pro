package com.genersoft.iot.vmp.streamProxy.dao.provider;

import java.util.Map;

public class StreamProxyProvider {

    public String getBaseSelectSql(){
        return "SELECT " +
                " st.*, " +
                " st.id as stream_proxy_id, " +
                " wdc.*, " +
                " wdc.id as gb_id" +
                " FROM wvp_stream_proxy st " +
                " LEFT join wvp_device_channel wdc " +
                " on st.id = wdc.stream_proxy_id ";
    }

    public String select(Map<String, Object> params ){
        return getBaseSelectSql() + " WHERE st.id = " + params.get("id");
    }

    public String selectForEnableInMediaServer(Map<String, Object> params ){
        return getBaseSelectSql() + String.format(" WHERE st.enable=%s and st.media_server_id= '%s' order by st.create_time desc",
                params.get("enable"), params.get("mediaServerId"));
    }

    public String selectOneByAppAndStream(Map<String, Object> params ){
        return getBaseSelectSql() + String.format(" WHERE st.app='%s' AND st.stream='%s' order by st.create_time desc",
                params.get("app"), params.get("stream"));
    }

    public String selectAll(Map<String, Object> params ){
        StringBuilder sqlBuild = new StringBuilder();
        sqlBuild.append(getBaseSelectSql());
        sqlBuild.append(" WHERE 1=1 ");
        if (params.get("query") != null) {
            sqlBuild.append(" AND ")
                    .append(" (")
                    .append(" st.app LIKE ").append("'%").append(params.get("query")).append("%' escape '/'")
                    .append(" OR")
                    .append(" st.stream LIKE ").append("'%").append(params.get("query")).append("%' escape '/'")
                    .append(" OR")
                    .append(" wdc.gb_device_id LIKE ").append("'%").append(params.get("query")).append("%' escape '/'")
                    .append(" OR")
                    .append(" wdc.gb_name LIKE ").append("'%").append(params.get("query")).append("%' escape '/'")
                    .append(" )")
            ;
        }
        Object pulling = params.get("pulling");
        if (pulling != null) {
            if ((Boolean) pulling) {
                sqlBuild.append(" AND st.pulling=1 ");
            }else {
                sqlBuild.append(" AND st.pulling=0 ");
            }
        }
        if (params.get("mediaServerId") != null) {
            sqlBuild.append(" AND st.media_server_id='").append(params.get("mediaServerId")).append("'");
        }
        sqlBuild.append(" order by st.create_time desc");
        return sqlBuild.toString();
    }
}
