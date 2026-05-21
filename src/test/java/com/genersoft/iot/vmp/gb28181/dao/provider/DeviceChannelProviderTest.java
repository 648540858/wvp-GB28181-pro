package com.genersoft.iot.vmp.gb28181.dao.provider;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeviceChannelProviderTest {

    private final DeviceChannelProvider provider = new DeviceChannelProvider();

    @Test
    void queryChannels_withChannelIds_shouldUseBindVariables() {
        Map<String, Object> params = new HashMap<>();
        params.put("channelIds", Arrays.asList("CH001", "CH002", "CH003"));
        String sql = provider.queryChannels(params);
        assertTrue(sql.contains("#{channelIds[0]}"), "should use #{channelIds[0]}");
        assertTrue(sql.contains("#{channelIds[1]}"), "should use #{channelIds[1]}");
        assertTrue(sql.contains("#{channelIds[2]}"), "should use #{channelIds[2]}");
        assertFalse(sql.contains("CH001"), "should not contain raw channel id");
        assertFalse(sql.contains("CH002"), "should not contain raw channel id");
        assertTrue(sql.contains("dc.device_id in ("), "should have IN clause");
    }

    @Test
    void queryChannels_withoutChannelIds_shouldNotContainInClause() {
        Map<String, Object> params = new HashMap<>();
        String sql = provider.queryChannels(params);
        assertFalse(sql.contains("device_id in ("), "should not have IN clause when no channelIds");
        assertTrue(sql.contains("ORDER BY"), "should have ORDER BY");
    }

    @Test
    void queryChannels_withEmptyChannelIds_shouldNotContainInClause() {
        Map<String, Object> params = new HashMap<>();
        params.put("channelIds", Collections.emptyList());
        String sql = provider.queryChannels(params);
        assertFalse(sql.contains("device_id in ("), "should not have IN clause when channelIds empty");
    }

    @Test
    void queryChannels_withDataDeviceId_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("dataDeviceId", 42);
        String sql = provider.queryChannels(params);
        assertTrue(sql.contains("#{dataDeviceId}"), "should use #{dataDeviceId}");
    }

    @Test
    void queryChannels_withQuery_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "test");
        String sql = provider.queryChannels(params);
        assertTrue(sql.contains("#{query}"), "should use #{query} bind variable");
        assertFalse(sql.contains("'test'"), "should not contain raw query value");
    }

    @Test
    void queryChannels_withOnline_shouldFilterStatus() {
        Map<String, Object> params = new HashMap<>();
        params.put("online", true);
        String sql = provider.queryChannels(params);
        assertTrue(sql.contains("'ON'"), "should filter for ON status");
        assertFalse(sql.contains("'OFF'"), "should not filter for OFF status");
    }

    @Test
    void queryChannels_withOffline_shouldFilterStatus() {
        Map<String, Object> params = new HashMap<>();
        params.put("online", false);
        String sql = provider.queryChannels(params);
        assertTrue(sql.contains("'OFF'"), "should filter for OFF status");
        assertFalse(sql.contains("'ON'"), "should not filter for ON status");
    }

    @Test
    void queryChannels_withBusinessGroupId_shouldFilter() {
        Map<String, Object> params = new HashMap<>();
        params.put("businessGroupId", "group-1");
        String sql = provider.queryChannels(params);
        assertTrue(sql.contains("#{businessGroupId}"), "should use bind variable");
    }

    @Test
    void queryChannelsByDeviceDbId_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("dataDeviceId", 99);
        String sql = provider.queryChannelsByDeviceDbId(params);
        assertTrue(sql.contains("#{dataDeviceId}"), "should use #{dataDeviceId}");
    }

    @Test
    void queryChannelsByDeviceDbId_shouldFilterByDataType() {
        Map<String, Object> params = new HashMap<>();
        params.put("dataDeviceId", 1);
        String sql = provider.queryChannelsByDeviceDbId(params);
        assertTrue(sql.contains("data_type = 1"), "should filter by GB28181 data type");
    }

    @Test
    void getOne_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", 123);
        String sql = provider.getOne(params);
        assertTrue(sql.contains("#{id}"), "should use #{id} bind variable");
        assertTrue(sql.contains("where"), "should have WHERE clause");
        assertTrue(sql.contains("#{id}"), "should have bind variable");
    }

    @Test
    void getOneByDeviceId_shouldUseBindVariables() {
        Map<String, Object> params = new HashMap<>();
        params.put("dataDeviceId", 10);
        params.put("channelId", "CH999");
        String sql = provider.getOneByDeviceId(params);
        assertTrue(sql.contains("#{dataDeviceId}"), "should use #{dataDeviceId}");
        assertTrue(sql.contains("#{channelId}"), "should use #{channelId}");
    }

    @Test
    void queryByDeviceId_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("gbDeviceId", "GB-TEST-123");
        String sql = provider.queryByDeviceId(params);
        assertTrue(sql.contains("#{gbDeviceId}"), "should use #{gbDeviceId}");
    }

    @Test
    void queryById_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("gbId", 456);
        String sql = provider.queryById(params);
        assertTrue(sql.contains("#{gbId}"), "should use #{gbId}");
    }

    @Test
    void queryList_withQuery_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "search-term");
        String sql = provider.queryList(params);
        assertTrue(sql.contains("#{query}"), "should use #{query} bind variable");
        assertFalse(sql.contains("search-term"), "should not contain raw query value");
    }

    @Test
    void queryList_withOnline_shouldFilter() {
        Map<String, Object> params = new HashMap<>();
        params.put("online", true);
        String sql = provider.queryList(params);
        assertTrue(sql.contains("'ON'"), "should filter for ON");
    }

    @Test
    void queryList_withHasCivilCode_shouldFilter() {
        Map<String, Object> params = new HashMap<>();
        params.put("hasCivilCode", true);
        String sql = provider.queryList(params);
        assertTrue(sql.contains("civil_code) is not null"), "should filter for not null civil code");
    }

    @Test
    void queryList_withHasGroup_shouldFilter() {
        Map<String, Object> params = new HashMap<>();
        params.put("hasGroup", true);
        String sql = provider.queryList(params);
        assertTrue(sql.contains("parent_id) is not null"), "should filter for not null parent");
    }

    @Test
    void queryChannels_withHasStream_shouldFilter() {
        Map<String, Object> params = new HashMap<>();
        params.put("hasStream", true);
        String sql = provider.queryChannels(params);
        assertTrue(sql.contains("stream_id IS NOT NULL"), "should filter for not null stream_id");
    }
}
