package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "SVAC编码配置")
public class SVACEncodeConfig implements DeviceConfigAware {

    @Schema(description = "感兴趣区域参数")
    private ROIParam roiParam;

    @Schema(description = "SVC参数")
    private SVCParam svcParam;

    @Schema(description = "监控专用信息参数")
    private SurveillanceParam surveillanceParam;

    @Schema(description = "音频参数")
    private AudioParam audioParam;

    @Override
    public String configType() {
        return "SVACEncodeConfig";
    }

    @Override
    public void fromXml(Element element) {
        Element roiEl = element.element("ROIParam");
        if (roiEl != null) {
            ROIParam r = new ROIParam();
            r.fromXml(roiEl);
            setRoiParam(r);
        }
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
        Element audioEl = element.element("AudioParam");
        if (audioEl != null) {
            AudioParam a = new AudioParam();
            a.fromXml(audioEl);
            setAudioParam(a);
        }
    }

    @Data
    public static class ROIParam {
        @Schema(description = "感兴趣区域开关，0:关闭，1:打开")
        private Integer ROIFlag;

        @Schema(description = "感兴趣区域数量，取值范围0~16")
        private Integer ROINumber;

        @Schema(description = "感兴趣区域列表")
        private List<ROIItem> Item;

        @Schema(description = "背景区域编码质量等级，0:一般，1:较好，2:好，3:很好")
        private Integer BackGroundQP;

        @Schema(description = "背景跳过开关，0:关闭，1:打开")
        private Integer BackGroundSkipFlag;

        public void fromXml(Element element) {
            setROIFlag(XmlUtil.getInteger(element, "ROIFlag"));
            setROINumber(XmlUtil.getInteger(element, "ROINumber"));
            List<Element> itemElements = element.elements("Item");
            if (!itemElements.isEmpty()) {
                List<ROIItem> list = new ArrayList<>();
                for (Element e : itemElements) {
                    ROIItem item = new ROIItem();
                    item.fromXml(e);
                    list.add(item);
                }
                setItem(list);
            }
            setBackGroundQP(XmlUtil.getInteger(element, "BackGroundQP"));
            setBackGroundSkipFlag(XmlUtil.getInteger(element, "BackGroundSkipFlag"));
        }
    }

    @Data
    public static class ROIItem {
        @Schema(description = "感兴趣区域编号，取值范围1~16")
        private Integer ROISeq;

        @Schema(description = "感兴趣区域左上角坐标，取值范围0~19683")
        private Integer TopLeft;

        @Schema(description = "感兴趣区域右下角坐标，取值范围0~19683")
        private Integer BottomRight;

        @Schema(description = "ROI区域编码质量等级，0:一般，1:较好，2:好，3:很好")
        private Integer ROIQP;

        public void fromXml(Element element) {
            setROISeq(XmlUtil.getInteger(element, "ROISeq"));
            setTopLeft(XmlUtil.getInteger(element, "TopLeft"));
            setBottomRight(XmlUtil.getInteger(element, "BottomRight"));
            setROIQP(XmlUtil.getInteger(element, "ROIQP"));
        }

    }

    @Data
    public static class SVCParam {
        @Schema(description = "空域编码方式，0:基本层，1:1级增强，2:2级增强，3:3级增强")
        private Integer SVCSpaceDomainMode;

        @Schema(description = "时域编码方式，0:基本层，1:1级增强，2:2级增强，3:3级增强")
        private Integer SVCTimeDomainMode;

        @Schema(description = "空域编码能力，0:不支持，1:1级增强，2:2级增强，3:3级增强")
        private Integer SVCSpaceSupportMode;

        @Schema(description = "时域编码能力，0:不支持，1:1级增强，2:2级增强，3:3级增强")
        private Integer SVCTimeSupportMode;

        public void fromXml(Element element) {
            setSVCSpaceDomainMode(XmlUtil.getInteger(element, "SVCSpaceDomainMode"));
            setSVCTimeDomainMode(XmlUtil.getInteger(element, "SVCTimeDomainMode"));
            setSVCSpaceSupportMode(XmlUtil.getInteger(element, "SVCSpaceSupportMode"));
            setSVCTimeSupportMode(XmlUtil.getInteger(element, "SVCTimeSupportMode"));
        }
    }

    @Data
    public static class SurveillanceParam {
        @Schema(description = "绝对时间信息开关，0:关闭，1:打开")
        private Integer TimeFlag;

        @Schema(description = "监控事件信息开关，0:关闭，1:打开")
        private Integer EventFlag;

        @Schema(description = "报警信息开关，0:关闭，1:打开")
        private Integer AlertFlag;

        public void fromXml(Element element) {
            setTimeFlag(XmlUtil.getInteger(element, "TimeFlag"));
            setEventFlag(XmlUtil.getInteger(element, "EventFlag"));
            setAlertFlag(XmlUtil.getInteger(element, "AlertFlag"));
        }
    }

    @Data
    public static class AudioParam {
        @Schema(description = "声音识别特征参数开关，0:关闭，1:打开")
        private Integer AudioRecognitionFlag;

        public void fromXml(Element element) {
            setAudioRecognitionFlag(XmlUtil.getInteger(element, "AudioRecognitionFlag"));
        }
    }
}
