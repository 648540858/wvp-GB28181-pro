package com.genersoft.iot.vmp.streamProxy.bean;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Data
public class StreamProxyExcelDto {

    public static final String SAMPLE_NAME = "示例数据（请勿删除）";
    public static final String PROMPT = "提示：示例数据不允许删除，否则会影响导入数据内容";

    public static final String[] HEADERS = {
            "名称", "代理类型", "应用名", "流ID", "拉流地址", "超时时间(秒)", "指定流媒体节点",
            "RTSP拉流方式", "启用", "开启音频", "录制", "无人观看时停流", "国标编码", "经度", "纬度"
    };

    @ExcelProperty(value = "名称", index = 0)
    private String name;

    @ExcelProperty(value = "代理类型", index = 1)
    private String type;

    @ExcelProperty(value = "应用名", index = 2)
    private String app;

    @ExcelProperty(value = "流ID", index = 3)
    private String stream;

    @ExcelProperty(value = "拉流地址", index = 4)
    private String srcUrl;

    @ExcelProperty(value = "超时时间(秒)", index = 5)
    private String timeout;

    @ExcelProperty(value = "指定流媒体节点", index = 6)
    private String relatesMediaServerId;

    @ExcelProperty(value = "RTSP拉流方式", index = 7)
    private String rtspType;

    @ExcelProperty(value = "启用", index = 8)
    private String enable;

    @ExcelProperty(value = "开启音频", index = 9)
    private String enableAudio;

    @ExcelProperty(value = "录制", index = 10)
    private String enableMp4;

    @ExcelProperty(value = "无人观看时停流", index = 11)
    private String enableDisableNoneReader;

    @ExcelProperty(value = "国标编码", index = 12)
    private String gbDeviceId;

    @ExcelProperty(value = "经度", index = 13)
    private String longitude;

    @ExcelProperty(value = "纬度", index = 14)
    private String latitude;

    public static StreamProxyExcelDto sample(String stream, String gbDeviceId) {
        StreamProxyExcelDto dto = new StreamProxyExcelDto();
        dto.setName(SAMPLE_NAME + "：拉流代理-" + stream);
        dto.setType("默认");
        dto.setApp("live");
        dto.setStream(stream);
        dto.setSrcUrl("rtsp://admin:123456@192.168.1.100:554/Streaming/Channels/101");
        dto.setTimeout("10");
        dto.setRelatesMediaServerId("自动选择");
        dto.setRtspType("TCP");
        dto.setEnable("是");
        dto.setEnableAudio("是");
        dto.setEnableMp4("否");
        dto.setEnableDisableNoneReader("是");
        dto.setGbDeviceId(gbDeviceId);
        return dto;
    }

    public static StreamProxyExcelDto sample() {
        return sample("1", "00000000000001000001");
    }

    public static StreamProxyExcelDto prompt() {
        StreamProxyExcelDto dto = new StreamProxyExcelDto();
        dto.setName(PROMPT);
        return dto;
    }

    public static StreamProxyExcelDto from(StreamProxy streamProxy) {
        StreamProxyExcelDto dto = new StreamProxyExcelDto();
        dto.setName(streamProxy.getGbName());
        dto.setType("ffmpeg".equalsIgnoreCase(streamProxy.getType()) ? "FFmpeg" : "默认");
        dto.setApp(streamProxy.getApp());
        dto.setStream(streamProxy.getStream());
        dto.setSrcUrl(streamProxy.getSrcUrl());
        dto.setTimeout(String.valueOf(streamProxy.getTimeout()));
        dto.setRelatesMediaServerId(ObjectUtils.isEmpty(streamProxy.getRelatesMediaServerId())
                ? "自动选择" : streamProxy.getRelatesMediaServerId());
        dto.setRtspType(toRtspTypeText(streamProxy.getRtspType()));
        dto.setEnable(toBooleanText(streamProxy.isEnable()));
        dto.setEnableAudio(toBooleanText(streamProxy.isEnableAudio()));
        dto.setEnableMp4(toBooleanText(streamProxy.isEnableMp4()));
        dto.setEnableDisableNoneReader(toBooleanText(streamProxy.isEnableDisableNoneReader()));
        dto.setGbDeviceId(streamProxy.getGbDeviceId());
        dto.setLongitude(streamProxy.getGbLongitude() == null ? null : streamProxy.getGbLongitude().toString());
        dto.setLatitude(streamProxy.getGbLatitude() == null ? null : streamProxy.getGbLatitude().toString());
        return dto;
    }

    public boolean isIgnoredRow() {
        return isBlankRow() || (name != null && name.startsWith(SAMPLE_NAME))
                || (name != null && name.startsWith("提示："));
    }

