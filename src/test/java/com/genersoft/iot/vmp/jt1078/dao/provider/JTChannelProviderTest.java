package com.genersoft.iot.vmp.jt1078.dao.provider;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JTChannelProviderTest {

    private final JTChannelProvider provider = new JTChannelProvider();

    @Test
    void selectAll_withQuery_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("terminalDbId", 1);
        params.put("query", "test-channel");
        String sql = provider.selectAll(params);
        assertTrue(sql.contains("#{query}"), "should use #{query} bind variable");
        assertFalse(sql.contains("test-channel"), "should not contain raw query value");
        assertTrue(sql.contains("concat('%',#{query},'%')"), "should use concat with bind variable");
        assertTrue(sql.contains("#{terminalDbId}"), "should use #{terminalDbId} bind variable");
    }

    @Test
    void selectAll_withoutQuery_shouldNotContainLike() {
        Map<String, Object> params = new HashMap<>();
        params.put("terminalDbId", 1);
        String sql = provider.selectAll(params);
        assertFalse(sql.contains("LIKE"), "should not contain LIKE clause when no query");
        assertTrue(sql.contains("#{terminalDbId}"), "should still have terminalDbId condition");
    }

    @Test
    void selectChannelByChannelId_shouldUseBindVariables() {
        Map<String, Object> params = new HashMap<>();
        params.put("terminalDbId", 5);
        params.put("channelId", 100);
        String sql = provider.selectChannelByChannelId(params);
        assertTrue(sql.contains("#{terminalDbId}"), "should use #{terminalDbId}");
        assertTrue(sql.contains("#{channelId}"), "should use #{channelId}");
    }

    @Test
    void selectChannelById_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", 42);
        String sql = provider.selectChannelById(params);
        assertTrue(sql.contains("#{id}"), "should use #{id} bind variable");
    }

    @Test
    void selectAll_shouldOrderByChannelId() {
        Map<String, Object> params = new HashMap<>();
        params.put("terminalDbId", 1);
        String sql = provider.selectAll(params);
        assertTrue(sql.contains("ORDER BY jc.channel_id"), "should order by channel_id");
    }

    @Test
    void baseSql_shouldHaveJoins() {
        assertTrue(JTChannelProvider.BASE_SQL.contains("LEFT join wvp_device_channel"), "should have LEFT JOIN");
        assertTrue(JTChannelProvider.BASE_SQL.contains("wvp_jt_channel"), "should query from jt_channel");
    }
}
