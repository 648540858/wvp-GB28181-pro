package com.genersoft.iot.vmp.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置文件 user-settings 映射的配置信息
 */
@Component
@ConfigurationProperties(prefix = "user-settings", ignoreInvalidFields = true)
@Order(0)
@Data
public class UserSetting {

    /**
     * 是否保存位置的历史记录(轨迹)
     */
    private Boolean savePositionHistory = Boolean.FALSE;

    /**
     * 是否开始自动点播: 请求流为未拉起的流时,自动开启点播, 需要rtp.enable=true
     */
    private Boolean autoApplyPlay = Boolean.FALSE;

    /**
     * [可选] 部分设备需要扩展SDP，需要打开此设置,一般设备无需打开
     */
    private Boolean seniorSdp = Boolean.FALSE;

    /**
     * 点播/录像回放 等待超时时间,单位：毫秒
     */
    private Integer playTimeout = 10000;

    /**
     * 获取设备录像数据超时时间,单位：毫秒
     */
    private Integer recordInfoTimeout = 15000;

    /**
     * 上级点播等待超时时间,单位：毫秒
     */
    private int platformPlayTimeout = 20000;

    /**
     * 是否开启接口鉴权
     */
    private Boolean interfaceAuthentication = Boolean.TRUE;

    /**
     * 接口鉴权例外的接口, 即不进行接口鉴权的接口,尽量详细书写，尽量不用/**，至少两级目录
     */
    private List<String> interfaceAuthenticationExcludes = new ArrayList<>();

    /**
     * 推流直播是否录制
     */
    private Boolean recordPushLive = Boolean.TRUE;

    /**
     * 国标是否录制
     */
    private Boolean recordSip = Boolean.TRUE;

    /**
     * 使用推流状态作为推流通道状态
     */
    private Boolean usePushingAsStatus = Boolean.FALSE;

    /**
     * 使用来源请求ip作为streamIp,当且仅当你只有zlm节点它与wvp在一起的情况下开启
     */
    private Boolean useSourceIpAsStreamIp = Boolean.FALSE;

    /**
     * 是否使用设备来源Ip作为回复IP， 不设置则为 false
     */
    private Boolean sipUseSourceIpAsRemoteAddress = Boolean.FALSE;

    /**
     * 国标点播 按需拉流, true：有人观看拉流，无人观看释放， false：拉起后不自动释放
     */
    private Boolean streamOnDemand = Boolean.TRUE;

    /**
     * 推流鉴权， 默认开启
     */
    private Boolean pushAuthority = Boolean.TRUE;

    /**
     * 设备上线时是否自动同步通道
     */
    private Boolean syncChannelOnDeviceOnline = Boolean.FALSE;

    /**
     * 是否开启sip日志
     */
    private Boolean sipLog = Boolean.FALSE;

    /**
     * 是否开启mybatis-sql日志
     */
    private Boolean sqlLog = Boolean.FALSE;

    /**
     * 消息通道功能-缺少国标ID是否给所有上级发送消息
     */
    private Boolean sendToPlatformsWhenIdLost = Boolean.FALSE;

    /**
     * 保持通道状态，不接受notify通道状态变化， 兼容海康平台发送错误消息
     */
    private Boolean refuseChannelStatusChannelFormNotify = Boolean.FALSE;

    /**
     * 设备/通道状态变化时发送消息
     */
    private Boolean deviceStatusNotify = Boolean.TRUE;

    /**
     * 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
     */
    private Boolean useCustomSsrcForParentInvite = Boolean.TRUE;

    /**
     * 开启接口文档页面。 默认开启，生产环境建议关闭，遇到swagger相关的漏洞时也可以关闭
     */
    private Boolean docEnable = Boolean.TRUE;

    /**
     * 服务ID，不写则为000000
     */
    private String serverId = "000000";


    /**
     * 国标级联语音喊话发流模式 * UDP:udp传输 TCP-ACTIVE：tcp主动模式 TCP-PASSIVE：tcp被动模式
     */
    private String broadcastForPlatform = "UDP";

    /**
     * 行政区划信息文件,系统启动时会加载到系统里
     */
    private String civilCodeFile = "classpath:civilCode.csv";

    /**
     * 跨域配置，不配置此项则允许所有跨域请求，配置后则只允许配置的页面的地址请求， 可以配置多个
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * 设置notify缓存队列最大长度，超过此长度的数据将返回486 BUSY_HERE，消息丢弃, 默认100000
     */
    private int maxNotifyCountQueue = 100000;

    /**
     * 国标级联离线后多久重试一次注册
     */
    private int registerAgainAfterTime = 60;

    /**
     * 国标续订方式，true为续订，每次注册在同一个会话里，false为重新注册，每次使用新的会话
     */
    private boolean registerKeepIntDialog = false;

    /**
     *     # 国标设备离线后的上线策略，
     *     # 0： 国标标准实现，设备离线后不回复心跳，直到设备重新注册上线，
     *     # 1（默认）： 对于离线设备，收到心跳就把设备设置为上线，并更新注册时间为上次这次心跳的时间。防止过期时间判断异常
     */
    private int gbDeviceOnline = 1;

    /**
     *    登录超时时间(分钟)，
     */
    private long loginTimeout = 60;

    /**
     * jwk文件路径，若不指定则使用resources目录下的jwk.json
     */
    private String jwkFile = "classpath:jwk.json";

    /**
     * wvp集群模式下如果注册向上级的wvp奔溃，则自动选择一个其他wvp继续注册到上级
     */
    private boolean autoRegisterPlatform = false;

    /**
     * 按需发送推流设备位置， 默认发送移动位置订阅时如果位置不变则不发送， 设置为false按照国标间隔持续发送
     */
    private boolean sendPositionOnDemand = true;

    /**
     * 部分设备会在短时间内发送大量注册， 导致协议栈内存溢出， 开启此项可以防止这部分设备注册， 避免服务崩溃，但是会降低系统性能， 描述如下
     * 默认值为 true。
     * 将此设置为 false 会使 Stack 在 Server Transaction 进入 TERMINATED 状态后关闭服务器套接字。
     * 这允许服务器防止客户端发起的基于 TCP 的拒绝服务攻击（即发起数百个客户端事务）。
     * 如果为 true（默认作），则堆栈将保持套接字打开，以便以牺牲线程和内存资源为代价来最大化性能 - 使自身容易受到 DOS 攻击。
     */
    private boolean sipCacheServerConnections = true;



}
