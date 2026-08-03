package com.genersoft.iot.vmp.streamProxy.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxyExcelDto;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("rawtypes")
/**
 * 拉流代理接口
 */
@Tag(name = "拉流代理", description = "")
@RestController
@Slf4j
@RequestMapping(value = "/api/proxy")
public class StreamProxyController {

    private static final int TEMPLATE_DATA_ROWS = 500;
    private static final int MEDIA_SERVER_COLUMN = 6;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private IStreamProxyPlayService streamProxyPlayService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private IGbChannelService gbChannelService;


    @Operation(summary = "分页查询流代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "pulling", description = "是否正在拉流")
    @Parameter(name = "mediaServerId", description = "流媒体ID")
    @GetMapping(value = "/list")
    @ResponseBody
    public PageInfo<StreamProxy> list(@RequestParam(required = false)Integer page,
                                      @RequestParam(required = false)Integer count,
                                      @RequestParam(required = false)String query,
                                      @RequestParam(required = false)Boolean pulling,
                                      @RequestParam(required = false)String mediaServerId){

        if (ObjectUtils.isEmpty(mediaServerId)) {
            mediaServerId = null;
        }
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        return streamProxyService.getAll(page, count, query, pulling, mediaServerId);
    }

    @GetMapping(value = "/excel/template")
    @Operation(summary = "下载拉流代理导入模板", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        List<StreamProxy> streamProxies = streamProxyService.getAllForExport(null, null, null);
        List<String> gbDeviceIds = gbChannelService.getAllDeviceIds();
        long nextStreamId = nextStreamId(streamProxies);
        int nextDeviceSequence = nextDeviceSequence(gbDeviceIds);
        String gbDevicePrefix = gbDevicePrefix();
        writeExcel(response, "拉流代理导入模板",
                Arrays.asList(StreamProxyExcelDto.sample(String.valueOf(nextStreamId),
                        gbDevicePrefix + formatDeviceSequence(nextDeviceSequence)), StreamProxyExcelDto.prompt()),
                true, nextStreamId + 1, nextDeviceSequence + 1, gbDevicePrefix);
    }

    @GetMapping(value = "/excel/export")
    @Operation(summary = "导出拉流代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String query,
                       @RequestParam(required = false) Boolean pulling,
                       @RequestParam(required = false) String mediaServerId) throws IOException {
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        if (ObjectUtils.isEmpty(mediaServerId)) {
            mediaServerId = null;
        }
        List<StreamProxyExcelDto> data = streamProxyService.getAllForExport(query, pulling, mediaServerId)
                .stream().map(StreamProxyExcelDto::from).toList();
        writeExcel(response, "拉流代理", data, false, 0, 0, null);
    }

    @PostMapping(value = "/excel/import")
    @Operation(summary = "导入拉流代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public WVPResult<Map<String, Object>> importExcel(@RequestParam(value = "file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "导入文件不能为空");
        }

