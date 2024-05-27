package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JT 终端属性
 */
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

    public JTDeviceType getType() {
        return type;
    }

    public void setType(JTDeviceType type) {
        this.type = type;
    }

    public String getMakerId() {
        return makerId;
    }

    public void setMakerId(String makerId) {
        this.makerId = makerId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getIccId() {
        return iccId;
    }

    public void setIccId(String iccId) {
        this.iccId = iccId;
    }

    public String getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public JTGnssAttribute getGnssAttribute() {
        return gnssAttribute;
    }

    public void setGnssAttribute(JTGnssAttribute gnssAttribute) {
        this.gnssAttribute = gnssAttribute;
    }

    public JTCommunicationModuleAttribute getCommunicationModuleAttribute() {
        return communicationModuleAttribute;
    }

    public void setCommunicationModuleAttribute(JTCommunicationModuleAttribute communicationModuleAttribute) {
        this.communicationModuleAttribute = communicationModuleAttribute;
    }

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
