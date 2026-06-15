package com.genersoft.iot.vmp.streamProxy.dao.provider;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StreamProxyProviderTest {

    private final StreamProxyProvider provider = new StreamProxyProvider();

    @Test
    void select_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", 123);
        String sql = provider.select(params);
        assertTrue(sql.contains("#{id}"), "should use #{id} bind variable");
        assertFalse(sql.contains("123"), "should not contain raw value");
        assertTrue(sql.contains("WHERE st.id = #{id}"), "should have proper WHERE clause");
    }

    @Test
    void selectOneByAppAndStream_shouldUseBindVariables() {
        Map<String, Object> params = new HashMap<>();
        params.put("app", "testApp");
        params.put("stream", "testStream");
        String sql = provider.selectOneByAppAndStream(params);
        assertTrue(sql.contains("#{app}"), "should use #{app} bind variable");
        assertTrue(sql.contains("#{stream}"), "should use #{stream} bind variable");
        assertFalse(sql.contains("testApp"), "should not contain raw app value");
        assertFalse(sql.contains("testStream"), "should not contain raw stream value");
    }

    @Test
    void selectForPushingInMediaServer_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("mediaServerId", "server-001");
        String sql = provider.selectForPushingInMediaServer(params);
        assertTrue(sql.contains("#{mediaServerId}"), "should use #{mediaServerId} bind variable");
    }

    @Test
    void selectAll_withQuery_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "test-query");
        String sql = provider.selectAll(params);
        assertTrue(sql.contains("#{query}"), "should use #{query} bind variable");
        assertFalse(sql.contains("test-query"), "should not contain raw query value");
        assertTrue(sql.contains("LIKE concat('%',#{query},'%')"), "should use concat with bind variable");
    }

    @Test
    void selectAll_withMediaServerId_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("mediaServerId", "server-001");
        String sql = provider.selectAll(params);
        assertTrue(sql.contains("#{mediaServerId}"), "should use #{mediaServerId} bind variable");
        assertFalse(sql.contains("server-001"), "should not contain raw server id");
    }

    @Test
    void selectAll_withPullingTrue() {
        Map<String, Object> params = new HashMap<>();
        params.put("pulling", true);
        String sql = provider.selectAll(params);
        assertTrue(sql.contains("st.pulling=1"), "should filter by pulling=1");
    }

    @Test
    void selectAll_withPullingFalse() {
        Map<String, Object> params = new HashMap<>();
        params.put("pulling", false);
        String sql = provider.selectAll(params);
        assertTrue(sql.contains("st.pulling=0"), "should filter by pulling=0");
    }

    @Test
    void selectAll_withoutParams_shouldReturnBaseQuery() {
        Map<String, Object> params = new HashMap<>();
        String sql = provider.selectAll(params);
        assertTrue(sql.contains("FROM wvp_stream_proxy"), "should have FROM clause");
        assertTrue(sql.contains("LEFT join wvp_device_channel"), "should have JOIN clause");
        assertTrue(sql.contains("order by"), "should have ORDER BY");
    }

    @Test
    void getBaseSelectSql_shouldReturnValidSql() {
        String sql = provider.getBaseSelectSql();
        assertTrue(sql.contains("SELECT"), "should start with SELECT");
        assertTrue(sql.contains("FROM wvp_stream_proxy"), "should have FROM");
        assertTrue(sql.contains("LEFT join wvp_device_channel"), "should have LEFT JOIN");
    }
}
