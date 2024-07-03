package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "国标通道")
public class CommonGBChannel {

    @Schema(description = "国标-数据库自增ID")
    private int gbId;

    @Schema(description = "国标-编码")
    private String gbDeviceId;

    @Schema(description = "国标-名称")
    private String gbName;

    @Schema(description = "国标-设备厂商")
    private String gbManufacturer;

    @Schema(description = "国标-设备型号")
    private String gbModel;

    // 2016
    @Schema(description = "国标-设备归属")
    private String gbOwner;

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

    // 2016
    @Schema(description = "国标-信令安全模式")
    private Integer gbSafetyWay;

    @Schema(description = "国标-注册方式")
    private Integer gbRegisterWay;

    // 2016
    @Schema(description = "国标-证书序列号")
    private Integer gbCertNum;

    // 2016
    @Schema(description = "国标-证书有效标识")
    private Integer gbCertifiable;

    // 2016
    @Schema(description = "国标-无效原因码(有证书且证书无效的设备必选)")
    private Integer gbErrCode;

    // 2016
    @Schema(description = "国标-证书终止有效期(有证书且证书无效的设备必选)")
    private Integer gbEndTime;

    // 2022
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
    private Integer gbPtzType;

    // 2016
    @Schema(description = "-摄像机位置类型扩展。1-省际检查站、2-党政机关、3-车站码头、4-中心广场、5-体育场馆、6-商业中心、7-宗教场所、" +
            "8-校园周边、9-治安复杂区域、10-交通干线。当目录项为摄像机时可选。")
    private Integer PositionType;

    @Schema(description = "国标-摄像机光电成像类型。1-可见光成像;2-热成像;3-雷达成像;4-X光成像;5-深度光场成像;9-其他。可多值,")
    private String gbPhotoelectricImagingTyp;

    @Schema(description = "国标-摄像机采集部位类型")
    private String gbCapturePositionType;

    @Schema(description = "国标-摄像机安装位置室外、室内属性。1-室外、2-室内。")
    private Integer gbRoomType;

    // 2016
    @Schema(description = "国标-用途属性")
    private Integer gbUseType;

    @Schema(description = "国标-摄像机补光属性。1-无补光;2-红外补光;3-白光补光;4-激光补光;9-其他")
    private Integer gbSupplyLightType;

    @Schema(description = "国标-摄像机监视方位(光轴方向)属性。1-东(西向东)、2-西(东向西)、3-南(北向南)、4-北(南向北)、" +
            "5-东南(西北到东南)、6-东北(西南到东北)、7-西南(东北到西南)、8-西北(东南到西北)")
    private Integer gbDirectionType;

    @Schema(description = "国标-摄像机支持的分辨率,可多值")
    private String gbResolution;

    // 2022
    @Schema(description = "国标-摄像机支持的码流编号列表,用于实时点播时指定码流编号(可选)")
    private String gbStreamNumberList;

    @Schema(description = "国标-下载倍速(可选),可多值")
    private String gbDownloadSpeed;

    @Schema(description = "国标-空域编码能力,取值0-不支持;1-1级增强(1个增强层);2-2级增强(2个增强层);3-3级增强(3个增强层)")
    private Integer gbSvcSpaceSupportMod;

    @Schema(description = "国标-时域编码能力,取值0-不支持;1-1级增强;2-2级增强;3-3级增强(可选)")
    private Integer gbSvcTimeSupportMode;

    // 2022
    @Schema(description = "国标- SSVC增强层与基本层比例能力 ")
    private String gbSsvcRatioSupportList;

    // 2022
    @Schema(description = "国标-移动采集设备类型(仅移动采集设备适用,必选);1-移动机器人载摄像机;2-执法记录仪;3-移动单兵设备;" +
            "4-车载视频记录设备;5-无人机载摄像机;9-其他")
    private Integer gbMobileDeviceType;

    // 2022
    @Schema(description = "国标-摄像机水平视场角(可选),取值范围大于0度小于等于360度")
    private Double gbHorizontalFieldAngle;

