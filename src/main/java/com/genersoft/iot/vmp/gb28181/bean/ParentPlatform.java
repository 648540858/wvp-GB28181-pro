package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author lin
 */
@Data
@Schema(description = "平台信息")
public class ParentPlatform {

    @Schema(description = "ID(数据库中)")
    private Integer id;

    @Schema(description = "是否启用")
    private boolean enable;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "SIP服务国标编码")
    private String serverGBId;

    @Schema(description = "SIP服务国标域")
    private String serverGBDomain;

    @Schema(description = "SIP服务IP")
    private String serverIP;

    @Schema(description = "SIP服务端口")
    private int serverPort;

    @Schema(description = "设备国标编号")
    private String deviceGBId;

    @Schema(description = "设备ip")
    private String deviceIp;

    @Schema(description = "设备端口")
    private int devicePort;

    @Schema(description = "SIP认证用户名(默认使用设备国标编号)")
    private String username;

    @Schema(description = "SIP认证密码")
    private String password;

    @Schema(description = "注册周期 (秒)")
    private int expires;

    @Schema(description = "心跳周期(秒)")
    private int keepTimeout;

    @Schema(description = "传输协议")
    private String transport;

    @Schema(description = "字符集")
    private String characterSet;

    @Schema(description = "允许云台控制")
    private boolean ptz;

    @Schema(description = "RTCP流保活")
    private boolean rtcp;

    @Schema(description = "在线状态")
    private boolean status;

    @Schema(description = "在线状态")
    private int channelCount;

    @Schema(description = "默认目录Id,自动添加的通道多放在这个目录下")
    private String catalogId;

    @Schema(description = "已被订阅目录信息")
    private boolean catalogSubscribe;

    @Schema(description = "已被订阅报警信息")
    private boolean alarmSubscribe;

    @Schema(description = "已被订阅移动位置信息")
    private boolean mobilePositionSubscribe;

    @Schema(description = "点播未推流的设备时是否使用redis通知拉起")
    private boolean startOfflinePush;

    @Schema(description = "目录分组-每次向上级发送通道信息时单个包携带的通道数量，取值1,2,4,8")
    private int catalogGroup;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "是否作为消息通道")
    private boolean asMessageChannel;

    @Schema(description = "是否作为消息通道")
    private boolean autoPushChannel;

    @Schema(description = "点播回复200OK使用次IP")
    private String sendStreamIp;

    @Schema(description = "是否使用自定义业务分组")
    private Boolean customCatalog;
}
