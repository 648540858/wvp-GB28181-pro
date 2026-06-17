package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dom4j.Element;

@Data
@Schema(description = "视频参数范围")
public class VideoParamOpt implements DeviceConfigAware {

    @Schema(description = "下载倍速范围，各可选参数以 '/' 分隔")
    private String downloadSpeed;

    @Schema(description = "摄像机支持的分辨率，多个分辨率值以 '/' 分隔")
    private String resolution;

    @Override
    public String configType() {
        return "VideoParamOpt";
    }

    @Override
    public void fromXml(Element element) {
        setDownloadSpeed(XmlUtil.getText(element, "DownloadSpeed"));
        setResolution(XmlUtil.getText(element, "Resolution"));
    }
}
