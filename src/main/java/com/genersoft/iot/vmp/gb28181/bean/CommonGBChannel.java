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
    private Integer gbParental;

    @Schema(description = "国标-父节点ID")
    private String gbParentId;

    // 2016
    @Schema(description = "国标-信令安全模式")
    private Integer gbSafetyWay;

    @Schema(description = "国标-注册方式")
    private Integer gbRegisterWay;

    // 2016
    @Schema(description = "国标-证书序列号")
    private String gbCertNum;

    // 2016
    @Schema(description = "国标-证书有效标识")
    private Integer gbCertifiable;

    // 2016
    @Schema(description = "国标-无效原因码(有证书且证书无效的设备必选)")
    private Integer gbErrCode;

    // 2016
    @Schema(description = "国标-证书终止有效期(有证书且证书无效的设备必选)")
    private String gbEndTime;

    @Schema(description = "国标-保密属性(必选)缺省为0;0-不涉密,1-涉密")
    private Integer gbSecrecy;

    @Schema(description = "国标-设备/系统IPv4/IPv6地址")
    private String gbIpAddress;

    @Schema(description = "国标-设备/系统端口")
    private Integer gbPort;

    @Schema(description = "国标-设备口令")
    private String gbPassword;

    @Schema(description = "国标-设备状态")
    private String gbStatus;

    @Schema(description = "国标-经度 WGS-84坐标系")
    private Double gbLongitude;

    @Schema(description = "国标-纬度 WGS-84坐标系")
    private Double gbLatitude;

    @Schema(description = "")
    private Double gpsAltitude;

    @Schema(description = "")
    private Double gpsSpeed;

    @Schema(description = "")
    private Double gpsDirection;

    @Schema(description = "")
    private String gpsTime;

    @Schema(description = "国标-虚拟组织所属的业务分组ID")
    private String gbBusinessGroupId;

    @Schema(description = "国标-摄像机结构类型,标识摄像机类型: 1-球机; 2-半球; 3-固定枪机; 4-遥控枪机;5-遥控半球;6-多目设备的全景/拼接通道;7-多目设备的分割通道")
    private Integer gbPtzType;

    // 2016
    @Schema(description = "-摄像机位置类型扩展。1-省际检查站、2-党政机关、3-车站码头、4-中心广场、5-体育场馆、6-商业中心、7-宗教场所、" +
            "8-校园周边、9-治安复杂区域、10-交通干线。当目录项为摄像机时可选。")
    private Integer gbPositionType;

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

    @Schema(description = "国标-下载倍速(可选),可多值")
    private String gbDownloadSpeed;

    @Schema(description = "国标-空域编码能力,取值0-不支持;1-1级增强(1个增强层);2-2级增强(2个增强层);3-3级增强(3个增强层)")
    private Integer gbSvcSpaceSupportMod;

    @Schema(description = "国标-时域编码能力,取值0-不支持;1-1级增强;2-2级增强;3-3级增强(可选)")
    private Integer gbSvcTimeSupportMode;

    @Schema(description = "二进制保存的录制计划, 每一位表示每个小时的前半个小时")
    private Long recordPLan;

    @Schema(description = "关联的数据类型")
    private Integer dataType;

    @Schema(description = "关联的设备ID")
    private Integer dataDeviceId;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    public String encode(String serverDeviceId) {
        return encode(null, serverDeviceId);
    }

    public String encode(String event,String serverDeviceId) {
        String content;
        if (event == null) {
            return getFullContent(null, serverDeviceId);
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
                content = getFullContent(event, serverDeviceId);
                break;
            default:
                content = null;
                break;
        }
        return content;
    }

    private String getFullContent(String event, String serverDeviceId) {
        StringBuilder content = new StringBuilder();
        // 行政区划目录项
        content.append("<Item>\n")
                .append("<DeviceID>" + this.getGbDeviceId() + "</DeviceID>\n")
                .append("<Name>" + this.getGbName() + "</Name>\n");


        if (this.getGbDeviceId().length() > 8) {

            String type = this.getGbDeviceId().substring(10, 13);
            if (type.equals("200")) {
                // 业务分组目录项
                if (this.getGbManufacturer() != null) {
                    content.append("<Manufacturer>" + this.getGbManufacturer() + "</Manufacturer>\n");
                }
                if (this.getGbModel() != null) {
                    content.append("<Model>" + this.getGbModel() + "</Model>\n");
                }
                if (this.getGbOwner() != null) {
                    content.append("<Owner>" + this.getGbOwner() + "</Owner>\n");
                }
                if (this.getGbCivilCode() != null) {
                    content.append("<CivilCode>" + this.getGbCivilCode() + "</CivilCode>\n");
                }
                if (this.getGbAddress() != null) {
                    content.append("<Address>" + this.getGbAddress() + "</Address>\n");
                }
                if (this.getGbRegisterWay() != null) {
                    content.append("<RegisterWay>" + this.getGbRegisterWay() + "</RegisterWay>\n");
                }
                if (this.getGbSecrecy() != null) {
                    content.append("<Secrecy>" + this.getGbSecrecy() + "</Secrecy>\n");
                }
            } else if (type.equals("215")) {
                // 业务分组
                if (this.getGbCivilCode() != null) {
                    content.append("<CivilCode>" + this.getGbCivilCode() + "</CivilCode>\n");
                }
                content.append("<ParentID>" + serverDeviceId + "</ParentID>\n");
            } else if (type.equals("216")) {
                // 虚拟组织目录项
                if (this.getGbCivilCode() != null) {
                    content.append("<CivilCode>" + this.getGbCivilCode() + "</CivilCode>\n");
                }
                if (this.getGbParentId() != null) {
                    content.append("<ParentID>" + this.getGbParentId() + "</ParentID>\n");
                }
                content.append("<BusinessGroupID>" + this.getGbBusinessGroupId() + "</BusinessGroupID>\n");
            } else {
                if (this.getGbManufacturer() != null) {
                    content.append("<Manufacturer>" + this.getGbManufacturer() + "</Manufacturer>\n");
                }
                if (this.getGbModel() != null) {
                    content.append("<Model>" + this.getGbModel() + "</Model>\n");
                }
                if (this.getGbOwner() != null) {
                    content.append("<Owner>" + this.getGbOwner() + "</Owner>\n");
                }
                if (this.getGbCivilCode() != null) {
                    content.append("<CivilCode>" + this.getGbCivilCode() + "</CivilCode>\n");
                }
                if (this.getGbAddress() != null) {
                    content.append("<Address>" + this.getGbAddress() + "</Address>\n");
                }
                if (this.getGbRegisterWay() != null) {
                    content.append("<RegisterWay>" + this.getGbRegisterWay() + "</RegisterWay>\n");
                }
                if (this.getGbSecrecy() != null) {
                    content.append("<Secrecy>" + this.getGbSecrecy() + "</Secrecy>\n");
                }
                if (this.getGbParentId() != null) {
                    content.append("<ParentID>" + this.getGbParentId() + "</ParentID>\n");
                }
                if (this.getGbParental() != null) {
                    content.append("<Parental>" + this.getGbParental() + "</Parental>\n");
                }
                if (this.getGbSafetyWay() != null) {
                    content.append("<SafetyWay>" + this.getGbSafetyWay() + "</SafetyWay>\n");
                }
                if (this.getGbRegisterWay() != null) {
                    content.append("<RegisterWay>" + this.getGbRegisterWay() + "</RegisterWay>\n");
                }
                if (this.getGbCertNum() != null) {
                    content.append("<CertNum>" + this.getGbCertNum() + "</CertNum>\n");
                }
                if (this.getGbCertifiable() != null) {
                    content.append("<Certifiable>" + this.getGbCertifiable() + "</Certifiable>\n");
                }
                if (this.getGbErrCode() != null) {
                    content.append("<ErrCode>" + this.getGbErrCode() + "</ErrCode>\n");
                }
                if (this.getGbEndTime() != null) {
                    content.append("<EndTime>" + this.getGbEndTime() + "</EndTime>\n");
                }
                if (this.getGbSecrecy() != null) {
                    content.append("<Secrecy>" + this.getGbSecrecy() + "</Secrecy>\n");
                }
                if (this.getGbIpAddress() != null) {
                    content.append("<IPAddress>" + this.getGbIpAddress() + "</IPAddress>\n");
                }
                if (this.getGbPort() != null) {
                    content.append("<Port>" + this.getGbPort() + "</Port>\n");
                }
                if (this.getGbPassword() != null) {
                    content.append("<Password>" + this.getGbPassword() + "</Password>\n");
                }
                if (this.getGbStatus() != null) {
                    content.append("<Status>" + this.getGbStatus() + "</Status>\n");
                }
                if (this.getGbLongitude() != null) {
                    content.append("<Longitude>" + this.getGbLongitude() + "</Longitude>\n");
                }
                if (this.getGbLatitude() != null) {
                    content.append("<Latitude>" + this.getGbLatitude() + "</Latitude>\n");
                }
                content.append("<Info>\n");

                if (this.getGbPtzType() != null) {
                    content.append("  <PTZType>" + this.getGbPtzType() + "</PTZType>\n");
                }
                if (this.getGbPositionType() != null) {
                    content.append("  <PositionType>" + this.getGbPositionType() + "</PositionType>\n");
                }
                if (this.getGbRoomType() != null) {
                    content.append("  <RoomType>" + this.getGbRoomType() + "</RoomType>\n");
                }
                if (this.getGbUseType() != null) {
                    content.append("  <UseType>" + this.getGbUseType() + "</UseType>\n");
                }
                if (this.getGbSupplyLightType() != null) {
                    content.append("  <SupplyLightType>" + this.getGbSupplyLightType() + "</SupplyLightType>\n");
                }
                if (this.getGbDirectionType() != null) {
                    content.append("  <DirectionType>" + this.getGbDirectionType() + "</DirectionType>\n");
                }
                if (this.getGbResolution() != null) {
                    content.append("  <Resolution>" + this.getGbResolution() + "</Resolution>\n");
                }
                if (this.getGbBusinessGroupId() != null) {
                    content.append("  <BusinessGroupID>" + this.getGbBusinessGroupId() + "</BusinessGroupID>\n");
                }
                if (this.getGbDownloadSpeed() != null) {
                    content.append("  <DownloadSpeed>" + this.getGbDownloadSpeed() + "</DownloadSpeed>\n");
                }
                if (this.getGbSvcSpaceSupportMod() != null) {
                    content.append("  <SVCSpaceSupportMode>" + this.getGbSvcSpaceSupportMod() + "</SVCSpaceSupportMode>\n");
                }
                if (this.getGbSvcTimeSupportMode() != null) {
                    content.append("  <SVCTimeSupportMode>" + this.getGbSvcTimeSupportMode() + "</SVCTimeSupportMode>\n");
                }
                content.append("</Info>\n");
            }
        }
        if (event != null) {
            content.append("<Event>" + event + "</Event>\n");
        }
        content.append("</Item>\n");
        return content.toString();
    }

    public static CommonGBChannel build(Group group) {
        GbCode gbCode = GbCode.decode(group.getDeviceId());
        CommonGBChannel channel = new CommonGBChannel();
        if (gbCode.getTypeCode().equals("215")) {
            // 业务分组
            channel.setGbName(group.getName());
            channel.setGbDeviceId(group.getDeviceId());
            channel.setGbCivilCode(group.getCivilCode());
        } else {
            // 虚拟组织
            channel.setGbName(group.getName());
            channel.setGbDeviceId(group.getDeviceId());
            channel.setGbParentId(group.getParentDeviceId());
            channel.setGbBusinessGroupId(group.getBusinessGroup());
            channel.setGbCivilCode(group.getCivilCode());
        }
        return channel;
    }

    public static CommonGBChannel build(Platform platform) {
        CommonGBChannel commonGBChannel = new CommonGBChannel();
        commonGBChannel.setGbDeviceId(platform.getDeviceGBId());
        commonGBChannel.setGbName(platform.getName());
        commonGBChannel.setGbManufacturer(platform.getManufacturer());
        commonGBChannel.setGbModel(platform.getModel());
        commonGBChannel.setGbCivilCode(platform.getCivilCode());
        commonGBChannel.setGbAddress(platform.getAddress());
        commonGBChannel.setGbRegisterWay(platform.getRegisterWay());
        commonGBChannel.setGbSecrecy(platform.getSecrecy());
        commonGBChannel.setGbStatus(platform.isStatus() ? "ON" : "OFF");
        return commonGBChannel;
    }

    public static CommonGBChannel build(Region region) {
        CommonGBChannel commonGBChannel = new CommonGBChannel();
        commonGBChannel.setGbDeviceId(region.getDeviceId());
        commonGBChannel.setGbName(region.getName());
        return commonGBChannel;
    }

}
