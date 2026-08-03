package com.genersoft.iot.vmp.streamProxy.controller;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxyExcelDto;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamProxyControllerTest {

    @Mock
    private IMediaServerService mediaServerService;

    @Mock
    private IStreamProxyService streamProxyService;

    @Mock
    private IStreamProxyPlayService streamProxyPlayService;

    @Mock
    private UserSetting userSetting;

    @Mock
    private SipConfig sipConfig;

    @Mock
    private IGbChannelService gbChannelService;

    @InjectMocks
    private StreamProxyController controller;

    @Test
    void templateShouldContainAutomaticValuesAndOnlineMediaServerDropdown() throws Exception {
        StreamProxy existing = new StreamProxy();
        existing.setStream("9");
        MediaServer mediaServer = new MediaServer();
        mediaServer.setId("zlm-node-1");
        when(streamProxyService.getAllForExport(null, null, null)).thenReturn(List.of(existing));
        when(gbChannelService.getAllDeviceIds()).thenReturn(List.of("41010500001327000005"));
        when(sipConfig.getId()).thenReturn("41010500002000000001");
        when(mediaServerService.getAllOnline()).thenReturn(List.of(mediaServer));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.downloadTemplate(response);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            var sheet = workbook.getSheet("拉流代理");
            assertEquals("名称", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("RTSP拉流方式", sheet.getRow(0).getCell(7).getStringCellValue());
            assertEquals(15, sheet.getRow(0).getLastCellNum());
            assertTrue(sheet.getRow(1).getCell(0).getStringCellValue().startsWith(StreamProxyExcelDto.SAMPLE_NAME));
            assertEquals("10", sheet.getRow(1).getCell(3).getStringCellValue());
            assertEquals("41010500001327000006", sheet.getRow(1).getCell(12).getStringCellValue());
            assertEquals(StreamProxyExcelDto.PROMPT, sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals(IndexedColors.RED.getIndex(),
                    workbook.getFontAt(sheet.getRow(2).getCell(0).getCellStyle().getFontIndex()).getColor());
            assertTrue(sheet.getRow(3).getCell(3).getCellFormula().contains("\"11\""));
            assertTrue(sheet.getRow(3).getCell(0).getCellFormula().contains("拉流代理-"));
            assertTrue(sheet.getRow(3).getCell(12).getCellFormula().contains("41010500001327000007"));
            assertEquals(1, sheet.getDataValidations().size());
            assertEquals("MediaServerOptions",
                    sheet.getDataValidations().get(0).getValidationConstraint().getFormula1());
            assertEquals("自动选择", workbook.getSheet("_options").getRow(0).getCell(0).getStringCellValue());
            assertEquals("zlm-node-1", workbook.getSheet("_options").getRow(1).getCell(0).getStringCellValue());
            assertTrue(workbook.isSheetHidden(workbook.getSheetIndex("_options")));
        }
    }

    @Test
    void exportShouldUseAutomaticSelectionAndOnlineMediaServerDropdown() throws Exception {
        StreamProxy streamProxy = new StreamProxy();
        streamProxy.setGbName("园区东门");
        streamProxy.setApp("live");
        streamProxy.setStream("1");
        streamProxy.setSrcUrl("rtsp://192.168.1.10/live");
        streamProxy.setTimeout(10);
        streamProxy.setGbDeviceId("41010500001327000001");
        when(streamProxyService.getAllForExport(null, null, null)).thenReturn(List.of(streamProxy));
        when(mediaServerService.getAllOnline()).thenReturn(List.of());
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.export(response, null, null, null);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            var sheet = workbook.getSheet("拉流代理");
            assertEquals("自动选择", sheet.getRow(1).getCell(6).getStringCellValue());
            assertEquals(1, sheet.getDataValidations().size());
            assertEquals("自动选择", workbook.getSheet("_options").getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test
    void importShouldGenerateRequiredValuesAndConvertAutomaticMediaServerToNull() throws Exception {
        when(streamProxyService.getAllForExport(null, null, null)).thenReturn(List.of());
        when(gbChannelService.getAllDeviceIds()).thenReturn(List.of());
        when(sipConfig.getId()).thenReturn("41010500002000000001");
        when(mediaServerService.getAllOnline()).thenReturn(List.of());
        when(userSetting.getServerId()).thenReturn("wvp-1");
        MockMultipartFile file = new MockMultipartFile("file", "proxy.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                importWorkbook(null, null, null));

        WVPResult<Map<String, Object>> result = controller.importExcel(file);

        assertEquals(1, result.getData().get("imported"));
        assertEquals(0, result.getData().get("failed"));
        ArgumentCaptor<StreamProxy> captor = ArgumentCaptor.forClass(StreamProxy.class);
        verify(streamProxyService).add(captor.capture());
        StreamProxy imported = captor.getValue();
        assertEquals("1", imported.getStream());
        assertEquals("拉流代理-1", imported.getGbName());
        assertEquals("41010500001327000001", imported.getGbDeviceId());
        assertEquals("wvp-1", imported.getServerId());
        assertNull(imported.getRelatesMediaServerId());
        assertNull(imported.getFfmpegCmdKey());
    }

    @Test
    void importShouldRejectDuplicateDeviceSequenceEvenWhenGbPrefixIsDifferent() throws Exception {
        when(streamProxyService.getAllForExport(null, null, null)).thenReturn(List.of());
        when(gbChannelService.getAllDeviceIds()).thenReturn(List.of("41010500001327000123"));
        when(sipConfig.getId()).thenReturn("41010500002000000001");
        when(mediaServerService.getAllOnline()).thenReturn(List.of());
        MockMultipartFile file = new MockMultipartFile("file", "proxy.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                importWorkbook("测试代理", "2", "33010000001327000123"));

        WVPResult<Map<String, Object>> result = controller.importExcel(file);

        assertEquals(0, result.getData().get("imported"));
        assertEquals(1, result.getData().get("failed"));
        assertTrue(((List<?>) result.getData().get("errors")).get(0).toString().contains("设备/用户序号已存在"));
        verify(streamProxyService, never()).add(any());
    }

    @Test
    void untouchedTemplateShouldNotImportSamplePromptOrFormulaRows() throws Exception {
        when(streamProxyService.getAllForExport(null, null, null)).thenReturn(List.of());
        when(gbChannelService.getAllDeviceIds()).thenReturn(List.of());
        when(sipConfig.getId()).thenReturn("41010500002000000001");
        when(mediaServerService.getAllOnline()).thenReturn(List.of());
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadTemplate(response);
        MockMultipartFile file = new MockMultipartFile("file", "proxy.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getContentAsByteArray());

        WVPResult<Map<String, Object>> result = controller.importExcel(file);

        assertEquals(0, result.getData().get("imported"));
        assertEquals(0, result.getData().get("failed"));
        verify(streamProxyService, never()).add(any());
    }

    private byte[] importWorkbook(String name, String stream, String gbDeviceId) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("拉流代理");
            var header = sheet.createRow(0);
            for (int index = 0; index < StreamProxyExcelDto.HEADERS.length; index++) {
                header.createCell(index).setCellValue(StreamProxyExcelDto.HEADERS[index]);
            }
            var row = sheet.createRow(1);
            if (name != null) {
                row.createCell(0).setCellValue(name);
            }
            row.createCell(2).setCellValue("live");
            if (stream != null) {
                row.createCell(3).setCellValue(stream);
            }
            row.createCell(4).setCellValue("rtsp://192.168.1.10/live");
            row.createCell(6).setCellValue("自动选择");
            if (gbDeviceId != null) {
                row.createCell(12).setCellValue(gbDeviceId);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
