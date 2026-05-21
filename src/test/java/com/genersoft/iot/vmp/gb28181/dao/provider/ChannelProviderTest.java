package com.genersoft.iot.vmp.gb28181.dao.provider;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.web.custom.bean.CameraGroup;
import com.genersoft.iot.vmp.web.custom.bean.Point;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ChannelProviderTest {

    private final ChannelProvider provider = new ChannelProvider();

    // ========== queryByGbDeviceIds ==========

    @Test
    void queryByGbDeviceIds_shouldUseBindVariables() {
        Map<String, Object> params = new HashMap<>();
        params.put("deviceIds", Arrays.asList("DEV001", "DEV002"));
        String sql = provider.queryByGbDeviceIds(params);
        assertTrue(sql.contains("#{deviceIds[0]}"), "should use #{deviceIds[0]}");
        assertTrue(sql.contains("#{deviceIds[1]}"), "should use #{deviceIds[1]}");
        assertFalse(sql.contains("'DEV001'"), "should not contain raw quoted value");
        assertFalse(sql.contains("'DEV002'"), "should not contain raw quoted value");
    }

    @Test
    void queryByGbDeviceIds_shouldNotQuoteBindVariables() {
        Map<String, Object> params = new HashMap<>();
        params.put("deviceIds", Collections.singletonList("INJECT' OR 1=1 --"));
        String sql = provider.queryByGbDeviceIds(params);
        assertTrue(sql.contains("#{deviceIds[0]}"), "should use bind variable for injection attempt");
        assertFalse(sql.contains("1=1"), "should not contain injection payload in SQL");
    }

    // ========== queryByGroupList ==========

    @Test
    void queryByGroupList_shouldUseBindVariables() {
        Map<String, Object> params = new HashMap<>();
        Group g1 = new Group();
        g1.setDeviceId("GRP001");
        Group g2 = new Group();
        g2.setDeviceId("GRP002");
        params.put("groupList", Arrays.asList(g1, g2));
        String sql = provider.queryByGroupList(params);
        assertTrue(sql.contains("#{groupList[0].deviceId}"), "should use #{groupList[0].deviceId}");
        assertTrue(sql.contains("#{groupList[1].deviceId}"), "should use #{groupList[1].deviceId}");
        assertFalse(sql.contains("GRP001"), "should not contain raw deviceId");
        assertFalse(sql.contains("GRP002"), "should not contain raw deviceId");
    }

    // ========== queryOnlineListsByGbDeviceIds ==========

    @Test
    void queryOnlineListsByGbDeviceIds_shouldUseBindVariables() {
        Map<String, Object> params = new HashMap<>();
        Device d1 = new Device();
        d1.setId(101);
        Device d2 = new Device();
        d2.setId(102);
        params.put("deviceList", Arrays.asList(d1, d2));
        String sql = provider.queryOnlineListsByGbDeviceIds(params);
        assertTrue(sql.contains("#{deviceList[0].id}"), "should use #{deviceList[0].id}");
        assertTrue(sql.contains("#{deviceList[1].id}"), "should use #{deviceList[1].id}");
        assertFalse(sql.contains("101"), "should not contain raw id");
        assertFalse(sql.contains("102"), "should not contain raw id");
    }

    @Test
    void queryOnlineListsByGbDeviceIds_withEmptyList_shouldNotHaveInClause() {
        Map<String, Object> params = new HashMap<>();
        params.put("deviceList", Collections.emptyList());
        String sql = provider.queryOnlineListsByGbDeviceIds(params);
        assertFalse(sql.contains("data_device_id in ("), "should not have IN clause when empty");
    }

    @Test
    void queryOnlineListsByGbDeviceIds_withNullList_shouldNotHaveInClause() {
        Map<String, Object> params = new HashMap<>();
        params.put("deviceList", null);
        String sql = provider.queryOnlineListsByGbDeviceIds(params);
        assertFalse(sql.contains("data_device_id in ("), "should not have IN clause when null");
    }

    // ========== queryListWithChildForSy ==========

    @Test
    void queryListWithChildForSy_shouldUseBindVariables() {
        Map<String, Object> params = new HashMap<>();
        CameraGroup cg1 = new CameraGroup();
        cg1.setDeviceId("CG001");
        CameraGroup cg2 = new CameraGroup();
        cg2.setDeviceId("CG002");
        params.put("groupList", Arrays.asList(cg1, cg2));
        String sql = provider.queryListWithChildForSy(params);
        assertTrue(sql.contains("#{groupList[0].deviceId}"), "should use #{groupList[0].deviceId}");
        assertTrue(sql.contains("#{groupList[1].deviceId}"), "should use #{groupList[1].deviceId}");
        assertFalse(sql.contains("'CG001'"), "should not contain raw quoted value");
    }

    @Test
    void queryListWithChildForSy_withQuery_shouldUseBindVariable() {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "search-term");
        params.put("groupList", Collections.singletonList(new CameraGroup()));
        String sql = provider.queryListWithChildForSy(params);
        assertTrue(sql.contains("#{query}"), "should use #{query} bind variable");
        assertFalse(sql.contains("search-term"), "should not contain raw query");
    }

    @Test
    void queryListWithChildForSy_withSort_shouldUseWhitelist() {
        Map<String, Object> params = new HashMap<>();
        params.put("groupList", Collections.singletonList(new CameraGroup()));
        params.put("sortName", "gbId");
        params.put("order", true);
        String sql = provider.queryListWithChildForSy(params);
        assertTrue(sql.contains("order by gb_id"), "should sort by gb_id");
        assertTrue(sql.contains("ASC"), "should be ascending");
    }

    @Test
    void queryListWithChildForSy_withSortDesc_shouldUseDesc() {
        Map<String, Object> params = new HashMap<>();
        params.put("groupList", Collections.singletonList(new CameraGroup()));
        params.put("sortName", "gbId");
        params.put("order", false);
        String sql = provider.queryListWithChildForSy(params);
        assertTrue(sql.contains("DESC"), "should be descending");
    }

    // ========== queryListInBox ==========

    @Test
    void queryListInBox_shouldUseBindVariables() {
        Map<String, Object> params = new HashMap<>();
        CameraGroup cg = new CameraGroup();
        cg.setDeviceId("BOX001");
        params.put("groupList", Collections.singletonList(cg));
        params.put("level", 3);
        String sql = provider.queryListInBox(params);
        assertTrue(sql.contains("#{groupList[0].deviceId}"), "should use bind variable");
        assertFalse(sql.contains("'BOX001'"), "should not contain raw value");
        assertTrue(sql.contains("#{level}"), "should use #{level} bind variable");
        assertTrue(sql.contains("#{minLongitude}"), "should use #{minLongitude}");
        assertTrue(sql.contains("#{maxLatitude}"), "should use #{maxLatitude}");
    }

    // ========== queryListInCircleForMysql ==========

    @Test
    void queryListInCircleForMysql_shouldUseBindVariablesForGeometry() {
        Map<String, Object> params = new HashMap<>();
        CameraGroup cg = new CameraGroup();
        cg.setDeviceId("CIRCLE001");
        params.put("groupList", Collections.singletonList(cg));
        params.put("centerLongitude", 116.397);
        params.put("centerLatitude", 39.908);
        params.put("radius", 1000);

        String sql = provider.queryListInCircleForMysql(params);
        assertTrue(sql.contains("#{centerLongitude}"), "should use #{centerLongitude} bind variable");
        assertTrue(sql.contains("#{centerLatitude}"), "should use #{centerLatitude} bind variable");
        assertTrue(sql.contains("#{radius}"), "should use #{radius} bind variable");
        assertFalse(sql.contains("116.397"), "should not contain raw longitude");
        assertFalse(sql.contains("39.908"), "should not contain raw latitude");
        assertTrue(sql.contains("CONCAT('point(', #{centerLongitude}, ' ', #{centerLatitude}, ')')"),
                "should build WKT via CONCAT with bind variables");
    }

    // ========== queryListInCircleForKingBase ==========

    @Test
    void queryListInCircleForKingBase_shouldUseBindVariablesForGeometry() {
        Map<String, Object> params = new HashMap<>();
        CameraGroup cg = new CameraGroup();
        cg.setDeviceId("CIRCLE002");
        params.put("groupList", Collections.singletonList(cg));
        params.put("centerLongitude", 121.473);
        params.put("centerLatitude", 31.230);
        params.put("radius", 500);

        String sql = provider.queryListInCircleForKingBase(params);
        assertTrue(sql.contains("#{centerLongitude}"), "should use #{centerLongitude}");
        assertTrue(sql.contains("#{centerLatitude}"), "should use #{centerLatitude}");
        assertTrue(sql.contains("#{radius}"), "should use #{radius}");
        assertFalse(sql.contains("121.473"), "should not contain raw longitude");
        assertFalse(sql.contains("31.230"), "should not contain raw latitude");
        assertTrue(sql.contains("CONCAT('point(', #{centerLongitude}, ' ', #{centerLatitude}, ')')"),
                "should build WKT via CONCAT with bind variables");
    }

    // ========== queryListInPolygonForMysql ==========

    @Test
    void queryListInPolygonForMysql_shouldUseBindVariablesForPoints() {
        Map<String, Object> params = new HashMap<>();
        CameraGroup cg = new CameraGroup();
        cg.setDeviceId("POLY001");
        params.put("groupList", Collections.singletonList(cg));

        List<Point> points = new ArrayList<>();
        Point p1 = new Point();
        p1.setLng(116.0);
        p1.setLat(39.0);
        Point p2 = new Point();
        p2.setLng(117.0);
        p2.setLat(40.0);
        points.add(p1);
        points.add(p2);
        params.put("pointList", points);

        String sql = provider.queryListInPolygonForMysql(params);
        assertTrue(sql.contains("#{pointList[0].lng}"), "should use #{pointList[0].lng}");
        assertTrue(sql.contains("#{pointList[0].lat}"), "should use #{pointList[0].lat}");
        assertTrue(sql.contains("#{pointList[1].lng}"), "should use #{pointList[1].lng}");
        assertTrue(sql.contains("#{pointList[1].lat}"), "should use #{pointList[1].lat}");
        assertFalse(sql.contains("116.0"), "should not contain raw lng");
        assertFalse(sql.contains("117.0"), "should not contain raw lat");
        assertTrue(sql.contains("CONCAT('POLYGON(('"), "should use CONCAT to build polygon WKT");
    }

    // ========== queryListInPolygonForKingBase ==========

    @Test
    void queryListInPolygonForKingBase_shouldUseBindVariablesForPoints() {
        Map<String, Object> params = new HashMap<>();
        CameraGroup cg = new CameraGroup();
        cg.setDeviceId("POLY002");
        params.put("groupList", Collections.singletonList(cg));

        List<Point> points = new ArrayList<>();
        Point p1 = new Point();
        p1.setLng(116.0);
        p1.setLat(39.0);
        points.add(p1);
        params.put("pointList", points);

        String sql = provider.queryListInPolygonForKingBase(params);
        assertTrue(sql.contains("#{pointList[0].lng}"), "should use #{pointList[0].lng}");
        assertTrue(sql.contains("#{pointList[0].lat}"), "should use #{pointList[0].lat}");
        assertFalse(sql.contains("116.0"), "should not contain raw lng");
        assertFalse(sql.contains("39.0"), "should not contain raw lat");
        assertTrue(sql.contains("ST_MakePoint"), "should use KingBase specific function");
    }

    // ========== queryListInCircleForMysql with injection attempt ==========

    @Test
    void queryListInCircleForMysql_shouldNotContainInjectionPayload() {
        Map<String, Object> params = new HashMap<>();
        CameraGroup cg = new CameraGroup();
        cg.setDeviceId("NORMAL");
        params.put("groupList", Collections.singletonList(cg));
        params.put("centerLongitude", "0) OR 1=1 -- ");
        params.put("centerLatitude", "0");
        params.put("radius", 1000);

        String sql = provider.queryListInCircleForMysql(params);
        assertTrue(sql.contains("#{centerLongitude}"), "should use bind variable for injection payload");
        assertFalse(sql.contains("1=1"), "should not contain 1=1 in SQL text");
        assertFalse(sql.contains("OR 1=1"), "should not contain injection");
    }

    // ========== queryByGbDeviceIds single element ==========

    @Test
    void queryByGbDeviceIds_withSingleElement() {
        Map<String, Object> params = new HashMap<>();
        params.put("deviceIds", Collections.singletonList("SINGLE01"));
        String sql = provider.queryByGbDeviceIds(params);
        assertEquals(1, countOccurrences(sql, "#{deviceIds[0]}"),
                "should have exactly one bind variable for single element");
        assertFalse(sql.contains("#{deviceIds[0]},"), "should not have trailing comma in IN clause");
        assertFalse(sql.contains(",#{deviceIds[0]}"), "should not have leading comma in IN clause");
    }

    // ========== helper ==========

    private int countOccurrences(String str, String substr) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(substr, idx)) != -1) {
            count++;
            idx += substr.length();
        }
        return count;
    }
}
