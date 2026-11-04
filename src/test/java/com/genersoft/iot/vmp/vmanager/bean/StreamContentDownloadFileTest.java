package com.genersoft.iot.vmp.vmanager.bean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 验证公开下载投影将完整成品与手动保存的部分文件分开，保持旧SDK的成功条件。 */
class StreamContentDownloadFileTest {

    /** 部分结果经过真实HTTP投影及JSON序列化后，不能出现在旧调用方使用的完整文件字段。 */
    @Test
    void partialFileIsMappedWithoutPromotingToCompleteFile() {
        DownloadFileInfo partial = file("https://example.invalid/partial.mp4");
        StreamInfo info = new StreamInfo();
        info.setDownloadStatus("interrupted");
        info.setDownloadedDuration(115.263);
        info.setProgress(0.19);
        info.setPartialDownloadFilePath(partial);

        StreamContent content = new StreamContent(info);
        JsonNode json = new ObjectMapper().valueToTree(content);

        assertNull(content.getDownLoadFilePath());
        assertSame(partial, content.getPartialDownloadFilePath());
        assertTrue(json.path("downLoadFilePath").isNull());
        assertEquals("https://example.invalid/partial.mp4", json.path("partialDownloadFilePath").path("httpsPath").asText());
        assertEquals("interrupted", json.path("downloadStatus").asText());
        assertEquals(115.263, json.path("downloadedDuration").asDouble());
    }

    /** 升级前中断缓存即使仍带旧字段，公开出口也只能给出明确的部分结果。 */
    @Test
    void legacyInterruptedFieldIsMovedToPartialField() {
        DownloadFileInfo partial = file("https://example.invalid/legacy-partial.mp4");
        StreamInfo info = new StreamInfo();
        info.setDownloadStatus("interrupted");
        info.setDownLoadFilePath(partial);
        StreamContent content = new StreamContent(info);
        assertNull(content.getDownLoadFilePath());
        assertSame(partial, content.getPartialDownloadFilePath());
    }

    /** 非终态中的切片不能通过旧字段让忽略新状态的SDK提前完成。 */
    @Test
    void intermediateFileIsNotExposedAsComplete() {
        for (String state : new String[]{"downloading", "finalizing"}) {
            StreamInfo info = new StreamInfo();
            info.setDownloadStatus(state);
            info.setDownLoadFilePath(file("https://example.invalid/slice.mp4"));
            assertNull(new StreamContent(info).getDownLoadFilePath());
        }
    }

    /** 正常完成仍使用原有字段，新增部分文件投影不改变旧调用方读取成功成品的方式。 */
    @Test
    void completedFileKeepsExistingPublicField() {
        DownloadFileInfo complete = file("https://example.invalid/complete.mp4");
        StreamInfo info = new StreamInfo();
        info.setDownloadStatus("completed");
        info.setProgress(1);
        info.setDownLoadFilePath(complete);

        StreamContent content = new StreamContent(info);
        JsonNode json = new ObjectMapper().valueToTree(content);

        assertSame(complete, content.getDownLoadFilePath());
        assertNull(content.getPartialDownloadFilePath());
        assertEquals("https://example.invalid/complete.mp4", json.path("downLoadFilePath").path("httpsPath").asText());
        assertTrue(json.path("partialDownloadFilePath").isNull());
        assertEquals(1, json.path("progress").asDouble());
    }

    /** 存量完整结果缺少新状态时仍保留原下载地址，避免DTO新增字段破坏旧缓存读取。 */
    @Test
    void legacyCompleteFileRemainsInOriginalField() {
        DownloadFileInfo complete = file("https://example.invalid/legacy.mp4");
        StreamInfo info = new StreamInfo();
        info.setDownLoadFilePath(complete);
        StreamContent content = new StreamContent(info);
        assertSame(complete, content.getDownLoadFilePath());
        assertNull(content.getPartialDownloadFilePath());
    }

    /** 使用无凭据的测试地址构造文件对象，测试仅验证字段归属。 */
    private DownloadFileInfo file(String url) {
        DownloadFileInfo info = new DownloadFileInfo();
        info.setHttpsPath(url);
        return info;
    }
}
