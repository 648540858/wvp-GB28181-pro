package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dom4j.Element;

@Data
@Schema(description = "SVAC解码配置")
public class SVACDecodeConfig implements DeviceConfigAware {

    @Schema(description = "SVC参数")
    private SVCParam svcParam;

    @Schema(description = "监控专用信息参数")
    private SurveillanceParam surveillanceParam;

    @Override
    public String configType() {
        return "SVACDecodeConfig";
    }

    @Override
    public void fromXml(Element element) {
        Element svcEl = element.element("SVCParam");
        if (svcEl != null) {
            SVCParam s = new SVCParam();
            s.fromXml(svcEl);
            setSvcParam(s);
        }
        Element survEl = element.element("SurveillanceParam");
        if (survEl != null) {
            SurveillanceParam s = new SurveillanceParam();
            s.fromXml(survEl);
            setSurveillanceParam(s);
        }
    }

    @Data
    public static class SVCParam {
        @Schema(description = "空域编码能力，0:不支持，1:1级增强，2:2级增强，3:3级增强")
        private Integer SVCSpaceSupportMode;

        @Schema(description = "时域编码能力，0:不支持，1:1级增强，2:2级增强，3:3级增强")
        private Integer SVCTimeSupportMode;

        public void fromXml(Element element) {
            setSVCSpaceSupportMode(XmlUtil.getInteger(element, "SVCSpaceSupportMode"));
            setSVCTimeSupportMode(XmlUtil.getInteger(element, "SVCTimeSupportMode"));
        }
    }

    @Data
    public static class SurveillanceParam {
        @Schema(description = "绝对时间信息显示开关，0:关闭，1:打开")
        private Integer TimeShowFlag;

        @Schema(description = "监控事件信息显示开关，0:关闭，1:打开")
        private Integer EventShowFlag;

        @Schema(description = "报警信息显示开关，0:关闭，1:打开")
        private Integer AlerShowtFlag;

        public void fromXml(Element element) {
            setTimeShowFlag(XmlUtil.getInteger(element, "TimeShowFlag"));
            setEventShowFlag(XmlUtil.getInteger(element, "EventShowFlag"));
            setAlerShowtFlag(XmlUtil.getInteger(element, "AlerShowtFlag"));
        }
    }
}
