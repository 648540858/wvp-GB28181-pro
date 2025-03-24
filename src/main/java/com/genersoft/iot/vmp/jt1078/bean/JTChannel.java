package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.ObjectUtils;

/**
 * JT 通道
 */
@Data
@Schema(description = "jt808通道")
@EqualsAndHashCode(callSuper = true)
public class JTChannel extends CommonGBChannel {

    @Schema(description = "数据库自增ID")
    private int id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "设备的数据库ID")
    private int terminalDbId;

    @Schema(description = "通道ID")
    private Integer channelId;

    @Schema(description = "是否含有音频")
    private boolean hasAudio;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "流信息")
    private String stream;

    private Integer dataType = ChannelDataType.JT_1078.value;

    @Override
    public String toString() {
        return "JTChannel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", terminalDbId=" + terminalDbId +
                ", channelId=" + channelId +
                ", createTime='" + createTime + '\'' +
                ", updateTime='" + updateTime + '\'' +
                ", hasAudio='" + hasAudio + '\'' +
                '}';
    }

    public CommonGBChannel buildCommonGBChannel() {
        if (ObjectUtils.isEmpty(this.getGbDeviceId())) {
            return null;
        }
        if (ObjectUtils.isEmpty(this.getGbName())) {
            this.setGbName(this.getName());
        }
        return this;

    }
}
