package com.genersoft.iot.vmp.common;

import io.swagger.v3.oas.annotations.media.Schema;

public class CommonGbChannel {

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
    private String commonIPAddress;

    /**
     * 设备/区域/系统端口(可选)
     */
    @Schema(description = "设备/区域/系统端口(可选)")
    private String commonPort;

    /**
     * 设备口令(可选)
     */
    @Schema(description = "设备口令(可选)")
    private String commonPassword;


    /**
     * 设备状态(必选)
     */
    @Schema(description = "设备状态(必选)")
    private String commonStatus;

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
    private Integer commonGbResolution;

    /**
     * 虚拟组织所属的业务分组ID,业务分组根据特定的业务需求制定,一个业
     * 务分组包含一组特定的虚拟组织
     *
     */
    @Schema(description = "虚拟组织所属的业务分组ID,业务分组根据特定的业务需求制定," +
            "一个业务分组包含一组特定的虚拟组织。")
    private Integer commonGbBusinessGroupID;

    /**
     * 下载倍速范围(可选),各可选参数以“/”分隔,如设备支持1,2,4倍速下
     * 载则应写为“1/2/4”
     */
    @Schema(description = "下载倍速范围(可选),各可选参数以“/”分隔,如设备支持1,2,4倍速下\n" +
            "载则应写为“1/2/4")
    private Integer commonGbDownloadSpeed;

    /**
     * 下载倍速范围(可选),各可选参数以“/”分隔,如设备支持1,2,4倍速下
     * 载则应写为“1/2/4”
     */
    @Schema(description = "时域编码能力,取值0:不支持;1:1级增强;2:2级增强;3:3级增强(可\n" +
            "选)")
    private Integer commonGbSVCTimeSupportMode;


}