    // 2022
    @Schema(description = "国标-摄像机竖直视场角(可选),取值范围大于0度小于等于360度 ")
    private Double gbVerticalFieldAngle;

    // 2022
    @Schema(description = "国标-摄像机可视距离(可选),单位:米")
    private Double gbMaxViewDistance;

    // 2022
    @Schema(description = "国标-基层组织编码(必选,非基层建设时为“000000”)")
    private String gbGrassrootsCode;

    // 2022
    @Schema(description = "国标-监控点位类型(当为摄像机时必选),1-一类视频监控点;2-二类视频监控点;3-三类视频监控点;9-其他点位。")
    private Integer gbPoType;

    // 2022
    @Schema(description = "国标-点位俗称")
    private String gbPoCommonName;

    // 2022
    @Schema(description = "国标-设备MAC地址(可选),用“XX-XX-XX-XX-XX-XX”格式表达")
    private String gbMac;

    // 2022
    @Schema(description = "国标-摄像机卡口功能类型,01-人脸卡口;02-人员卡口;03-机动车卡口;04-非机动车卡口;05-物品卡口;99-其他")
    private String gbFunctionType;

    // 2022
    @Schema(description = "国标-摄像机视频编码格式")
    private String gbEncodeType;

    // 2022
    @Schema(description = "国标-摄像机安装使用时间")
    private String gbInstallTime;

    // 2022
    @Schema(description = "国标-摄像机所属管理单位名称")
    private String gbManagementUnit;

    // 2022
    @Schema(description = "国标-摄像机所属管理单位联系人的联系方式(电话号码,可多值,用英文半角“/”分割)")
    private String gbContactInfo;

    // 2022
    @Schema(description = "国标-录像保存天数(可选)")
    private Integer gbRecordSaveDays;

    // 2022
    @Schema(description = "国标-国民经济行业分类代码(可选)")
    private String gbIndustrialClassification;

    @Schema(description = "关联的国标设备数据库ID")
    private Integer gbDeviceDbId;

    @Schema(description = "关联的推流Id（流来源是推流时有效）")
    private Integer streamPushId;

    @Schema(description = "关联的拉流代理Id（流来源是拉流代理时有效）")
    private Integer streamProxyId;

    public String encode(){
        return encode(null);
    }
    public String encode(String event){
        String content;
        if (event == null) {
            return getFullContent(null);
        }
        switch (event) {
            case CatalogEvent.DEL:
            case CatalogEvent.DEFECT:
            case CatalogEvent.VLOST:
                content = "<Item>\n" +
                        "<DeviceID>" + this.getGbDeviceId() + "</DeviceID>\n" +
                        "<Event>" + event + "</Event>\n" +
                        "</Item>\n";
                break;
            case CatalogEvent.ON:
            case CatalogEvent.OFF:
                content = "<Item>\n" +
                        "<DeviceID>" + this.getGbDeviceId() + "</DeviceID>\n" +
                        "<Event>" + event + "</Event>\r\n" +
                        "</Item>\n";
                break;
            case CatalogEvent.ADD:
            case CatalogEvent.UPDATE:
                content = getFullContent(event);
                break;
            default:
                content = null;
                break;
        }
        return content;
    }