        List<StreamProxyExcelDto> rows;
        try {
            rows = EasyExcel.read(file.getInputStream()).head(StreamProxyExcelDto.class).sheet().doReadSync();
        } catch (ExcelDataConvertException e) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(),
                    "第" + (e.getRowIndex() + 1) + "行第" + (e.getColumnIndex() + 1) + "列数据格式错误");
        } catch (Exception e) {
            log.warn("[拉流代理导入] 文件解析失败", e);
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "Excel文件解析失败");
        }

        int imported = 0;
        int ignored = 0;
        List<String> errors = new ArrayList<>();
        List<StreamProxy> existingStreamProxies = streamProxyService.getAllForExport(null, null, null);
        Set<String> usedStreamIds = new HashSet<>();
        if (existingStreamProxies != null) {
            existingStreamProxies.stream().map(StreamProxy::getStream).filter(StringUtils::hasText)
                    .map(String::trim).forEach(usedStreamIds::add);
        }
        List<String> existingGbDeviceIds = gbChannelService.getAllDeviceIds();
        Set<String> usedDeviceSequences = deviceSequences(existingGbDeviceIds);
        long nextStreamId = nextStreamId(existingStreamProxies);
        int nextDeviceSequence = nextDeviceSequence(existingGbDeviceIds);
        String gbDevicePrefix = gbDevicePrefix();
        Set<String> onlineMediaServerIds = new HashSet<>(onlineMediaServerIds());
        for (int index = 0; index < rows.size(); index++) {
            StreamProxyExcelDto row = rows.get(index);
            if (row == null || row.isIgnoredRow()) {
                ignored++;
                continue;
            }
            try {
                if (!StringUtils.hasText(row.getStream())) {
                    while (usedStreamIds.contains(String.valueOf(nextStreamId))) {
                        nextStreamId++;
                    }
                    row.setStream(String.valueOf(nextStreamId++));
                }
                if (!StringUtils.hasText(row.getName())) {
                    row.setName("拉流代理-" + row.getStream().trim());
                }
                if (!StringUtils.hasText(row.getGbDeviceId())) {
                    while (usedDeviceSequences.contains(formatDeviceSequence(nextDeviceSequence))) {
                        nextDeviceSequence++;
                    }
                    row.setGbDeviceId(gbDevicePrefix + formatDeviceSequence(nextDeviceSequence++));
                }
                StreamProxy streamProxy = row.toStreamProxy();
                if (usedStreamIds.contains(streamProxy.getStream())) {
                    throw new IllegalArgumentException("流ID已存在，必须保持唯一");
                }
                String deviceSequence = streamProxy.getGbDeviceId().substring(14);
                if (usedDeviceSequences.contains(deviceSequence)) {
                    throw new IllegalArgumentException("国标编码最后六位设备/用户序号已存在，必须保持唯一");
                }
                if (streamProxy.getRelatesMediaServerId() != null
                        && !onlineMediaServerIds.contains(streamProxy.getRelatesMediaServerId())) {
                    throw new IllegalArgumentException("指定流媒体节点不在线或不存在");
                }
                streamProxy.setServerId(userSetting.getServerId());
                streamProxyService.add(streamProxy);
                usedStreamIds.add(streamProxy.getStream());
                usedDeviceSequences.add(deviceSequence);
                imported++;
            } catch (ControllerException e) {
                errors.add("第" + (index + 2) + "行：" + e.getMsg());
            } catch (Exception e) {
                errors.add("第" + (index + 2) + "行：" + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("ignored", ignored);
        result.put("failed", errors.size());
        result.put("errors", errors);
        return WVPResult.success(result, errors.isEmpty() ? "导入成功" : "导入完成，部分数据失败");
    }

    @Operation(summary = "查询流代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名")
    @Parameter(name = "stream", description = "流Id")
    @GetMapping(value = "/one")
    @ResponseBody
    public StreamProxy one(String app, String stream){

        return streamProxyService.getStreamProxyByAppAndStream(app, stream);
    }

    @Operation(summary = "新增代理", security = @SecurityRequirement(name = JwtUtils.HEADER), parameters = {
            @Parameter(name = "param", description = "代理参数", required = true),
    })
    @PostMapping(value = "/add")
    @ResponseBody
    public StreamProxy add(@RequestBody StreamProxy param){
        log.info("添加代理： " + JSONObject.toJSONString(param));
        if (ObjectUtils.isEmpty(param.getRelatesMediaServerId())) {
            param.setRelatesMediaServerId(null);
        }
        if (ObjectUtils.isEmpty(param.getType())) {
            param.setType("default");
        }
        if (ObjectUtils.isEmpty(param.getGbId())) {
            param.setGbDeviceId(null);
        }
        param.setServerId(userSetting.getServerId());
        streamProxyService.add(param);
        return param;
    }

    @Operation(summary = "更新代理", security = @SecurityRequirement(name = JwtUtils.HEADER), parameters = {
            @Parameter(name = "param", description = "代理参数", required = true),
    })
    @PostMapping(value = "/update")
    @ResponseBody
    public StreamProxy update(@RequestBody StreamProxy param){
        log.info("更新代理： " + JSONObject.toJSONString(param));
        if (param.getId() == 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "缺少代理信息的ID");
        }
        if (ObjectUtils.isEmpty(param.getRelatesMediaServerId())) {
            param.setRelatesMediaServerId(null);
        }
        if (ObjectUtils.isEmpty(param.getGbId())) {
            param.setGbDeviceId(null);
        }
        streamProxyService.update(param);
        return param;
    }

    @GetMapping(value = "/ffmpeg_cmd/list")
    @ResponseBody
    @Operation(summary = "获取ffmpeg.cmd模板", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "mediaServerId", description = "流媒体ID", required = true)
    public Map<String, String> getFFmpegCMDs(@RequestParam String mediaServerId){
        log.debug("获取节点[ {} ]ffmpeg.cmd模板", mediaServerId );

        MediaServer mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "流媒体： " + mediaServerId + "未找到");
        }
        return streamProxyService.getFFmpegCMDs(mediaServerItem);
    }

    @DeleteMapping(value = "/del")
    @ResponseBody
    @Operation(summary = "移除代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    public void del(@RequestParam String app, @RequestParam String stream){
        log.info("移除代理： " + app + "/" + stream);
        if (app == null || stream == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), app == null ?"app不能为null":"stream不能为null");
        }else {
            streamProxyService.delteByAppAndStream(app, stream);
        }
    }

    @DeleteMapping(value = "/delete")
    @ResponseBody
    @Operation(summary = "移除代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "代理ID", required = true)
    public void delte(int id){
        log.info("移除代理： {}", id);
        streamProxyService.delete(id);
    }

    @GetMapping(value = "/start")
    @ResponseBody
    @Operation(summary = "播放代理", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "代理Id", required = true)
    public DeferredResult<WVPResult<StreamContent>> start(HttpServletRequest request, int id){
        log.info("播放代理： {}", id);
        StreamProxy streamProxy = streamProxyService.getStreamProxy(id);
        Assert.notNull(streamProxy, "代理信息不存在");

        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

        ErrorCallback<StreamInfo> callback = (code, msg, streamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                WVPResult<StreamContent> wvpResult = WVPResult.success();
                if (streamInfo != null) {
                    if (userSetting.getUseSourceIpAsStreamIp()) {
                        streamInfo=streamInfo.clone();//深拷贝
                        String host;
                        try {
                            URL url=new URL(request.getRequestURL().toString());
                            host=url.getHost();
                        } catch (MalformedURLException e) {
                            host=request.getLocalAddr();
                        }
                        streamInfo.changeStreamIp(host);
                    }
                    if (!ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix())
                            && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }
                    wvpResult.setData(new StreamContent(streamInfo));
                }else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }

                result.setResult(wvpResult);
            }else {
                result.setResult(WVPResult.fail(code, msg));
            }
        };

        streamProxyPlayService.start(id, null, callback);
        return result;
    }

    @GetMapping(value = "/stop")
    @ResponseBody
    @Operation(summary = "停止播放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "代理Id", required = true)
    public void stop(int id){
        log.info("停止播放： {}", id);
        streamProxyPlayService.stop(id);
    }

    private void writeExcel(HttpServletResponse response, String fileName,
                            List<StreamProxyExcelDto> data, boolean template, long firstStreamId,
                            int firstDeviceSequence, String gbDevicePrefix) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedFileName = URLEncoder.encode(fileName + ".xlsx", "UTF-8").replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFileName);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("拉流代理");
            CellStyle headerStyle = createHeaderStyle(workbook);
            Row header = sheet.createRow(0);
            for (int column = 0; column < StreamProxyExcelDto.HEADERS.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(StreamProxyExcelDto.HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }

            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                StreamProxyExcelDto item = data.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                if (template && StreamProxyExcelDto.PROMPT.equals(item.getName())) {
                    Cell cell = row.createCell(0);
                    cell.setCellValue(StreamProxyExcelDto.PROMPT);
                    cell.setCellStyle(createPromptStyle(workbook));
                    sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1,
                            0, StreamProxyExcelDto.HEADERS.length - 1));
                    continue;
                }
                String[] values = item.toRow();
                for (int column = 0; column < values.length; column++) {
                    row.createCell(column).setCellValue(values[column] == null ? "" : values[column]);
                }
            }

            if (template) {
                addTemplateFormulaRows(sheet, firstStreamId, firstDeviceSequence, gbDevicePrefix);
                workbook.setForceFormulaRecalculation(true);
            }
            addMediaServerOptions(workbook, sheet, template, data.size());

            for (int column = 0; column < StreamProxyExcelDto.HEADERS.length; column++) {
                sheet.autoSizeColumn(column);
                sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 512, 50 * 256));
            }
            workbook.write(response.getOutputStream());
        }
    }

    private void addTemplateFormulaRows(Sheet sheet, long firstStreamId, int firstDeviceSequence,
                                        String gbDevicePrefix) {
        for (int index = 0; index < TEMPLATE_DATA_ROWS; index++) {
            int rowIndex = index + 3;
            int excelRow = rowIndex + 1;
            Row row = sheet.createRow(rowIndex);
            long streamId = Math.addExact(firstStreamId, index);
            String gbDeviceId = gbDevicePrefix + formatDeviceSequence(firstDeviceSequence + index);
            row.createCell(3).setCellFormula("IF(OR(C" + excelRow + "<>\"\",E" + excelRow
                    + "<>\"\"),\"" + streamId + "\",\"\")");
            row.createCell(0).setCellFormula("IF(D" + excelRow + "<>\"\",\"拉流代理-\"&D"
                    + excelRow + ",\"\")");
            row.createCell(12).setCellFormula("IF(D" + excelRow + "<>\"\",\"" + gbDeviceId + "\",\"\")");
        }
    }

    private void addMediaServerOptions(Workbook workbook, Sheet sheet, boolean template, int dataSize) {
        List<String> mediaServerIds = onlineMediaServerIds();
        Sheet optionSheet = workbook.createSheet("_options");
        for (int index = 0; index < mediaServerIds.size(); index++) {
            optionSheet.createRow(index).createCell(0).setCellValue(mediaServerIds.get(index));
        }

        Name optionName = workbook.createName();
        optionName.setNameName("MediaServerOptions");
        optionName.setRefersToFormula("'_options'!$A$1:$A$" + mediaServerIds.size());
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint("MediaServerOptions");
        CellRangeAddressList addressList = new CellRangeAddressList();
        if (template) {
            addressList.addCellRangeAddress(1, MEDIA_SERVER_COLUMN, 1, MEDIA_SERVER_COLUMN);
            addressList.addCellRangeAddress(3, MEDIA_SERVER_COLUMN,
                    TEMPLATE_DATA_ROWS + 2, MEDIA_SERVER_COLUMN);
        } else {
            addressList.addCellRangeAddress(1, MEDIA_SERVER_COLUMN,
                    Math.max(dataSize, 1), MEDIA_SERVER_COLUMN);
        }
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.createErrorBox("节点选择错误", "请选择下拉菜单中的在线节点，或选择自动选择");
        sheet.addValidationData(validation);
        workbook.setSheetHidden(workbook.getSheetIndex(optionSheet), true);
    }

    private List<String> onlineMediaServerIds() {
        Set<String> result = new LinkedHashSet<>();
        result.add("自动选择");
        List<MediaServer> mediaServers = mediaServerService.getAllOnline();
        if (mediaServers != null) {
            mediaServers.stream().map(MediaServer::getId).filter(StringUtils::hasText)
                    .forEach(result::add);
        }
        return new ArrayList<>(result);
    }

    private long nextStreamId(List<StreamProxy> streamProxies) {
        long max = 0;
        if (streamProxies != null) {
            for (StreamProxy streamProxy : streamProxies) {
                String stream = streamProxy.getStream();
                if (StringUtils.hasText(stream) && stream.matches("\\d+")) {
                    try {
                        max = Math.max(max, Long.parseLong(stream));
                    } catch (NumberFormatException ignored) {
                        // 超出 long 范围的手工流ID不参与默认数字流ID的计算
                    }
                }
            }
        }
        if (max == Long.MAX_VALUE) {
            throw new ControllerException(ErrorCode.ERROR500.getCode(), "数字流ID已用尽");
        }
        return max + 1;
    }

    private int nextDeviceSequence(List<String> gbDeviceIds) {
        int max = 0;
        if (gbDeviceIds != null) {
            for (String gbDeviceId : gbDeviceIds) {
                String deviceSequence = deviceSequence(gbDeviceId);
                if (deviceSequence != null) {
                    max = Math.max(max, Integer.parseInt(deviceSequence));
                }
            }
        }
        if (max >= 999999) {
            throw new ControllerException(ErrorCode.ERROR500.getCode(), "国标编码设备/用户序号已用尽");
        }
        return max + 1;
    }

    private Set<String> deviceSequences(List<String> gbDeviceIds) {
        Set<String> result = new HashSet<>();
        if (gbDeviceIds != null) {
            gbDeviceIds.stream().map(this::deviceSequence).filter(value -> value != null).forEach(result::add);
        }
        return result;
    }

    private String deviceSequence(String gbDeviceId) {
        if (!StringUtils.hasText(gbDeviceId)) {
            return null;
        }
        String value = gbDeviceId.trim();
        if (value.length() < 6) {
            return null;
        }
        String sequence = value.substring(value.length() - 6);
        return sequence.matches("\\d{6}") ? sequence : null;
    }

    private String gbDevicePrefix() {
        String sipId = sipConfig.getId();
        if (sipId == null || !sipId.matches("\\d{20}")) {
            throw new ControllerException(ErrorCode.ERROR500.getCode(), "SIP国标编号必须为20位数字，无法自动生成国标编码");
        }
        return sipId.substring(0, 8) + "001327";
    }

    private String formatDeviceSequence(int sequence) {
        if (sequence < 1 || sequence > 999999) {
            throw new ControllerException(ErrorCode.ERROR500.getCode(), "国标编码设备/用户序号已用尽");
        }
        return String.format("%06d", sequence);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createPromptStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
