package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.genersoft.iot.vmp.gb28181.bean.RecordItem;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 级联录像查询(RecordInfo)回复分页的最小验证: 只覆盖切分逻辑与单页 XML 组装,
 * 不涉及真实 SIP 收发(端到端未验证)。
 */
class RecordInfoPagingTest {

    private static List<RecordItem> createRecords(int count) {
        List<RecordItem> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            RecordItem item = new RecordItem();
            item.setDeviceId("34020000001320000001");
            item.setName("record-" + i);
            item.setStartTime("2026-05-06 10:00:00");
            item.setEndTime("2026-05-06 10:05:00");
            item.setFilePath("/record-" + i);
            item.setFileSize("1024");
            item.setType("time");
            list.add(item);
        }
        return list;
    }

    private static int countOccurrences(String source, String token) {
        int count = 0;
        int index = source.indexOf(token);
        while (index >= 0) {
            count++;
            index = source.indexOf(token, index + token.length());
        }
        return count;
    }

    @Test
    void partitionKeepsSinglePageWhenPageSizeIsNotPositive() {
        List<RecordItem> records = createRecords(5);

        List<List<RecordItem>> noLimit = SIPCommanderForPlatform.partitionRecordItems(records, 0);
        assertEquals(1, noLimit.size());
        assertEquals(5, noLimit.get(0).size());

        List<List<RecordItem>> nullLimit = SIPCommanderForPlatform.partitionRecordItems(records, null);
        assertEquals(1, nullLimit.size());
        assertEquals(5, nullLimit.get(0).size());
    }

    @Test
    void partitionSplitsIntoPagesAndKeepsEveryRecordExactlyOnce() {
        List<RecordItem> records = createRecords(250);

        List<List<RecordItem>> pages = SIPCommanderForPlatform.partitionRecordItems(records, 100);

        assertEquals(3, pages.size());
        assertEquals(100, pages.get(0).size());
        assertEquals(100, pages.get(1).size());
        assertEquals(50, pages.get(2).size());

        Set<String> names = new HashSet<>();
        int total = 0;
        for (List<RecordItem> page : pages) {
            assertTrue(page.size() <= 100);
            for (RecordItem item : page) {
                names.add(item.getName());
                total++;
            }
        }
        assertEquals(250, total);
        assertEquals(250, names.size());
    }

    @Test
    void partitionReturnsSingleEmptyPageForEmptyList() {
        List<List<RecordItem>> pages = SIPCommanderForPlatform.partitionRecordItems(new ArrayList<>(), 10);

        assertEquals(1, pages.size());
        assertTrue(pages.get(0).isEmpty());
    }

    @Test
    void paginatedXmlKeepsSumNumAsTotalAndPageCountAsPageSize() {
        List<RecordItem> records = createRecords(250);
        List<List<RecordItem>> pages = SIPCommanderForPlatform.partitionRecordItems(records, 100);

        String firstPage = SIPCommanderForPlatform.buildRecordInfoXml("GB2312", "1234",
                "34020000001320000001", records.size(), pages.get(0));
        String lastPage = SIPCommanderForPlatform.buildRecordInfoXml("GB2312", "1234",
                "34020000001320000001", records.size(), pages.get(2));

        // 总条数始终为 250, 每页的 RecordList Num 为本页条数
        assertTrue(firstPage.contains("<SumNum>250</SumNum>"));
        assertTrue(firstPage.contains("<RecordList Num=\"100\">"));
        assertEquals(100, countOccurrences(firstPage, "<Item>"));
        assertTrue(firstPage.startsWith("<?xml version=\"1.0\" encoding=\"GB2312\"?>"));
        assertTrue(firstPage.trim().endsWith("</Response>"));

        assertTrue(lastPage.contains("<SumNum>250</SumNum>"));
        assertTrue(lastPage.contains("<RecordList Num=\"50\">"));
        assertEquals(50, countOccurrences(lastPage, "<Item>"));

        // 单页(不分页)时的输出必须与改动前逐字一致
        String startTime = DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601("2026-05-06 10:00:00");
        String endTime = DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601("2026-05-06 10:05:00");
        String expected = "<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n"
                + "<Response>\r\n"
                + "<CmdType>RecordInfo</CmdType>\r\n"
                + "<SN>1234</SN>\r\n"
                + "<DeviceID>34020000001320000001</DeviceID>\r\n"
                + "<SumNum>1</SumNum>\r\n"
                + "<RecordList Num=\"1\">\r\n"
                + "<Item>\r\n"
                + "<DeviceID>34020000001320000001</DeviceID>\r\n"
                + "<Name>record-0</Name>\r\n"
                + "<StartTime>" + startTime + "</StartTime>\r\n"
                + "<EndTime>" + endTime + "</EndTime>\r\n"
                + "<Secrecy>0</Secrecy>\r\n"
                + "<Type>time</Type>\r\n"
                + "<FileSize>1024</FileSize>\r\n"
                + "<FilePath>/record-0</FilePath>\r\n"
                + "</Item>\r\n"
                + "</RecordList>\r\n"
                + "</Response>\r\n";
        String singlePage = SIPCommanderForPlatform.buildRecordInfoXml("GB2312", "1234",
                "34020000001320000001", 1, createRecords(1));
        assertEquals(expected, singlePage);
        assertFalse(singlePage.contains("null"));
    }
}