    public StreamProxy toStreamProxy() {
        if (!StringUtils.hasText(app)) {
            throw new IllegalArgumentException("应用名不能为空");
        }
        if (!StringUtils.hasText(stream)) {
            throw new IllegalArgumentException("流ID不能为空");
        }
        if (!StringUtils.hasText(srcUrl)) {
            throw new IllegalArgumentException("拉流地址不能为空");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("名称不能为空");
        }
        if (!StringUtils.hasText(gbDeviceId)) {
            throw new IllegalArgumentException("国标编码不能为空");
        }
        if (!gbDeviceId.trim().matches("\\d{20}")) {
            throw new IllegalArgumentException("国标编码必须为20位数字");
        }

        StreamProxy streamProxy = new StreamProxy();
        streamProxy.setGbName(name.trim());
        streamProxy.setType(parseType(type));
        streamProxy.setApp(app.trim());
        streamProxy.setStream(stream.trim());
        streamProxy.setSrcUrl(srcUrl.trim());
        streamProxy.setTimeout(parseInteger(timeout, 10, "超时时间"));
        streamProxy.setRelatesMediaServerId("自动选择".equals(trimToNull(relatesMediaServerId))
                ? null : trimToNull(relatesMediaServerId));
        streamProxy.setRtspType(parseRtspType(rtspType));
        streamProxy.setEnable(parseBoolean(enable, true));
        streamProxy.setEnableAudio(parseBoolean(enableAudio, true));
        streamProxy.setEnableMp4(parseBoolean(enableMp4, false));
        streamProxy.setEnableDisableNoneReader(parseBoolean(enableDisableNoneReader, true));
        streamProxy.setGbDeviceId(gbDeviceId.trim());
        streamProxy.setGbLongitude(parseDouble(longitude, "经度"));
        streamProxy.setGbLatitude(parseDouble(latitude, "纬度"));
        streamProxy.setGbStatus("OFF");
        streamProxy.setPulling(false);
        return streamProxy;
    }

    public String[] toRow() {
        return new String[]{
                name, type, app, stream, srcUrl, timeout, relatesMediaServerId, rtspType,
                enable, enableAudio, enableMp4, enableDisableNoneReader, gbDeviceId, longitude, latitude
        };
    }

    private boolean isBlankRow() {
        return ObjectUtils.isEmpty(name) && ObjectUtils.isEmpty(type) && ObjectUtils.isEmpty(app)
                && ObjectUtils.isEmpty(stream) && ObjectUtils.isEmpty(srcUrl);
    }

    private static String parseType(String value) {
        if (ObjectUtils.isEmpty(value) || "默认".equalsIgnoreCase(value) || "default".equalsIgnoreCase(value)) {
            return "default";
        }
        if ("FFmpeg".equalsIgnoreCase(value)) {
            return "ffmpeg";
        }
        throw new IllegalArgumentException("代理类型仅支持默认或FFmpeg");
    }

    private static String parseRtspType(String value) {
        if (ObjectUtils.isEmpty(value) || "TCP".equalsIgnoreCase(value) || "0".equals(value)) {
            return "0";
        }
        if ("UDP".equalsIgnoreCase(value) || "1".equals(value)) {
            return "1";
        }
        if ("组播".equals(value) || "2".equals(value)) {
            return "2";
        }
        throw new IllegalArgumentException("RTSP拉流方式仅支持TCP、UDP或组播");
    }

    private static String toRtspTypeText(String value) {
        if ("1".equals(value)) {
            return "UDP";
        }
        if ("2".equals(value)) {
            return "组播";
        }
        return "TCP";
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (ObjectUtils.isEmpty(value)) {
            return defaultValue;
        }
        String text = value.trim();
        if ("是".equals(text) || "true".equalsIgnoreCase(text) || "1".equals(text) || "启用".equals(text)) {
            return true;
        }
        if ("否".equals(text) || "false".equalsIgnoreCase(text) || "0".equals(text) || "停用".equals(text)) {
            return false;
        }
        throw new IllegalArgumentException("布尔字段仅支持是/否、true/false或1/0");
    }

    private static String toBooleanText(boolean value) {
        return value ? "是" : "否";
    }

    private static int parseInteger(String value, int defaultValue, String fieldName) {
        if (ObjectUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            int result = Integer.parseInt(value.trim());
            if (result <= 0) {
                throw new IllegalArgumentException(fieldName + "必须大于0");
            }
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须为整数");
        }
    }

    private static Double parseDouble(String value, String fieldName) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须为数字");
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
