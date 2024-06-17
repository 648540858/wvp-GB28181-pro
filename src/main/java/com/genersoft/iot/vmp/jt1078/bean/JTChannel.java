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

    @Schema(description = "国标-摄像机安全能力等级代码")
    private String gbSecurityLevelCode;

    @Schema(description = "国标-保密属性(必选)缺省为0;0-不涉密,1-涉密")
    private Integer gbSecrecy;

    @Schema(description = "国标-设备/系统IPv4/IPv6地址")
    private String gbIpAddress;

    @Schema(description = "国标-设备/系统端口")
    private Integer gbPort;

    @Schema(description = "国标-设备口令")
    private String gbPassword;

    @Schema(description = "国标-设备状态")
    private Boolean gbStatus;

    @Schema(description = "国标-经度 WGS-84坐标系")
    private Double gbLongitude;

    @Schema(description = "国标-,纬度 WGS-84坐标系")
    private Double gbLatitude;

    @Schema(description = "国标-虚拟组织所属的业务分组ID")
    private String gbBusinessGroupId;

    @Schema(description = "国标-摄像机结构类型,标识摄像机类型: 1-球机; 2-半球; 3-固定枪机; 4-遥控枪机;5-遥控半球;6-多目设备的全景/拼接通道;7-多目设备的分割通道")
    private String gbPtzType;

    @Schema(description = "国标-摄像机光电成像类型。1-可见光成像;2-热成像;3-雷达成像;4-X光成像;5-深度光场成像;9-其他。可多值,")
    private String gbPhotoelectricImagingTyp;

    @Schema(description = "国标-摄像机采集部位类型")
    private String gbCapturePositionType;

    @Schema(description = "国标-摄像机安装位置室外、室内属性。1-室外、2-室内。")
    private Integer gbRoomType;

    @Schema(description = "国标-摄像机补光属性。1-无补光;2-红外补光;3-白光补光;4-激光补光;9-其他")
    private Integer gbSupplyLightType;

    @Schema(description = "国标-摄像机监视方位(光轴方向)属性。1-东(西向东)、2-西(东向西)、3-南(北向南)、4-北(南向北)、" +
            "5-东南(西北到东南)、6-东北(西南到东北)、7-西南(东北到西南)、8-西北(东南到西北)")
    private Integer gbDirectionType;

    @Schema(description = "国标-摄像机支持的分辨率,可多值")
    private String gbResolution;

    @Schema(description = "国标-摄像机支持的码流编号列表,用于实时点播时指定码流编号(可选)")
    private String gbStreamNumberList;

    @Schema(description = "国标-下载倍速(可选),可多值")
    private String gbDownloadSpeed;

    @Schema(description = "国标-空域编码能力,取值0-不支持;1-1级增强(1个增强层);2-2级增强(2个增强层);3-3级增强(3个增强层)")
    private Integer gbSvcSpaceSupportMod;

    @Schema(description = "国标-时域编码能力,取值0-不支持;1-1级增强;2-2级增强;3-3级增强(可选)")
    private Integer gbSvcTimeSupportMode;

    @Schema(description = "国标- SSVC增强层与基本层比例能力 ")
    private String gbSsvcRatioSupportList;

    @Schema(description = "国标-移动采集设备类型(仅移动采集设备适用,必选);1-移动机器人载摄像机;2-执法记录仪;3-移动单兵设备;" +
            "4-车载视频记录设备;5-无人机载摄像机;9-其他")
    private Integer gbMobileDeviceType;

    @Schema(description = "国标-摄像机水平视场角(可选),取值范围大于0度小于等于360度")
    private Double gbHorizontalFieldAngle;

    @Schema(description = "国标-摄像机竖直视场角(可选),取值范围大于0度小于等于360度 ")
    private Double gbVerticalFieldAngle;

    @Schema(description = "国标-摄像机可视距离(可选),单位:米")
    private Double gbMaxViewDistance;

    @Schema(description = "国标-基层组织编码(必选,非基层建设时为“000000”)")
    private String gbGrassrootsCode;

    @Schema(description = "国标-监控点位类型(当为摄像机时必选),1-一类视频监控点;2-二类视频监控点;3-三类视频监控点;9-其他点位。")
    private Integer gbPoType;

    @Schema(description = "国标-点位俗称")
    private String gbPoCommonName;

    @Schema(description = "国标-设备MAC地址(可选),用“XX-XX-XX-XX-XX-XX”格式表达")
    private String gbMac;

    @Schema(description = "国标-摄像机卡口功能类型,01-人脸卡口;02-人员卡口;03-机动车卡口;04-非机动车卡口;05-物品卡口;99-其他")
    private String gbFunctionType;

    @Schema(description = "国标-摄像机视频编码格式")
    private String gbEncodeType;

    @Schema(description = "国标-摄像机安装使用时间")
    private String gbInstallTime;

    @Schema(description = "国标-摄像机所属管理单位名称")
    private String gbManagementUnit;

    @Schema(description = "国标-摄像机所属管理单位联系人的联系方式(电话号码,可多值,用英文半角“/”分割)")
    private String gbContactInfo;

    @Schema(description = "国标-录像保存天数(可选)")
    private Integer gbRecordSaveDays;

    @Schema(description = "国标-国民经济行业分类代码(可选)")
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


    public boolean isHasAudio() {
        return hasAudio;
    }

    public String getGbDeviceId() {
        return gbDeviceId;
    }

    public void setGbDeviceId(String gbDeviceId) {
        this.gbDeviceId = gbDeviceId;
    }

    public String getGbName() {
        return gbName;
    }

    public void setGbName(String gbName) {
        this.gbName = gbName;
    }

    public String getGbManufacturer() {
        return gbManufacturer;
    }

    public void setGbManufacturer(String gbManufacturer) {
        this.gbManufacturer = gbManufacturer;
    }

    public String getGbModel() {
        return gbModel;
    }

    public void setGbModel(String gbModel) {
        this.gbModel = gbModel;
    }

    public String getGbCivilCode() {
        return gbCivilCode;
    }

    public void setGbCivilCode(String gbCivilCode) {
        this.gbCivilCode = gbCivilCode;
    }

    public String getGbBlock() {
        return gbBlock;
    }

    public void setGbBlock(String gbBlock) {
        this.gbBlock = gbBlock;
    }

    public String getGbAddress() {
        return gbAddress;
    }

    public void setGbAddress(String gbAddress) {
        this.gbAddress = gbAddress;
    }

    public Boolean getGbParental() {
        return gbParental;
    }

    public void setGbParental(Boolean gbParental) {
        this.gbParental = gbParental;
    }

    public String getGbParentId() {
        return gbParentId;
    }

    public void setGbParentId(String gbParentId) {
        this.gbParentId = gbParentId;
    }

    public Integer getGbRegisterWay() {
        return gbRegisterWay;
    }

    public void setGbRegisterWay(Integer gbRegisterWay) {
        this.gbRegisterWay = gbRegisterWay;
    }

    public String getGbSecurityLevelCode() {
        return gbSecurityLevelCode;
    }

    public void setGbSecurityLevelCode(String gbSecurityLevelCode) {
        this.gbSecurityLevelCode = gbSecurityLevelCode;
    }

    public Integer getGbSecrecy() {
        return gbSecrecy;
    }

    public void setGbSecrecy(Integer gbSecrecy) {
        this.gbSecrecy = gbSecrecy;
    }

    public String getGbIpAddress() {
        return gbIpAddress;
    }

    public void setGbIpAddress(String gbIpAddress) {
        this.gbIpAddress = gbIpAddress;
    }

    public Integer getGbPort() {
        return gbPort;
    }

    public void setGbPort(Integer gbPort) {
        this.gbPort = gbPort;
    }

    public String getGbPassword() {
        return gbPassword;
    }

    public void setGbPassword(String gbPassword) {
        this.gbPassword = gbPassword;
    }

    public Boolean getGbStatus() {
        return gbStatus;
    }

    public void setGbStatus(Boolean gbStatus) {
        this.gbStatus = gbStatus;
    }

    public Double getGbLongitude() {
        return gbLongitude;
    }

    public void setGbLongitude(Double gbLongitude) {
        this.gbLongitude = gbLongitude;
    }

    public Double getGbLatitude() {
        return gbLatitude;
    }

    public void setGbLatitude(Double gbLatitude) {
        this.gbLatitude = gbLatitude;
    }

    public String getGbBusinessGroupId() {
        return gbBusinessGroupId;
    }

    public void setGbBusinessGroupId(String gbBusinessGroupId) {
        this.gbBusinessGroupId = gbBusinessGroupId;
    }

    public String getGbPtzType() {
        return gbPtzType;
    }

    public void setGbPtzType(String gbPtzType) {
        this.gbPtzType = gbPtzType;
    }

    public String getGbPhotoelectricImagingTyp() {
        return gbPhotoelectricImagingTyp;
    }

    public void setGbPhotoelectricImagingTyp(String gbPhotoelectricImagingTyp) {
        this.gbPhotoelectricImagingTyp = gbPhotoelectricImagingTyp;
    }

    public String getGbCapturePositionType() {
        return gbCapturePositionType;
    }

    public void setGbCapturePositionType(String gbCapturePositionType) {
        this.gbCapturePositionType = gbCapturePositionType;
    }

    public Integer getGbRoomType() {
        return gbRoomType;
    }

    public void setGbRoomType(Integer gbRoomType) {
        this.gbRoomType = gbRoomType;
    }

    public Integer getGbSupplyLightType() {
        return gbSupplyLightType;
    }

    public void setGbSupplyLightType(Integer gbSupplyLightType) {
        this.gbSupplyLightType = gbSupplyLightType;
    }

    public Integer getGbDirectionType() {
        return gbDirectionType;
    }

    public void setGbDirectionType(Integer gbDirectionType) {
        this.gbDirectionType = gbDirectionType;
    }

    public String getGbResolution() {
        return gbResolution;
    }

    public void setGbResolution(String gbResolution) {
        this.gbResolution = gbResolution;
    }

    public String getGbStreamNumberList() {
        return gbStreamNumberList;
    }

    public void setGbStreamNumberList(String gbStreamNumberList) {
        this.gbStreamNumberList = gbStreamNumberList;
    }

    public String getGbDownloadSpeed() {
        return gbDownloadSpeed;
    }

    public void setGbDownloadSpeed(String gbDownloadSpeed) {
        this.gbDownloadSpeed = gbDownloadSpeed;
    }

    public Integer getGbSvcSpaceSupportMod() {
        return gbSvcSpaceSupportMod;
    }

    public void setGbSvcSpaceSupportMod(Integer gbSvcSpaceSupportMod) {
        this.gbSvcSpaceSupportMod = gbSvcSpaceSupportMod;
    }

    public Integer getGbSvcTimeSupportMode() {
        return gbSvcTimeSupportMode;
    }

    public void setGbSvcTimeSupportMode(Integer gbSvcTimeSupportMode) {
        this.gbSvcTimeSupportMode = gbSvcTimeSupportMode;
    }

    public String getGbSsvcRatioSupportList() {
        return gbSsvcRatioSupportList;
    }

    public void setGbSsvcRatioSupportList(String gbSsvcRatioSupportList) {
        this.gbSsvcRatioSupportList = gbSsvcRatioSupportList;
    }

    public Integer getGbMobileDeviceType() {
        return gbMobileDeviceType;
    }

    public void setGbMobileDeviceType(Integer gbMobileDeviceType) {
        this.gbMobileDeviceType = gbMobileDeviceType;
    }

    public Double getGbHorizontalFieldAngle() {
        return gbHorizontalFieldAngle;
    }

    public void setGbHorizontalFieldAngle(Double gbHorizontalFieldAngle) {
        this.gbHorizontalFieldAngle = gbHorizontalFieldAngle;
    }

    public Double getGbVerticalFieldAngle() {
        return gbVerticalFieldAngle;
    }

    public void setGbVerticalFieldAngle(Double gbVerticalFieldAngle) {
        this.gbVerticalFieldAngle = gbVerticalFieldAngle;
    }

    public Double getGbMaxViewDistance() {
        return gbMaxViewDistance;
    }

    public void setGbMaxViewDistance(Double gbMaxViewDistance) {
        this.gbMaxViewDistance = gbMaxViewDistance;
    }

    public String getGbGrassrootsCode() {
        return gbGrassrootsCode;
    }

    public void setGbGrassrootsCode(String gbGrassrootsCode) {
        this.gbGrassrootsCode = gbGrassrootsCode;
    }

    public Integer getGbPoType() {
        return gbPoType;
    }

    public void setGbPoType(Integer gbPoType) {
        this.gbPoType = gbPoType;
    }

    public String getGbPoCommonName() {
        return gbPoCommonName;
    }

    public void setGbPoCommonName(String gbPoCommonName) {
        this.gbPoCommonName = gbPoCommonName;
    }

    public String getGbMac() {
        return gbMac;
    }

    public void setGbMac(String gbMac) {
        this.gbMac = gbMac;
    }

    public String getGbFunctionType() {
        return gbFunctionType;
    }

    public void setGbFunctionType(String gbFunctionType) {
        this.gbFunctionType = gbFunctionType;
    }

    public String getGbEncodeType() {
        return gbEncodeType;
    }

    public void setGbEncodeType(String gbEncodeType) {
        this.gbEncodeType = gbEncodeType;
    }

    public String getGbInstallTime() {
        return gbInstallTime;
    }

    public void setGbInstallTime(String gbInstallTime) {
        this.gbInstallTime = gbInstallTime;
    }

    public String getGbManagementUnit() {
        return gbManagementUnit;
    }

    public void setGbManagementUnit(String gbManagementUnit) {
        this.gbManagementUnit = gbManagementUnit;
    }

    public String getGbContactInfo() {
        return gbContactInfo;
    }

    public void setGbContactInfo(String gbContactInfo) {
        this.gbContactInfo = gbContactInfo;
    }

    public Integer getGbRecordSaveDays() {
        return gbRecordSaveDays;
    }

    public void setGbRecordSaveDays(Integer gbRecordSaveDays) {
        this.gbRecordSaveDays = gbRecordSaveDays;
    }

    public String getGbIndustrialClassification() {
        return gbIndustrialClassification;
    }

    public void setGbIndustrialClassification(String gbIndustrialClassification) {
        this.gbIndustrialClassification = gbIndustrialClassification;
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
