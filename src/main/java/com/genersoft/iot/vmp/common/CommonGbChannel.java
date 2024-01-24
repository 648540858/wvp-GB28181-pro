package com.genersoft.iot.vmp.common;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Gb28181CodeType;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPush;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxy;
import com.genersoft.iot.vmp.service.bean.CommonGbChannelType;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.service.impl.CommonGbChannelServiceImpl;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CommonGbChannel {

    private final static Logger logger = LoggerFactory.getLogger(CommonGbChannel.class);


    /**
     * 国标字段：自增ID
     */
    @Schema(description = "自增ID")
    private int commonGbId;

    /**
     * 国标字段：设备/区域/系统编码(必选)
     */
    @Schema(description = "设备/区域/系统编码(必选)")
    private String commonGbDeviceID;

    /**
     * 国标字段：设备/区域/系统名称(必选)
     */
    @Schema(description = "设备/区域/系统名称(必选)")
    private String commonGbName;

    /**
     * 国标字段：当为设备时,设备厂商(必选)
     */
    @Schema(description = "当为设备时,设备厂商(必选)")
    private String commonGbManufacturer;

    /**
     * 国标字段：当为设备时,设备型号(必选)
     */
    @Schema(description = "当为设备时,设备型号(必选)")
    private String commonGbModel;

    /**
     * 国标字段：当为设备时,设备归属(必选)
     */
    @Schema(description = "当为设备时,设备归属(必选)")
    private String commonGbOwner;

    /**
     * 国标字段：行政区域(必选)
     */
    @Schema(description = "行政区域(必选)")
    private String commonGbCivilCode;

    /**
     * 国标字段：警区(可选)
     */
    @Schema(description = "警区(可选)")
    private String commonGbBlock;

    /**
     * 国标字段：当为设备时,安装地址(必选)
     */
    @Schema(description = "当为设备时,安装地址(必选)")
    private String commonGbAddress;

    /**
     * 国标字段：当为设备时,是否有子设备(必选)1有,0没有
     */
    @Schema(description = "当为设备时,是否有子设备(必选)1有,0没有")
    private Integer commonGbParental;

    /**
     * 国标字段：父设备/区域/系统ID(必选)
     */
    @Schema(description = "父设备/区域/系统ID(必选)")
    private String commonGbParentID;

    /**
     * 国标字段：信令安全模式(可选)缺省为0; 0:不采用;2:S/MIME 签名方式;3:S/
     * MIME加密签名同时采用方式;4:数字摘要方式
     */
    @Schema(description = "信令安全模式(可选)缺省为0; " +
            "0:不采用;" +
            "2:S/MIME 签名方式;" +
            "3:S/MIME加密签名同时采用方式;4:数字摘要方式")
    private Integer commonGbSafetyWay;

    /**
     * 国标字段：注册方式(必选)缺省为1;1:符合IETFRFC3261标准的认证注册模
     * 式;2:基于口令的双向认证注册模式;3:基于数字证书的双向认证注册模式
     */
    @Schema(description = "注册方式(必选)缺省为1;" +
            "1:符合IETFRFC3261标准的认证注册模式;" +
            "2:基于口令的双向认证注册模式;" +
            "3:基于数字证书的双向认证注册模式")
    private Integer commonGbRegisterWay;

    /**
     * 证书序列号(有证书的设备必选)
     */
    @Schema(description = "证书序列号(有证书的设备必选)")
    private String commonGbCertNum;

    /**
     * 证书有效标识(有证书的设备必选)缺省为0;证书有效标识:0:无效 1:有效
     */
    @Schema(description = "证书有效标识(有证书的设备必选)缺省为0;证书有效标识:0:无效 1:有效")
    private Integer commonGbCertifiable;

    /**
     * 无效原因码(有证书且证书无效的设备必选)
     */
    @Schema(description = "无效原因码(有证书且证书无效的设备必选)")
    private Integer commonGbErrCode;

    /**
     * 证书终止有效期(有证书的设备必选)
     */
    @Schema(description = "证书终止有效期(有证书的设备必选)")
    private String commonGbEndTime;

    /**
     * 保密属性(必选)缺省为0;0:不涉密,1:涉密
     */
    @Schema(description = "保密属性(必选)缺省为0;0:不涉密,1:涉密")
    private Integer commonGbSecrecy;

    /**
     * 设备/区域/系统IP地址(可选)
     */
    @Schema(description = "设备/区域/系统IP地址(可选)")
    private String commonGbIPAddress;

    /**
     * 设备/区域/系统端口(可选)
     */
    @Schema(description = "设备/区域/系统端口(可选)")
    private Integer commonGbPort;

    /**
     * 设备口令(可选)
     */
    @Schema(description = "设备口令(可选)")
    private String commonGbPassword;


    /**
     * 设备状态(必选)
     */
    @Schema(description = "设备状态(必选)")
    private Boolean commonGbStatus;

    /**
     * 国标字段：经度(可选)
     */
    @Schema(description = "经度(可选)")
    private Double commonGbLongitude;

    /**
     * 国标字段：纬度(可选)
     */
    @Schema(description = "纬度(可选)")
    private Double commonGbLatitude;

    /**
     * 国标字段：摄像机类型扩展,标识摄像机类型, 当目录项为摄像机时可选:
     * 1-球机;
     * 2-半球;
     * 3-固定枪机;
     * 4-遥控枪机
     */
    @Schema(description = "摄像机类型扩展,标识摄像机类型,当目录项为摄像机时可选:\n" +
            "      1-球机;\n" +
            "      2-半球;\n" +
            "      3-固定枪机;\n" +
            "      4-遥控枪机")
    private Integer commonGbPtzType;

    /**
     * 国标字段：摄像机位置类型扩展。
     * 1-省际检查站、
     * 2-党政机关、
     * 3-车站码头、
     * 4-中心广场、
     * 5-体育场馆、
     * 6-商业中心、
     * 7-宗教场所、
     * 8-校园周边、
     * 9-治安复杂区域、
     * 10-交通干线
     */
    @Schema(description = "摄像机位置类型扩展" +
            "      1-省际检查站、\n" +
            "      2-党政机关、\n" +
            "      3-车站码头、\n" +
            "      4-中心广场、\n" +
            "      5-体育场馆、\n" +
            "      6-商业中心、\n" +
            "      7-宗教场所、\n" +
            "      8-校园周边、\n" +
            "      9-治安复杂区域、\n" +
            "      10-交通干线")
    private Integer commonGbPositionType;

    /**
     * 国标字段：安装位置室外、室内属性, 当目录项为摄像机时可选。
     * 1-室外、
     * 2-室内
     */
    @Schema(description = "安装位置室外、室内属性, 1-室外、2-室内")
    private Integer commonGbRoomType;

    /**
     * 国标字段：用途, 当目录项为摄像机时可选。
     * 1-治安、
     * 2-交通、
     * 3-重点、
     */
    @Schema(description = "用途, " +
            "      1-治安、\n" +
            "      2-交通、\n" +
            "      3-重点、")
    private Integer commonGbUseType;

    /**
     * 国标字段：补光属性
     * 1-无补光、
     * 2-红外补光、
     * 3-白光补光
     */
    @Schema(description = "补光属性,      " +
            "      1-无补光、\n" +
            "      2-红外补光、\n" +
            "      3-白光补光")
    private Integer commonGbSupplyLightType;

    /**
     * 摄像机监视方位属性。当目录项为摄像机时且为固定摄像机或设置看守位摄像机时可选。
     * 1-东、
     * 2-西、
     * 3-南、
     * 4-北、
     * 5-东南、
     * 6-东北、
     * 7-西南、
     * 8-西北
     *
     */
    @Schema(description = "方位, 当目录项为摄像机时且为固定摄像机或设置看守位摄像机时可选。" +
            "      1-东、\n" +
            "      2-西、\n" +
            "      3-南、\n" +
            "      4-北、\n" +
            "      5-东南、\n" +
            "      6-东北、\n" +
            "      7-西南、\n" +
            "      8-西北")
    private Integer commonGbDirectionType;

    /**
     * 摄像机支持的分辨率,可有多个分辨率值,各个取值间以“/”分隔。
     * 分辨率取值参见附录 F中SDPf字段规定。当目录项为摄像机时可选。
     */
    @Schema(description = "摄像机支持的分辨率,可有多个分辨率值,各个取值间以“/”分隔。" +
            "分辨率取值参见附录 F中SDPf字段规定。当目录项为摄像机时可选。")
    private String commonGbResolution;

    /**
     * 虚拟组织所属的业务分组ID,业务分组根据特定的业务需求制定,一个业
     * 务分组包含一组特定的虚拟组织
     *
     */
    @Schema(description = "虚拟组织所属的业务分组ID,业务分组根据特定的业务需求制定," +
            "一个业务分组包含一组特定的虚拟组织。")
    private String commonGbBusinessGroupID;

    /**
     * 下载倍速范围(可选),各可选参数以“/”分隔,如设备支持1,2,4倍速下
     * 载则应写为“1/2/4”
     */
    @Schema(description = "下载倍速范围(可选),各可选参数以“/”分隔,如设备支持1,2,4倍速下\n" +
            "载则应写为“1/2/4")
    private String commonGbDownloadSpeed;

    /**
     * 空域编码能力,取值0:不支持;1:1级增强;2:2级增强;3:3级增强(可选)
     */
    @Schema(description = "空域编码能力,取值0:不支持;1:1级增强(1个增强层);2:2级增强(2个增强层);3:3级增强(3个增强层)(可选)")
    private Integer commonGbSVCSpaceSupportMode;

    /**
     * 时域编码能力,取值0:不支持;1:1级增强;2:2级增强;3:3级增强(可选)
     */
    @Schema(description = "时域编码能力,取值0:不支持;1:1级增强;2:2级增强;3:3级增强(可选)")
    private Integer commonGbSVCTimeSupportMode;

    /**
     * 类型: 28181, push, proxy
     */
    @Schema(description = "类型: 28181, push, proxy")
    private String type;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private String updateTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private String createTime;


    public int getCommonGbId() {
        return commonGbId;
    }

    public void setCommonGbId(int commonGbId) {
        this.commonGbId = commonGbId;
    }

    public String getCommonGbDeviceID() {
        return commonGbDeviceID;
    }

    public void setCommonGbDeviceID(String commonGbDeviceID) {
        this.commonGbDeviceID = commonGbDeviceID;
    }

    public String getCommonGbName() {
        return commonGbName;
    }

    public void setCommonGbName(String commonGbName) {
        this.commonGbName = commonGbName;
    }

    public String getCommonGbManufacturer() {
        return commonGbManufacturer;
    }

    public void setCommonGbManufacturer(String commonGbManufacturer) {
        this.commonGbManufacturer = commonGbManufacturer;
    }

    public String getCommonGbModel() {
        return commonGbModel;
    }

    public void setCommonGbModel(String commonGbModel) {
        this.commonGbModel = commonGbModel;
    }

    public String getCommonGbOwner() {
        return commonGbOwner;
    }

    public void setCommonGbOwner(String commonGbOwner) {
        this.commonGbOwner = commonGbOwner;
    }

    public String getCommonGbCivilCode() {
        return commonGbCivilCode;
    }

    public void setCommonGbCivilCode(String commonGbCivilCode) {
        this.commonGbCivilCode = commonGbCivilCode;
    }

    public String getCommonGbBlock() {
        return commonGbBlock;
    }

    public void setCommonGbBlock(String commonGbBlock) {
        this.commonGbBlock = commonGbBlock;
    }

    public String getCommonGbAddress() {
        return commonGbAddress;
    }

    public void setCommonGbAddress(String commonGbAddress) {
        this.commonGbAddress = commonGbAddress;
    }

    public Integer getCommonGbParental() {
        return commonGbParental;
    }

    public void setCommonGbParental(Integer commonGbParental) {
        this.commonGbParental = commonGbParental;
    }

    public String getCommonGbParentID() {
        return commonGbParentID;
    }

    public void setCommonGbParentID(String commonGbParentID) {
        this.commonGbParentID = commonGbParentID;
    }

    public Integer getCommonGbSafetyWay() {
        return commonGbSafetyWay;
    }

    public void setCommonGbSafetyWay(Integer commonGbSafetyWay) {
        this.commonGbSafetyWay = commonGbSafetyWay;
    }

    public Integer getCommonGbRegisterWay() {
        return commonGbRegisterWay;
    }

    public void setCommonGbRegisterWay(Integer commonGbRegisterWay) {
        this.commonGbRegisterWay = commonGbRegisterWay;
    }

    public String getCommonGbCertNum() {
        return commonGbCertNum;
    }

    public void setCommonGbCertNum(String commonGbCertNum) {
        this.commonGbCertNum = commonGbCertNum;
    }

    public Integer getCommonGbCertifiable() {
        return commonGbCertifiable;
    }

    public void setCommonGbCertifiable(Integer commonGbCertifiable) {
        this.commonGbCertifiable = commonGbCertifiable;
    }

    public Integer getCommonGbErrCode() {
        return commonGbErrCode;
    }

    public void setCommonGbErrCode(Integer commonGbErrCode) {
        this.commonGbErrCode = commonGbErrCode;
    }

    public String getCommonGbEndTime() {
        return commonGbEndTime;
    }

    public void setCommonGbEndTime(String commonGbEndTime) {
        this.commonGbEndTime = commonGbEndTime;
    }

    public Integer getCommonGbSecrecy() {
        return commonGbSecrecy;
    }

    public void setCommonGbSecrecy(Integer commonGbSecrecy) {
        this.commonGbSecrecy = commonGbSecrecy;
    }

    public String getCommonGbIPAddress() {
        return commonGbIPAddress;
    }

    public void setCommonGbIPAddress(String commonGbIPAddress) {
        this.commonGbIPAddress = commonGbIPAddress;
    }

    public Integer getCommonGbPort() {
        return commonGbPort;
    }

    public void setCommonGbPort(Integer commonGbPort) {
        this.commonGbPort = commonGbPort;
    }

    public String getCommonGbPassword() {
        return commonGbPassword;
    }

    public void setCommonGbPassword(String commonGbPassword) {
        this.commonGbPassword = commonGbPassword;
    }

    public Boolean getCommonGbStatus() {
        return commonGbStatus;
    }

    public void setCommonGbStatus(Boolean commonGbStatus) {
        this.commonGbStatus = commonGbStatus;
    }

    public Double getCommonGbLongitude() {
        return commonGbLongitude;
    }

    public void setCommonGbLongitude(Double commonGbLongitude) {
        this.commonGbLongitude = commonGbLongitude;
    }

    public Double getCommonGbLatitude() {
        return commonGbLatitude;
    }

    public void setCommonGbLatitude(Double commonGbLatitude) {
        this.commonGbLatitude = commonGbLatitude;
    }

    public Integer getCommonGbPtzType() {
        return commonGbPtzType;
    }

    public void setCommonGbPtzType(Integer commonGbPtzType) {
        this.commonGbPtzType = commonGbPtzType;
    }

    public Integer getCommonGbPositionType() {
        return commonGbPositionType;
    }

    public void setCommonGbPositionType(Integer commonGbPositionType) {
        this.commonGbPositionType = commonGbPositionType;
    }

    public Integer getCommonGbRoomType() {
        return commonGbRoomType;
    }

    public void setCommonGbRoomType(Integer commonGbRoomType) {
        this.commonGbRoomType = commonGbRoomType;
    }

    public Integer getCommonGbUseType() {
        return commonGbUseType;
    }

    public void setCommonGbUseType(Integer commonGbUseType) {
        this.commonGbUseType = commonGbUseType;
    }

    public Integer getCommonGbSupplyLightType() {
        return commonGbSupplyLightType;
    }

    public void setCommonGbSupplyLightType(Integer commonGbSupplyLightType) {
        this.commonGbSupplyLightType = commonGbSupplyLightType;
    }

    public Integer getCommonGbDirectionType() {
        return commonGbDirectionType;
    }

    public void setCommonGbDirectionType(Integer commonGbDirectionType) {
        this.commonGbDirectionType = commonGbDirectionType;
    }

    public String getCommonGbResolution() {
        return commonGbResolution;
    }

    public void setCommonGbResolution(String commonGbResolution) {
        this.commonGbResolution = commonGbResolution;
    }

    public String getCommonGbBusinessGroupID() {
        return commonGbBusinessGroupID;
    }

    public void setCommonGbBusinessGroupID(String commonGbBusinessGroupID) {
        this.commonGbBusinessGroupID = commonGbBusinessGroupID;
    }

    public String getCommonGbDownloadSpeed() {
        return commonGbDownloadSpeed;
    }

    public void setCommonGbDownloadSpeed(String commonGbDownloadSpeed) {
        this.commonGbDownloadSpeed = commonGbDownloadSpeed;
    }

    public Integer getCommonGbSVCTimeSupportMode() {
        return commonGbSVCTimeSupportMode;
    }

    public void setCommonGbSVCTimeSupportMode(Integer commonGbSVCTimeSupportMode) {
        this.commonGbSVCTimeSupportMode = commonGbSVCTimeSupportMode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public static CommonGbChannel getInstance(StreamProxy streamProxy) {
        CommonGbChannel commonGbChannel = new CommonGbChannel();
        commonGbChannel.setType(CommonGbChannelType.PROXY);
        commonGbChannel.setCommonGbDeviceID(streamProxy.getGbId());
        commonGbChannel.setCommonGbName(streamProxy.getName());
        commonGbChannel.setCommonGbLongitude(streamProxy.getLongitude());
        commonGbChannel.setCommonGbLatitude(streamProxy.getLatitude());
        commonGbChannel.setCommonGbStatus(true);
        commonGbChannel.setCreateTime(DateUtil.getNow());
        commonGbChannel.setUpdateTime(DateUtil.getNow());
        return commonGbChannel;
    }

    public static CommonGbChannel getInstance(List<String> syncKeys, DeviceChannel deviceChannel){
        CommonGbChannel commonGbChannel = new CommonGbChannel();
        commonGbChannel.setCommonGbDeviceID(deviceChannel.getChannelId());
        commonGbChannel.setCommonGbStatus(deviceChannel.isStatus());
        commonGbChannel.setType(CommonGbChannelType.GB28181);
        commonGbChannel.setCreateTime(DateUtil.getNow());
        commonGbChannel.setUpdateTime(DateUtil.getNow());
        if (syncKeys == null || syncKeys.contains("commonGbName")) {
            commonGbChannel.setCommonGbName(deviceChannel.getName());
        }
        if (syncKeys == null || syncKeys.contains("commonGbManufacturer")) {
            commonGbChannel.setCommonGbManufacturer(deviceChannel.getManufacture());
        }
        if (syncKeys == null || syncKeys.contains("commonGbName")) {
            commonGbChannel.setCommonGbModel(deviceChannel.getModel());
        }
        if (syncKeys == null || syncKeys.contains("commonGbOwner")) {
            commonGbChannel.setCommonGbOwner(deviceChannel.getOwner());
        }
        if (syncKeys == null || syncKeys.contains("commonGbCivilCode")) {
            if (deviceChannel.getCivilCode() != null) {
                Gb28181CodeType channelIdType = SipUtils.getChannelIdType(deviceChannel.getCivilCode());
                if (channelIdType == Gb28181CodeType.CIVIL_CODE_PROVINCE
                        || channelIdType == Gb28181CodeType.CIVIL_CODE_CITY
                        || channelIdType == Gb28181CodeType.CIVIL_CODE_COUNTY
                        || channelIdType == Gb28181CodeType.CIVIL_CODE_GRASS_ROOTS
                ) {
                    commonGbChannel.setCommonGbCivilCode(deviceChannel.getCivilCode());
                } else {
                    logger.warn("[不规范的CivilCode]，deviceId: {}, channel: {}, civilCode: {}",
                            deviceChannel.getDeviceId(),
                            deviceChannel.getChannelId(),
                            deviceChannel.getCivilCode());
                }
                commonGbChannel.setCommonGbCivilCode(deviceChannel.getCivilCode());
            }
        }
        if (syncKeys == null || syncKeys.contains("commonGbBlock")) {
            commonGbChannel.setCommonGbBlock(deviceChannel.getBlock());
        }
        if (syncKeys == null || syncKeys.contains("commonGbAddress")) {
            commonGbChannel.setCommonGbAddress(deviceChannel.getAddress());
        }
        if (syncKeys == null || syncKeys.contains("commonGbParental")) {
            commonGbChannel.setCommonGbParental(deviceChannel.getParental());
        }
        if (syncKeys == null || syncKeys.contains("commonGbParentID")) {
            commonGbChannel.setCommonGbParentID(deviceChannel.getParentId());
        }
        if (syncKeys == null || syncKeys.contains("commonGbSafetyWay")) {
            commonGbChannel.setCommonGbSafetyWay(deviceChannel.getSafetyWay());
        }
        if (syncKeys == null || syncKeys.contains("commonGbRegisterWay")) {
            commonGbChannel.setCommonGbRegisterWay(deviceChannel.getRegisterWay());
        }
        if (syncKeys == null || syncKeys.contains("commonGbCertNum")) {
            commonGbChannel.setCommonGbCertNum(deviceChannel.getCertNum());
        }
        if (syncKeys == null || syncKeys.contains("commonGbCertifiable")) {

        commonGbChannel.setCommonGbCertifiable(deviceChannel.getCertifiable());
        }
        if (syncKeys == null || syncKeys.contains("commonGbErrCode")) {
        commonGbChannel.setCommonGbErrCode(deviceChannel.getErrCode());
    }
        if (syncKeys == null || syncKeys.contains("commonGbEndTime")) {
            commonGbChannel.setCommonGbEndTime(deviceChannel.getEndTime());
        }

        if (syncKeys == null || syncKeys.contains("commonGbSecrecy")) {
            if (NumberUtils.isParsable(deviceChannel.getSecrecy())) {
                commonGbChannel.setCommonGbSecrecy(Integer.parseInt(deviceChannel.getSecrecy()));
            }
        }

        if (syncKeys == null || syncKeys.contains("commonGbIPAddress")) {
            commonGbChannel.setCommonGbIPAddress(deviceChannel.getIpAddress());
        }

        if (syncKeys == null || syncKeys.contains("commonGbPort")) {
            commonGbChannel.setCommonGbPort(deviceChannel.getPort());
        }

        if (syncKeys == null || syncKeys.contains("commonGbPassword")) {
            commonGbChannel.setCommonGbPassword(deviceChannel.getPassword());
        }

        if (syncKeys == null || syncKeys.contains("commonGbLongitude")) {
            commonGbChannel.setCommonGbLongitude(deviceChannel.getLongitude());
        }

        if (syncKeys == null || syncKeys.contains("commonGbLatitude")) {
            commonGbChannel.setCommonGbLatitude(deviceChannel.getLatitude());
        }

        if (syncKeys == null || syncKeys.contains("commonGbPtzType")) {
            commonGbChannel.setCommonGbPtzType(deviceChannel.getPTZType());
        }

//        if (syncKeys == null || syncKeys.contains("commonGbPositionType")) {
////                        commonGbChannel.setCommonGbPositionType(deviceChannel.getCommonGbPositionType());
//        }
//
//        if (syncKeys == null || syncKeys.contains("commonGbRoomType")) {
//
//        }
//        if (syncKeys == null || syncKeys.contains("commonGbUseType")) {
//
//        }
//        if (syncKeys == null || syncKeys.contains("commonGbSupplyLightType")) {
//
//        }
//        if (syncKeys == null || syncKeys.contains("commonGbDirectionType")) {
//
//        }
//        if (syncKeys == null || syncKeys.contains("commonGbResolution")) {
//
//        }
//        if (syncKeys == null || syncKeys.contains("commonGbBusinessGroupID")) {
//            commonGbChannel.setCommonGbBusinessGroupID(deviceChannel.getBusinessGroupId());
//        }
//
//        if (syncKeys == null || syncKeys.contains("commonGbDownloadSpeed")) {
//
//        }
//        if (syncKeys == null || syncKeys.contains("commonGbSVCTimeSupportMode")) {
//
//        }
        return commonGbChannel;
    }

    public static CommonGbChannel getInstance(StreamPush streamPush){
        CommonGbChannel commonGbChannel = new CommonGbChannel();
        commonGbChannel.setCommonGbDeviceID(streamPush.getGbId());
        commonGbChannel.setType(CommonGbChannelType.PUSH);
        if (!ObjectUtils.isEmpty(streamPush.getName().trim())) {
            commonGbChannel.setCommonGbName(streamPush.getName().trim());
        }
        if (streamPush.getLongitude() > 0) {
            commonGbChannel.setCommonGbLongitude(streamPush.getLongitude());
        }
        if (streamPush.getLatitude() > 0) {
            commonGbChannel.setCommonGbLatitude(streamPush.getLatitude());
        }
        if (!ObjectUtils.isEmpty(streamPush.getGroupDeviceId())) {
            commonGbChannel.setCommonGbBusinessGroupID(streamPush.getGroupDeviceId());
        }
        commonGbChannel.setUpdateTime(DateUtil.getNow());
        commonGbChannel.setCreateTime(DateUtil.getNow());

        return commonGbChannel;
    }

    public Integer getCommonGbSVCSpaceSupportMode() {
        return commonGbSVCSpaceSupportMode;
    }

    public void setCommonGbSVCSpaceSupportMode(Integer commonGbSVCSpaceSupportMode) {
        this.commonGbSVCSpaceSupportMode = commonGbSVCSpaceSupportMode;
    }

    public static CommonGbChannel getInstance(Group group) {
        CommonGbChannel commonGbChannel = new CommonGbChannel();
        commonGbChannel.setCommonGbDeviceID(group.getCommonGroupDeviceId());
        commonGbChannel.setCommonGbName(group.getCommonGroupName());
        commonGbChannel.setCommonGbParental(1);
        commonGbChannel.setCommonGbParentID(group.getCommonGroupParentId());
        commonGbChannel.setCommonGbBusinessGroupID(group.getCommonGroupTopId());
        return commonGbChannel;
    }
}
