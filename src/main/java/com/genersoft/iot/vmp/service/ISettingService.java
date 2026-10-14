package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.vmanager.bean.JtServerConfig;
import com.genersoft.iot.vmp.vmanager.bean.ProfileInfo;
import com.genersoft.iot.vmp.vmanager.bean.SipServerConfig;
import com.genersoft.iot.vmp.vmanager.bean.UserSettingConfig;
import com.genersoft.iot.vmp.vmanager.bean.UserSettingSaveResult;
import com.genersoft.iot.vmp.vmanager.bean.WebServerConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统设置相关业务
 */
public interface ISettingService {

    /**
     * 查询配置文件列表
     * <p>
     * 1. 确定配置文件所在目录: 启动时通过 --spring.config.location 指定则使用该路径, 否则使用默认配置目录<br/>
     * 2. 扫描目录下 application-*.yml, 取中间部分作为配置文件ID<br/>
     * 3. 当前生效的配置文件被标记为 active
     */
    List<ProfileInfo> queryProfiles();

    /**
     * 切换配置文件: 将当前生效配置文件中的 spring.profiles.active 修改为指定的配置文件ID
     * <p>
     * 修改后需要重启服务才能生效
     *
     * @param profileId 目标配置文件ID
     * @return 被修改的配置文件路径
     */
    String switchProfile(String profileId);

    /**
     * 读取指定配置文件的内容
     *
     * @param profileId 配置文件ID
     * @return 配置文件文本内容
     */
    String readProfileContent(String profileId);

    /**
     * 查询 WEB服务(server)配置
     * <p>
     * 返回当前生效配置文件中的 server 配置项(可编辑), 以及运行中实际生效的值(只读)
     */
    WebServerConfig queryWebServerConfig();

    /**
     * 保存 WEB服务(server)配置到当前生效的配置文件
     * <p>
     * 修改后需要重启服务才能生效
     *
     * @param config WEB服务配置
     * @return 被修改的配置文件路径
     */
    String saveWebServerConfig(WebServerConfig config);

    /**
     * 查询国标(GB28181) SIP 服务配置
     * <p>
     * 返回当前生效配置文件中的 sip 配置项(可编辑), 以及运行中实际生效的值(只读)
     */
    SipServerConfig querySipServerConfig();

    /**
     * 保存国标(GB28181) SIP 服务配置到当前生效的配置文件
     * <p>
     * 修改后需要重启服务才能生效
     *
     * @param config 国标SIP服务配置
     * @return 被修改的配置文件路径
     */
    String saveSipServerConfig(SipServerConfig config);

    /**
     * 查询部标(JT/T 1078)服务配置
     * <p>
     * 返回当前生效配置文件中的 ftp / jt1078 配置项(可编辑), 以及运行中实际生效的值(只读)
     */
    JtServerConfig queryJtServerConfig();

    /**
     * 保存部标(JT/T 1078)服务配置到当前生效的配置文件
     * <p>
     * 修改后需要重启服务才能生效
     *
     * @param config 部标服务配置
     * @return 被修改的配置文件路径
     */
    String saveJtServerConfig(JtServerConfig config);

    /**
     * 将默认媒体节点配置回写到当前生效的配置文件(media.*)
     * <p>
     * 供媒体节点编辑保存时调用, 仅默认节点需要写配置文件; 配置文件不可写时抛异常
     *
     * @param mediaServer 默认媒体节点
     */
    void saveDefaultMediaConfig(MediaServer mediaServer);

    /**
     * 当前生效的配置文件是否可写
     */
    boolean isActiveConfigWritable();

    /**
     * 查询全局策略-平台策略(user-settings)配置
     * <p>
     * 返回当前生效配置文件中的 user-settings 配置项(可编辑)
     */
    UserSettingConfig queryUserSettingConfig();

    /**
     * 保存全局策略-平台策略(user-settings)配置到当前生效的配置文件
     * <p>
     * 无需重启的字段会被就地热应用立即生效; 需要重启的字段仍以配置为准, 重启后生效。
     *
     * @param config 平台策略配置
     * @return 保存结果, 含被修改的文件路径以及本次变更且需要重启的字段
     */
    UserSettingSaveResult saveUserSettingConfig(UserSettingConfig config);

    /**
     * 查询可用的报警订阅类型列表
     *
     * @return 报警类型列表, 每项包含 name(枚举名) 与 description(描述)
     */
    List<Map<String, String>> queryAlarmTypes();
}
