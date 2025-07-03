package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.MessageElement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "拉框放大/缩小控制参数")
public class DragZoomParam {

    @MessageElement("Length")
    @Schema(description = "播放窗口长度像素值(必选)")
    protected Integer length;

    @MessageElement("Width")
    @Schema(description = "播放窗口宽度像素值(必选)")
    protected Integer width;

    @MessageElement("MidPointX")
    @Schema(description = "拉框中心的横轴坐标像素值(必选)")
    protected Integer midPointX;

    @MessageElement("MidPointY")
    @Schema(description = "拉框中心的纵轴坐标像素值(必选)")
    protected Integer midPointY;

    @MessageElement("LengthX")
    @Schema(description = "拉框长度像素值(必选)")
    protected Integer lengthX;

    @MessageElement("LengthY")
    @Schema(description = "拉框宽度像素值(必选)")
    protected Integer lengthY;
}
