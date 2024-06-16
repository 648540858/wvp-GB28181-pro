package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JT 通道
 */
@Schema(description = "jt808通道")
public class JTChannel {

    private int id;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;

    /**
     * 设备的数据库ID
     */
    @Schema(description = "设备的数据库ID")
    private int terminalDbId;

    /**
     * 通道ID
     */
    @Schema(description = "通道ID")
    private Integer channelId;

    /**
     * 是否含有音频
     */
    @Schema(description = "是否含有音频")
    private boolean hasAudio;


    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "流信息")
    private String stream;

    // 国标28181信息
    @Schema(description = "国标-编码")
    private String gbDeviceId;

    @Schema(description = "国标-名称")
    private String gbName;

    @Schema(description = "国标-设备厂商")
    private String gbManufacturer;

    @Schema(description = "国标-设备型号")
    private String gbModel;

    @Schema(description = "国标-行政区域")
    private String gbCivilCode;

    @Schema(description = "国标-警区")
    private String gbBlock;

    @Schema(description = "国标-安装地址")
    private String gbAddress;

    @Schema(description = "国标-是否有子设备")
    private Boolean gbParental;

    @Schema(description = "国标-父节点ID")
    private String gbParentId;

    @Schema(description = "国标-注册方式")
    private Integer gbRegisterWay;

    @Schema(description = "国标-编码")
    private String gbSecurityLevelCode;

    @Schema(description = "国标-编码")
    private String gbSecrecy;

    @Schema(description = "国标-编码")
    private String gbIpAddress;

    @Schema(description = "国标-编码")
    private String gbPort;

    @Schema(description = "国标-编码")
    private String gbPassword;

    @Schema(description = "国标-编码")
    private String gbStatus;

    @Schema(description = "国标-编码")
    private String gbLongitude;

    @Schema(description = "国标-编码")
    private String gbLatitude;

    @Schema(description = "国标-编码")
    private String gbBusinessGroupId;

    @Schema(description = "国标-编码")
    private String gbPtzType;

    @Schema(description = "国标-编码")
    private String gbPhotoelectricImagingTyp;

    @Schema(description = "国标-编码")
    private String gbCapturePositionType;

    @Schema(description = "国标-编码")
    private String gbRoomType;

    @Schema(description = "国标-编码")
    private String gbSupplyLightType;

    @Schema(description = "国标-编码")
    private String gbDirectionType;

    @Schema(description = "国标-编码")
    private String gbResolution;

    @Schema(description = "国标-编码")
    private String gbStreamNumberList;

    @Schema(description = "国标-编码")
    private String gbDownloadSpeed;

    @Schema(description = "国标-编码")
    private String gbSvcSpaceSupportMod;

    @Schema(description = "国标-编码")
    private String gbSvcTimeSupportMode;

    @Schema(description = "国标-编码")
    private String gbSsvcRatioSupportList;

    @Schema(description = "国标-编码")
    private String gbMobileDeviceType;

    @Schema(description = "国标-编码")
    private String gbHorizontalFieldAngle;

    @Schema(description = "国标-编码")
    private String gbVerticalFieldAngle;

    @Schema(description = "国标-编码")
    private String gbMaxViewDistance;

    @Schema(description = "国标-编码")
    private String gbGrassrootsCode;

    @Schema(description = "国标-编码")
    private String gbPoType;

    @Schema(description = "国标-编码")
    private String gbPoCommonName;

    @Schema(description = "国标-编码")
    private String gbMac;

    @Schema(description = "国标-编码")
    private String gbFunctionType;

    @Schema(description = "国标-编码")
    private String gbEncodeType;

    @Schema(description = "国标-编码")
    private String gbInstallTime;

    @Schema(description = "国标-编码")
    private String gbManagementUnit;

    @Schema(description = "国标-编码")
    private String gbContactInfo;

    @Schema(description = "国标-编码")
    private String gbRecordSaveDays;

    @Schema(description = "国标-编码")
    private String gbIndustrialClassification;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTerminalDbId() {
        return terminalDbId;
    }

    public void setTerminalDbId(int terminalDbId) {
        this.terminalDbId = terminalDbId;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public boolean getHasAudio() {
        return hasAudio;
    }

    public void setHasAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

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
}
