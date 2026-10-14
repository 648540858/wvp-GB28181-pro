package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 全局策略-平台策略(user-settings)配置
 * <p>
 * 字段与 {@code user-settings} 下的配置项一一对应, 均为当前生效配置文件中的值(可编辑)
 */
@Setter
@Getter
@Schema(description = "平台策略(user-settings)配置")
public class UserSettingConfig {

    // ---------------- 基础配置 ----------------

    @Schema(description = "是否开启接口鉴权")
    private Boolean interfaceAuthentication;

    @Schema(description = "接口鉴权例外的接口")
    private List<String> interfaceAuthenticationExcludes;

    @Schema(description = "使用来源请求IP作为streamIp")
    private Boolean useSourceIpAsStreamIp;

    @Schema(description = "是否开启sql日志")
    private Boolean sqlLog;

    @Schema(description = "设备/通道状态变化时是否发送消息")
    private Boolean deviceStatusNotify;

    @Schema(description = "是否开启接口文档页面")
    private Boolean docEnable;

    @Schema(description = "服务ID, 不写则为000000")
    private String serverId;

    @Schema(description = "行政区划信息文件")
    private String civilCodeFile;

    @Schema(description = "跨域白名单")
    private List<String> allowedOrigins;

    @Schema(description = "notify缓存队列最大长度")
    private Integer maxNotifyCountQueue;

    @Schema(description = "登录超时时间(分钟)")
    private Long loginTimeout;

    @Schema(description = "jwk文件路径")
    private String jwkFile;

    @Schema(description = "是否保存位置历史轨迹")
    private Boolean savePositionHistory;

    // ---------------- 视频播放 ----------------

    @Schema(description = "是否自动点播")
    private Boolean autoApplyPlay;

    @Schema(description = "点播/录像回放等待超时时间(毫秒)")
    private Integer playTimeout;

    @Schema(description = "获取设备录像数据超时时间(毫秒)")
    private Integer recordInfoTimeout;

    @Schema(description = "国标点播是否按需拉流")
    private Boolean streamOnDemand;

    @Schema(description = "截图是否使用ZLM内部解码通道")
    private Boolean snapAsync;

    // ---------------- 推流控制 ----------------

    @Schema(description = "推流鉴权")
    private Boolean pushAuthority;

    @Schema(description = "推流直播是否录制")
    private Boolean recordPushLive;

    @Schema(description = "使用推流状态作为推流通道状态")
    private Boolean usePushingAsStatus;

    @Schema(description = "分组同步时是否使用别名")
    private Boolean useAliasForGroupSync;

    // ---------------- 国标服务 ----------------

    @Schema(description = "设备ID严格模式")
    private Boolean deviceIdStrict;

    @Schema(description = "是否扩展SDP")
    private Boolean seniorSdp;

    @Schema(description = "是否开启sip日志")
    private Boolean sipLog;

    @Schema(description = "是否使用设备来源IP作为回复IP")
    private Boolean sipUseSourceIpAsRemoteAddress;

    @Schema(description = "国标是否录制")
    private Boolean recordSip;

    @Schema(description = "设备上线时是否自动同步通道")
    private Boolean syncChannelOnDeviceOnline;

    @Schema(description = "缺少国标ID时是否给所有上级发送消息")
    private Boolean sendToPlatformsWhenIdLost;

    @Schema(description = "是否不接受notify通道状态变化")
    private Boolean refuseChannelStatusChannelFormNotify;

    @Schema(description = "多端口模式是否使用随机SSRC")
    private Boolean ssrcRandom;

    @Schema(description = "国标设备离线后的上线策略: 0-标准实现, 1-收到心跳即上线")
    private Integer gbDeviceOnline;

    @Schema(description = "协议栈是否保持服务器套接字打开")
    private Boolean sipCacheServerConnections;

    @Schema(description = "是否禁用date头(变相禁用校时)")
    private Boolean disableDateHeader;

    @Schema(description = "同步业务分组时自动生成分组国标编号的模板")
    private String groupSyncDeviceTemplate;

    @Schema(description = "是否默认开启位置订阅")
    private Boolean subscribeMobilePosition;

    @Schema(description = "设备注册时间列表最大保留条数")
    private Integer deviceRegisterTimeMaxCount;

    @Schema(description = "设备注册时间列表过期时间(小时), 0表示永不过期")
    private Integer deviceRegisterTimeTtlHours;

    @Schema(description = "设备心跳时间列表最大保留条数")
    private Integer deviceKeepaliveTimeMaxCount;

    @Schema(description = "设备心跳时间列表过期时间(小时), 0表示永不过期")
    private Integer deviceKeepaliveTimeTtlHours;

    // ---------------- 国标级联 ----------------

    @Schema(description = "上级点播等待超时时间(毫秒)")
    private Integer platformPlayTimeout;

    @Schema(description = "上级平台点播是否使用自定义SSRC")
    private Boolean useCustomSsrcForParentInvite;

    @Schema(description = "国标级联语音喊话发流模式: UDP / TCP-ACTIVE / TCP-PASSIVE")
    private String broadcastForPlatform;

    @Schema(description = "国标级联离线后多久重试一次注册(秒)")
    private Integer registerAgainAfterTime;

    @Schema(description = "国标续订方式: true-续订, false-重新注册")
    private Boolean registerKeepIntDialog;

    @Schema(description = "集群模式下是否自动注册到上级")
    private Boolean autoRegisterPlatform;

    @Schema(description = "按需发送位置")
    private Boolean sendPositionOnDemand;

    // ---------------- 报警控制 ----------------

    @Schema(description = "处理报警消息时的通道缓存大小")
    private Long alarmCatchSize;

    @Schema(description = "是否使用拉流的方式获取快照")
    private Boolean alarmSnapByStream;

    @Schema(description = "报警订阅白名单(AlarmType枚举名)")
    private List<String> allowedAlarmType;

    @Schema(description = "报警记录保留天数, 0表示不自动清理")
    private Integer alarmKeepDays;

    // ---------------- 元信息 ----------------

    @Schema(description = "需要重启服务才能生效的字段(仅查询时下发)")
    private List<String> restartRequiredFields;

    @Schema(description = "当前生效的配置文件ID")
    private String profileId;

    @Schema(description = "目标配置文件路径")
    private String filePath;

    @Schema(description = "目标配置文件是否可写")
    private boolean writable;
}