    private String getFullContent(String event) {
        StringBuilder content = new StringBuilder();
        // 行政区划目录项
        content.append("<Item>\n")
                .append("<DeviceID>" + this.getGbDeviceId() + "</DeviceID>\n")
                .append("<Name>" + this.getGbName() + "</Name>\n");

        if (this.getGbDeviceId().length() > 8) {

            String type = this.getGbDeviceId().substring(10,13);
            if (type.equals("200")) {
                // 业务分组目录项
                content.append("<Manufacturer>" + this.getGbManufacturer() + "</Manufacturer>\n")
                        .append("<Model>" + this.getGbModel() + "</Model>\n")
                        .append("<Owner>" + this.getGbOwner() + "</Owner>\n")
                        .append("<CivilCode>" + this.getGbCivilCode() + "</CivilCode>\n")
                        .append("<Address>" + this.getGbAddress() + "</Address>\n")
                        .append("<RegisterWay>" + this.getGbRegisterWay() + "</RegisterWay>\n")
                        .append("<Secrecy>" + this.getGbSecrecy() + "</Secrecy>\n");
            }else if (type.equals("215")) {
                // 业务分组
                content.append("<ParentID>" + this.getGbParentId() + "</ParentID>\n");
            }else if (type.equals("216")) {
                // 虚拟组织目录项
                content.append("<ParentID>" + this.getGbParentId() + "</ParentID>\n")
                        .append("<BusinessGroupID>" + this.getGbBusinessGroupId() + "</BusinessGroupID>\n");
            }else {
                // 设备目录项
                content.append("<Manufacturer>" + this.getGbManufacturer() + "</Manufacturer>\n")
                        .append("<Model>" + this.getGbModel() + "</Model>\n")
                        .append("<Owner>" + this.getGbOwner() + "</Owner>\n")
                        .append("<CivilCode>" + this.getGbCivilCode() + "</CivilCode>\n")
                        .append("<Block>" + this.getGbBlock() + "</Block>\n")
                        .append("<Address>" + this.getGbAddress() + "</Address>\n")
                        .append("<Parental>" + this.getGbParental() + "</Parental>\n")
                        .append("<ParentID>" + this.getGbParentId() + "</ParentID>\n")
                        .append("<SafetyWay>" + this.getGbSafetyWay() + "</SafetyWay>\n")
                        .append("<RegisterWay>" + this.getGbRegisterWay() + "</RegisterWay>\n")
                        .append("<CertNum>" + this.getGbCertNum() + "</CertNum>\n")
                        .append("<Certifiable>" + this.getGbCertifiable() + "</Certifiable>\n")
                        .append("<ErrCode>" + this.getGbErrCode() + "</ErrCode>\n")
                        .append("<EndTime>" + this.getGbEndTime() + "</EndTime>\n")
                        .append("<Secrecy>" + this.getGbSecrecy() + "</Secrecy>\n")
                        .append("<IPAddress>" + this.getGbIpAddress() + "</IPAddress>\n")
                        .append("<Port>" + this.getGbPort() + "</Port>\n")
                        .append("<Password>" + this.getGbPassword() + "</Password>\n")
                        .append("<Status>" + this.getGbStatus() + "</Status>\n")
                        .append("<Longitude>" + this.getGbLongitude() + "</Longitude>\n")
                        .append("<Latitude>" + this.getGbLatitude() + "</Latitude>\n")
                        .append("<Info>\n")
                        .append("  <PTZType>" + this.getGbPtzType() + "</PTZType>\n")
                        .append("  <PositionType>" + this.getPositionType() + "</PositionType>\n")
                        .append("  <RoomType>" + this.getGbRoomType() + "</RoomType>\n")
                        .append("  <UseType>" + this.getGbUseType() + "</UseType>\n")
                        .append("  <SupplyLightType>" + this.getGbSupplyLightType() + "</SupplyLightType>\n")
                        .append("  <DirectionType>" + this.getGbDirectionType() + "</DirectionType>\n")
                        .append("  <Resolution>" + this.getGbResolution() + "</Resolution>\n")
                        .append("  <BusinessGroupID>" + this.getGbBusinessGroupId() + "</BusinessGroupID>\n")
                        .append("  <DownloadSpeed>" + this.getGbDownloadSpeed() + "</DownloadSpeed>\n")
                        .append("  <SVCSpaceSupportMode>" + this.getGbSvcSpaceSupportMod() + "</SVCSpaceSupportMode>\n")
                        .append("  <SVCTimeSupportMode>" + this.getGbSvcTimeSupportMode() + "</SVCTimeSupportMode>\n")
                        .append("</Info>")
                ;
            }
        }
        if (event != null) {
            content.append("<Event>" + event + "</Event>\n");
        }
        content.append("</Item>\n");
        return content.toString();
    }

}
