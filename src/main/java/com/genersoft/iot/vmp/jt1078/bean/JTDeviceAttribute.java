package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * JT 终端属性
 */
@Setter
@Getter
@Schema(description = "JT终端属性")
public class JTDeviceAttribute {

    @Schema(description = "终端类型")
    private JTDeviceType type;

    @Schema(description = "制造商 ID")
    private String makerId;

    @Schema(description = "终端型号")
    private String deviceModel;

    @Schema(description = "终端 ID")
    private String terminalId;

    @Schema(description = "终端 SIM卡 ICCID")
    private String iccId;

    @Schema(description = "终端硬件版本号")
    private String hardwareVersion;

    @Schema(description = "固件版本号")
    private String firmwareVersion ;

    @Schema(description = "GNSS 模块属性")
    private JTGnssAttribute gnssAttribute ;

    @Schema(description = "通信模块属性")
    private JTCommunicationModuleAttribute communicationModuleAttribute ;

    @Override
    public String toString() {
        return "JTDeviceAttribute{" +
                "type=" + type +
                ", makerId='" + makerId + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", terminalId='" + terminalId + '\'' +
                ", iccId='" + iccId + '\'' +
                ", hardwareVersion='" + hardwareVersion + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", gnssAttribute=" + gnssAttribute +
                ", communicationModuleAttribute=" + communicationModuleAttribute +
                '}';
    }
}
