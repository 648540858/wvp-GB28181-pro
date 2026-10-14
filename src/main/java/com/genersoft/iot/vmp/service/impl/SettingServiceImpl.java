package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.service.ISettingService;
import com.genersoft.iot.vmp.service.bean.AlarmType;
import com.genersoft.iot.vmp.service.bean.ConfigEntry;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.vmanager.bean.JtServerConfig;
import com.genersoft.iot.vmp.vmanager.bean.ProfileInfo;
import com.genersoft.iot.vmp.vmanager.bean.SipServerConfig;
import com.genersoft.iot.vmp.vmanager.bean.UserSettingConfig;
import com.genersoft.iot.vmp.vmanager.bean.WebServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 系统设置业务实现
 * <p>
 * 配置文件来源的判定规则:
 * <ol>
 *     <li>启动时通过 --spring.config.location(或 spring.config.additional-location) 指定了路径, 则使用该路径;</li>
 *     <li>未指定时, 使用 classpath(打包在jar内) 中的 application.yml;</li>
 * </ol>
 * 是否支持切换由 <b>主配置文件 application.yml</b> 中是否存在 spring.profiles 决定。
 */
@Service
@Slf4j
public class SettingServiceImpl implements ISettingService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 可切换的配置文件命名规则 application-{id}.yml
     */
    private static final Pattern PROFILE_FILE_PATTERN =
            Pattern.compile("^application-(.+)\\.(yml|yaml)$", Pattern.CASE_INSENSITIVE);

    /**
     * 主配置文件名称, 其中保存 spring.profiles.active
     */
    private static final String[] MAIN_CONFIG_NAMES = {"application.yml", "application.yaml"};

    private static final String LOCATION_PROPERTY = "spring.config.location";
    private static final String ADDITIONAL_LOCATION_PROPERTY = "spring.config.additional-location";

    /**
     * 从 PropertySource 名称中提取配置文件路径, 如 file [/opt/wvp/application.yml]
     */
    private static final Pattern FILE_ORIGIN_PATTERN = Pattern.compile("file \\[([^\\]]+)\\]");

    /**
     * 在线查看配置文件的内容大小上限(字节)
     */
    private static final int MAX_CONTENT_LENGTH = 2 * 1024 * 1024;

    /**
     * 国标服务命令超时时间的默认值(毫秒), 与 {@link SipConfig#timeout} 默认值保持一致
     */
    private static final long DEFAULT_SIP_TIMEOUT = 1000L;

    /**
     * 国标服务是否存储alarm信息的默认值, 与 {@link SipConfig#alarm} 默认值保持一致
     */
    private static final boolean DEFAULT_SIP_ALARM = true;

    private static final String[] CLASSPATH_PATTERNS = {
            "classpath*:application-*.yml",
            "classpath*:application-*.yaml"
    };

    /**
     * 未指定 spring.config.location 时, 按 Spring Boot 默认搜索路径查找 application.yml
     * <p>
     * 按优先级从高到低排列(后者覆盖前者, 因此高优先级在前)
     */
    private static final String[] DEFAULT_LOCATION_CANDIDATES = {
            "optional:file:./config/*/",
            "optional:file:./config/",
            "optional:file:./",
            "optional:classpath:/config/",
            "optional:classpath:/"
    };

    @Autowired
    private ConfigurableEnvironment environment;

    /**
     * 运行中的 SIP 配置, 用于回显实际生效的值(单元测试中可能未注入)
     */
    @Autowired(required = false)
    private SipConfig sipConfig;

    /**
     * 运行中的平台策略配置, 用于回显默认值(单元测试中可能未注入)
     */
    @Autowired(required = false)
    private UserSetting userSetting;

    @Override
    public List<ProfileInfo> queryProfiles() {
        ConfigSource source = resolveConfigSource();
        if (source.isNone()) {
            log.warn("[系统设置] {}", source.describe());
            return new ArrayList<>();
        }

        // 可切换的配置文件列表 application-{id}.yml
        List<ConfigEntry> profileEntries = listProfileEntries(source);

        // 主配置文件 application.yml, 决定当前生效的profile以及是否支持切换
        ConfigEntry mainConfig = resolveMainConfig(source);

        // 当前生效的profile ID
        String activeId = resolveActiveProfileId(mainConfig);

        // 是否支持切换: 主配置文件中存在 spring.profiles
        boolean supportSwitch;
        if (mainConfig != null) {
            supportSwitch = hasProfilesConfig(mainConfig.getContent());
        } else {
            // 主配置文件缺失时, 只要存在携带 spring.profiles 的配置文件即认为支持
            supportSwitch = profileEntries.stream().anyMatch(entry -> hasProfilesConfig(entry.getContent()));
        }
        // 配置文件是否可写, 不可写时(位于jar内)不支持切换
        boolean writable = mainConfig != null && mainConfig.isWritable();

        List<ProfileInfo> result = new ArrayList<>();
        if (!supportSwitch) {
            // 没有 spring.profiles 配置, 只返回当前使用的一个, 不支持切换
            ConfigEntry current = mainConfig;
            if (current == null && activeId != null) {
                current = findById(profileEntries, activeId);
            }
            if (current == null && profileEntries.size() == 1) {
                current = profileEntries.get(0);
            }
            if (current != null) {
                result.add(toProfileInfo(current, true, writable));
            } else {
                log.warn("[系统设置] 未找到可用的配置文件, 来源: {}", source.describe());
            }
            return result;
        }

        // 支持切换, 返回全部候选配置, 并标记当前生效项
        boolean activeMarked = false;
        for (ConfigEntry entry : profileEntries) {
            boolean active = !activeMarked && activeId != null && activeId.equals(entry.getId());
            if (active) {
                activeMarked = true;
            }
            result.add(toProfileInfo(entry, active, writable));
        }
        if (!result.isEmpty() && !activeMarked) {
            log.warn("[系统设置] 未能在配置列表中找到当前生效的profile: {}", activeId);
        }
        return result;
    }

    @Override
    public String switchProfile(String profileId) {
        if (ObjectUtils.isEmpty(profileId)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "配置文件ID不能为空");
        }
        ConfigSource source = resolveConfigSource();
        if (source.isNone()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),
                    "spring.config.location 指定的配置路径不可用");
        }
        ConfigEntry mainConfig = resolveMainConfig(source);
        if (mainConfig == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到主配置文件 " + MAIN_CONFIG_NAMES[0]);
        }
        if (!hasProfilesConfig(mainConfig.getContent())) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),
                    "当前配置文件未配置 spring.profiles, 不支持切换");
        }
        if (!mainConfig.isWritable()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),
                    "当前配置文件位于jar内部, 无法修改。请使用 --spring.config.location 指定外部配置目录");
        }

        // 目标配置文件必须存在
        List<ConfigEntry> profileEntries = listProfileEntries(source);
        ConfigEntry target = findById(profileEntries, profileId);
        if (target == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "配置文件不存在: " + profileId);
        }

        String currentId = resolveActiveProfileId(mainConfig);
        if (profileId.equals(currentId)) {
            // 已经是目标配置, 幂等返回
            return mainConfig.getPath();
        }

        writeActiveProfile(mainConfig, profileId);
        log.info("[系统设置] 配置文件已切换: {} -> {} ({})", currentId, profileId, mainConfig.getPath());
        return mainConfig.getPath();
    }

    @Override
    public String readProfileContent(String profileId) {
        ConfigEntry entry = resolveEntry(profileId);
        if (entry.getContent() == null || entry.getContent().length == 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "配置文件内容为空或读取失败");
        }
        if (entry.getContent().length > MAX_CONTENT_LENGTH) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "配置文件过大, 不支持在线查看");
        }
        return new String(entry.getContent(), StandardCharsets.UTF_8);
    }

    @Override
    public WebServerConfig queryWebServerConfig() {
        ConfigEntry entry = resolveTargetConfigEntry();
        Map<String, Object> root = loadYaml(entry.getContent());

        WebServerConfig config = new WebServerConfig();
        Integer port = toInteger(readNested(root, "server", "port"));
        config.setPort(port != null ? port : readRuntimePort());
        config.setSslEnabled(toBoolean(readNested(root, "server", "ssl", "enabled")));
        config.setSslCertificate(toStringValue(readNested(root, "server", "ssl", "certificate")));
        config.setSslCertificatePrivateKey(toStringValue(readNested(root, "server", "ssl", "certificate-private-key")));

        config.setRuntimePort(readRuntimePort());
        config.setRuntimeSslEnabled(environment.getProperty("server.ssl.enabled", Boolean.class));

        config.setProfileId(isMainConfig(entry) ? null : entry.getId());
        config.setFilePath(entry.getPath());
        config.setWritable(entry.isWritable());
        return config;
    }

    @Override
    public String saveWebServerConfig(WebServerConfig config) {
        if (config == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "配置不能为空");
        }
        validateWebServerConfig(config);

        ConfigEntry entry = resolveTargetConfigEntry();
        if (!entry.isWritable() || entry.getFile() == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),
                    "当前配置文件位于jar内部或不可写, 无法修改。请使用 --spring.config.location 指定外部配置目录");
        }

        writeServerConfig(entry.getFile(), config);
        log.info("[系统设置] WEB服务配置已保存: {}", entry.getPath());
        return entry.getPath();
    }

    // ------------------------------------------------------------------
    // 国标服务(sip)
    // ------------------------------------------------------------------

    @Override
    public SipServerConfig querySipServerConfig() {
        ConfigEntry entry = resolveTargetConfigEntry();
        Map<String, Object> root = loadYaml(entry.getContent());

        SipServerConfig config = new SipServerConfig();
        config.setIp(toStringValue(readNested(root, "sip", "ip")));
        config.setShowIp(toStringValue(readNested(root, "sip", "show-ip")));
        Integer port = toInteger(readNested(root, "sip", "port"));
        config.setPort(port != null ? port : readRuntimeSipPort());
        config.setDomain(toStringValue(readNested(root, "sip", "domain")));
        config.setId(toStringValue(readNested(root, "sip", "id")));
        config.setPassword(toStringValue(readNested(root, "sip", "password")));
        Boolean alarm = toBoolean(readNested(root, "sip", "alarm"));
        config.setAlarm(alarm != null ? alarm : readRuntimeSipAlarm());
        Long timeout = toLong(readNested(root, "sip", "timeout"));
        config.setTimeout(timeout != null ? timeout : readRuntimeSipTimeout());

        if (sipConfig != null) {
            config.setRuntimePort(sipConfig.getPort());
            config.setRuntimeIp(sipConfig.getIp());
            config.setRuntimeShowIp(sipConfig.getShowIp());
            config.setRuntimeMonitorIps(sipConfig.getMonitorIps());
        }

        config.setProfileId(isMainConfig(entry) ? null : entry.getId());
        config.setFilePath(entry.getPath());
        config.setWritable(entry.isWritable());
        return config;
    }

    @Override
    public String saveSipServerConfig(SipServerConfig config) {
        if (config == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "配置不能为空");
        }
        validateSipServerConfig(config);
        File file = resolveWritableConfigFile();
        List<String> lines = readConfigLines(file);

        setYamlScalar(lines, new String[]{"sip", "port"}, String.valueOf(config.getPort()));
        if (config.getId() != null) {
            setYamlScalar(lines, new String[]{"sip", "id"}, config.getId().trim());
        }
        if (config.getDomain() != null) {
            setYamlScalar(lines, new String[]{"sip", "domain"}, config.getDomain().trim());
        }
        if (config.getPassword() != null) {
            setYamlScalar(lines, new String[]{"sip", "password"}, config.getPassword());
        }
        if (config.getIp() != null) {
            setYamlScalar(lines, new String[]{"sip", "ip"}, config.getIp().trim());
        }
        if (config.getShowIp() != null) {
            setYamlScalar(lines, new String[]{"sip", "show-ip"}, config.getShowIp().trim());
        }
        if (config.getAlarm() != null) {
            setYamlScalar(lines, new String[]{"sip", "alarm"}, String.valueOf(config.getAlarm()));
        }
        if (config.getTimeout() != null) {
            setYamlScalar(lines, new String[]{"sip", "timeout"}, String.valueOf(config.getTimeout()));
        }

        writeConfigLines(file, lines);
        log.info("[系统设置] 国标服务配置已保存: {}", file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    // ------------------------------------------------------------------
    // 部标服务(ftp / jt1078)
    // ------------------------------------------------------------------

    @Override
    public JtServerConfig queryJtServerConfig() {
        ConfigEntry entry = resolveTargetConfigEntry();
        Map<String, Object> root = loadYaml(entry.getContent());

        JtServerConfig config = new JtServerConfig();
        config.setFtpEnable(toBoolean(readNested(root, "ftp", "enable")));
        config.setFtpPort(toInteger(readNested(root, "ftp", "port")));
        config.setFtpPassivePorts(toStringValue(readNested(root, "ftp", "passive-ports")));
        config.setJt1078Enable(toBoolean(readNested(root, "jt1078", "enable")));
        config.setJt1078Port(toInteger(readNested(root, "jt1078", "port")));
        config.setJt1078Password(toStringValue(readNested(root, "jt1078", "password")));
        config.setJt1078Record(toBoolean(readNested(root, "jt1078", "record")));
        config.setJt1078ReaderIdleTime(toInteger(readNested(root, "jt1078", "reader-idle-time")));
        config.setRuntimeFtpEnable(environment.getProperty("ftp.enable", Boolean.class));
        config.setRuntimeJt1078Enable(environment.getProperty("jt1078.enable", Boolean.class));

        config.setProfileId(isMainConfig(entry) ? null : entry.getId());
        config.setFilePath(entry.getPath());
        config.setWritable(entry.isWritable());
        return config;
    }

    @Override
    public String saveJtServerConfig(JtServerConfig config) {
        if (config == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "配置不能为空");
        }
        validateJtServerConfig(config);
        File file = resolveWritableConfigFile();
        List<String> lines = readConfigLines(file);

        if (config.getFtpEnable() != null) {
            setYamlScalar(lines, new String[]{"ftp", "enable"}, String.valueOf(config.getFtpEnable()));
        }
        if (config.getFtpPort() != null) {
            setYamlScalar(lines, new String[]{"ftp", "port"}, String.valueOf(config.getFtpPort()));
        }
        if (config.getFtpPassivePorts() != null) {
            setYamlScalar(lines, new String[]{"ftp", "passive-ports"}, config.getFtpPassivePorts().trim());
        }
        if (config.getJt1078Enable() != null) {
            setYamlScalar(lines, new String[]{"jt1078", "enable"}, String.valueOf(config.getJt1078Enable()));
        }
        if (config.getJt1078Port() != null) {
            setYamlScalar(lines, new String[]{"jt1078", "port"}, String.valueOf(config.getJt1078Port()));
        }
        if (config.getJt1078Password() != null) {
            setYamlScalar(lines, new String[]{"jt1078", "password"}, config.getJt1078Password());
        }
        if (config.getJt1078Record() != null) {
            setYamlScalar(lines, new String[]{"jt1078", "record"}, String.valueOf(config.getJt1078Record()));
        }
        if (config.getJt1078ReaderIdleTime() != null) {
            setYamlScalar(lines, new String[]{"jt1078", "reader-idle-time"},
                    String.valueOf(config.getJt1078ReaderIdleTime()));
        }

        writeConfigLines(file, lines);
        log.info("[系统设置] 部标服务配置已保存: {}", file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    // ------------------------------------------------------------------
    // 全局策略-流媒体(media) - 默认媒体节点配置
    // ------------------------------------------------------------------

    @Override
    public void saveDefaultMediaConfig(MediaServer mediaServer) {
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "配置不能为空");
        }
        File file = resolveWritableConfigFile();
        List<String> lines = readConfigLines(file);

        if (!ObjectUtils.isEmpty(mediaServer.getId())) {
            setYamlScalar(lines, new String[]{"media", "id"}, mediaServer.getId().trim());
        }
        if (!ObjectUtils.isEmpty(mediaServer.getIp())) {
            setYamlScalar(lines, new String[]{"media", "ip"}, mediaServer.getIp().trim());
        }
        if (!ObjectUtils.isEmpty(mediaServer.getHookIp())) {
            setYamlScalar(lines, new String[]{"media", "hook-ip"}, mediaServer.getHookIp().trim());
        }
        // sdp-ip / stream-ip 允许置空(表示回退到 media.ip)
        if (mediaServer.getSdpIp() != null) {
            setYamlScalar(lines, new String[]{"media", "sdp-ip"}, mediaServer.getSdpIp().trim());
        }
        if (mediaServer.getStreamIp() != null) {
            setYamlScalar(lines, new String[]{"media", "stream-ip"}, mediaServer.getStreamIp().trim());
        }
        if (mediaServer.getHttpPort() > 0) {
            setYamlScalar(lines, new String[]{"media", "http-port"}, String.valueOf(mediaServer.getHttpPort()));
        }
        if (!ObjectUtils.isEmpty(mediaServer.getSecret())) {
            setYamlScalar(lines, new String[]{"media", "secret"}, mediaServer.getSecret());
        }
        setYamlScalar(lines, new String[]{"media", "auto-config"}, String.valueOf(mediaServer.isAutoConfig()));
        setYamlScalar(lines, new String[]{"media", "rtp", "enable"}, String.valueOf(mediaServer.isRtpEnable()));
        // 端口范围为空时不写, 避免把已有配置写成空值
        if (!ObjectUtils.isEmpty(mediaServer.getRtpPortRange())) {
            setYamlScalar(lines, new String[]{"media", "rtp", "port-range"}, mediaServer.getRtpPortRange().trim());
        }
        if (!ObjectUtils.isEmpty(mediaServer.getSendRtpPortRange())) {
            setYamlScalar(lines, new String[]{"media", "rtp", "send-port-range"},
                    mediaServer.getSendRtpPortRange().trim());
        }
        if (mediaServer.getRecordAssistPort() >= 0) {
            setYamlScalar(lines, new String[]{"media", "record-assist-port"},
                    String.valueOf(mediaServer.getRecordAssistPort()));
        }
        if (mediaServer.getRecordDay() > 0) {
            setYamlScalar(lines, new String[]{"media", "record-day"}, String.valueOf(mediaServer.getRecordDay()));
        }
        if (!ObjectUtils.isEmpty(mediaServer.getRecordPath())) {
            setYamlScalar(lines, new String[]{"media", "record-path"}, mediaServer.getRecordPath().trim());
        }
        if (!ObjectUtils.isEmpty(mediaServer.getType())) {
            setYamlScalar(lines, new String[]{"media", "type"}, mediaServer.getType().trim());
        }

        writeConfigLines(file, lines);
        log.info("[系统设置] 默认媒体节点配置已回写: {}", file.getAbsolutePath());
    }

    @Override
    public boolean isActiveConfigWritable() {
        try {
            ConfigEntry entry = resolveTargetConfigEntry();
            return entry.isWritable() && entry.getFile() != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 全局策略-平台策略(user-settings)
    // ------------------------------------------------------------------

    @Override
    public UserSettingConfig queryUserSettingConfig() {
        ConfigEntry entry = resolveTargetConfigEntry();
        Map<String, Object> root = loadYaml(entry.getContent());
        UserSetting defaults = userSettingOrDefaults();

        UserSettingConfig config = new UserSettingConfig();

        // 基础配置
        config.setInterfaceAuthentication(readBoolean(root, new String[]{"user-settings", "interface-authentication"},
                defaults.getInterfaceAuthentication()));
        config.setInterfaceAuthenticationExcludes(readStringList(root,
                new String[]{"user-settings", "interface-authentication-excludes"},
                defaults.getInterfaceAuthenticationExcludes()));
        config.setUseSourceIpAsStreamIp(readBoolean(root, new String[]{"user-settings", "use-source-ip-as-stream-ip"},
                defaults.getUseSourceIpAsStreamIp()));
        config.setSqlLog(readBoolean(root, new String[]{"user-settings", "sql-log"}, defaults.getSqlLog()));
        config.setDeviceStatusNotify(readBoolean(root, new String[]{"user-settings", "device-status-notify"},
                defaults.getDeviceStatusNotify()));
        config.setDocEnable(readBoolean(root, new String[]{"user-settings", "doc-enable"}, defaults.getDocEnable()));
        config.setServerId(readString(root, new String[]{"user-settings", "server-id"}, defaults.getServerId()));
        config.setCivilCodeFile(readString(root, new String[]{"user-settings", "civil-code-file"},
                defaults.getCivilCodeFile()));
        config.setAllowedOrigins(readStringList(root, new String[]{"user-settings", "allowed-origins"},
                defaults.getAllowedOrigins()));
        config.setMaxNotifyCountQueue(readInteger(root, new String[]{"user-settings", "max-notify-count-queue"},
                defaults.getMaxNotifyCountQueue()));
        config.setLoginTimeout(readLongValue(root, new String[]{"user-settings", "login-timeout"},
                defaults.getLoginTimeout()));
        config.setJwkFile(readString(root, new String[]{"user-settings", "jwk-file"}, defaults.getJwkFile()));
        config.setSavePositionHistory(readBoolean(root, new String[]{"user-settings", "save-position-history"},
                defaults.getSavePositionHistory()));

        // 视频播放
        config.setAutoApplyPlay(readBoolean(root, new String[]{"user-settings", "auto-apply-play"},
                defaults.getAutoApplyPlay()));
        config.setPlayTimeout(readInteger(root, new String[]{"user-settings", "play-timeout"},
                defaults.getPlayTimeout()));
        config.setRecordInfoTimeout(readInteger(root, new String[]{"user-settings", "record-info-timeout"},
                defaults.getRecordInfoTimeout()));
        config.setStreamOnDemand(readBoolean(root, new String[]{"user-settings", "stream-on-demand"},
                defaults.getStreamOnDemand()));
        config.setSnapAsync(readBoolean(root, new String[]{"user-settings", "snap-async"},
                defaults.isSnapAsync()));

        // 推流控制
        config.setPushAuthority(readBoolean(root, new String[]{"user-settings", "push-authority"},
                defaults.getPushAuthority()));
        config.setRecordPushLive(readBoolean(root, new String[]{"user-settings", "record-push-live"},
                defaults.getRecordPushLive()));
        config.setUsePushingAsStatus(readBoolean(root, new String[]{"user-settings", "use-pushing-as-status"},
                defaults.getUsePushingAsStatus()));
        config.setUseAliasForGroupSync(readBoolean(root, new String[]{"user-settings", "use-alias-for-group-sync"},
                defaults.isUseAliasForGroupSync()));

        // 国标服务
        config.setDeviceIdStrict(readBoolean(root, new String[]{"user-settings", "device-id-strict"},
                defaults.isDeviceIdStrict()));
        config.setSeniorSdp(readBoolean(root, new String[]{"user-settings", "senior-sdp"}, defaults.getSeniorSdp()));
        config.setSipLog(readBoolean(root, new String[]{"user-settings", "sip-log"}, defaults.getSipLog()));
        config.setSipUseSourceIpAsRemoteAddress(readBoolean(root,
                new String[]{"user-settings", "sip-use-source-ip-as-remote-address"},
                defaults.getSipUseSourceIpAsRemoteAddress()));
        config.setRecordSip(readBoolean(root, new String[]{"user-settings", "record-sip"}, defaults.getRecordSip()));
        config.setSyncChannelOnDeviceOnline(readBoolean(root,
                new String[]{"user-settings", "sync-channel-on-device-online"}, defaults.getSyncChannelOnDeviceOnline()));
        config.setSendToPlatformsWhenIdLost(readBoolean(root,
                new String[]{"user-settings", "send-to-platforms-when-id-lost"},
                defaults.getSendToPlatformsWhenIdLost()));
        config.setRefuseChannelStatusChannelFormNotify(readBoolean(root,
                new String[]{"user-settings", "refuse-channel-status-channel-form-notify"},
                defaults.getRefuseChannelStatusChannelFormNotify()));
        config.setSsrcRandom(readBoolean(root, new String[]{"user-settings", "ssrc-random"},
                defaults.getSsrcRandom()));
        config.setGbDeviceOnline(readInteger(root, new String[]{"user-settings", "gb-device-online"},
                defaults.getGbDeviceOnline()));
        config.setSipCacheServerConnections(readBoolean(root,
                new String[]{"user-settings", "sip-cache-server-connections"}, defaults.isSipCacheServerConnections()));
        config.setDisableDateHeader(readBoolean(root, new String[]{"user-settings", "disable-date-header"},
                defaults.isDisableDateHeader()));
        config.setGroupSyncDeviceTemplate(readString(root, new String[]{"user-settings", "group-sync-device-template"},
                defaults.getGroupSyncDeviceTemplate()));
        config.setSubscribeMobilePosition(readBoolean(root, new String[]{"user-settings", "subscribe-mobile-position"},
                defaults.isSubscribeMobilePosition()));
        config.setDeviceRegisterTimeMaxCount(readInteger(root,
                new String[]{"user-settings", "device-register-time-max-count"},
                defaults.getDeviceRegisterTimeMaxCount()));
        config.setDeviceRegisterTimeTtlHours(readInteger(root,
                new String[]{"user-settings", "device-register-time-ttl-hours"},
                defaults.getDeviceRegisterTimeTtlHours()));
        config.setDeviceKeepaliveTimeMaxCount(readInteger(root,
                new String[]{"user-settings", "device-keepalive-time-max-count"},
                defaults.getDeviceKeepaliveTimeMaxCount()));
        config.setDeviceKeepaliveTimeTtlHours(readInteger(root,
                new String[]{"user-settings", "device-keepalive-time-ttl-hours"},
                defaults.getDeviceKeepaliveTimeTtlHours()));

        // 国标级联
        config.setPlatformPlayTimeout(readInteger(root, new String[]{"user-settings", "platform-play-timeout"},
                defaults.getPlatformPlayTimeout()));
        config.setUseCustomSsrcForParentInvite(readBoolean(root,
                new String[]{"user-settings", "use-custom-ssrc-for-parent-invite"},
                defaults.getUseCustomSsrcForParentInvite()));
        config.setBroadcastForPlatform(readString(root, new String[]{"user-settings", "broadcast-for-platform"},
                defaults.getBroadcastForPlatform()));
        config.setRegisterAgainAfterTime(readInteger(root, new String[]{"user-settings", "register-again-after-time"},
                defaults.getRegisterAgainAfterTime()));
        config.setRegisterKeepIntDialog(readBoolean(root, new String[]{"user-settings", "register-keep-int-dialog"},
                defaults.isRegisterKeepIntDialog()));
        config.setAutoRegisterPlatform(readBoolean(root, new String[]{"user-settings", "auto-register-platform"},
                defaults.isAutoRegisterPlatform()));
        config.setSendPositionOnDemand(readBoolean(root, new String[]{"user-settings", "send-position-on-demand"},
                defaults.isSendPositionOnDemand()));

        // 报警控制
        config.setAlarmCatchSize(readLongValue(root, new String[]{"user-settings", "alarm-catch-size"},
                defaults.getAlarmCatchSize()));
        config.setAlarmSnapByStream(readBoolean(root, new String[]{"user-settings", "alarm-snap-by-stream"},
                defaults.isAlarmSnapByStream()));
        config.setAllowedAlarmType(readStringList(root, new String[]{"user-settings", "allowed-alarm-type"},
                alarmTypeNames(defaults.getAllowedAlarmType())));
        config.setAlarmKeepDays(readInteger(root, new String[]{"user-settings", "alarm-keep-days"},
                defaults.getAlarmKeepDays()));

        config.setProfileId(isMainConfig(entry) ? null : entry.getId());
        config.setFilePath(entry.getPath());
        config.setWritable(entry.isWritable());
        return config;
    }

    @Override
    public String saveUserSettingConfig(UserSettingConfig config) {
        if (config == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "配置不能为空");
        }
        validateUserSettingConfig(config);
        File file = resolveWritableConfigFile();
        List<String> lines = readConfigLines(file);

        String prefix = "user-settings";

        // 基础配置
        putYamlScalar(lines, prefix, "interface-authentication", config.getInterfaceAuthentication());
        putYamlList(lines, new String[]{prefix, "interface-authentication-excludes"},
                config.getInterfaceAuthenticationExcludes());
        putYamlScalar(lines, prefix, "use-source-ip-as-stream-ip", config.getUseSourceIpAsStreamIp());
        putYamlScalar(lines, prefix, "sql-log", config.getSqlLog());
        putYamlScalar(lines, prefix, "device-status-notify", config.getDeviceStatusNotify());
        putYamlScalar(lines, prefix, "doc-enable", config.getDocEnable());
        putYamlScalar(lines, prefix, "server-id", config.getServerId());
        putYamlScalar(lines, prefix, "civil-code-file", config.getCivilCodeFile());
        putYamlList(lines, new String[]{prefix, "allowed-origins"}, config.getAllowedOrigins());
        putYamlScalar(lines, prefix, "max-notify-count-queue", config.getMaxNotifyCountQueue());
        putYamlScalar(lines, prefix, "login-timeout", config.getLoginTimeout());
        putYamlScalar(lines, prefix, "jwk-file", config.getJwkFile());
        putYamlScalar(lines, prefix, "save-position-history", config.getSavePositionHistory());

        // 视频播放
        putYamlScalar(lines, prefix, "auto-apply-play", config.getAutoApplyPlay());
        putYamlScalar(lines, prefix, "play-timeout", config.getPlayTimeout());
        putYamlScalar(lines, prefix, "record-info-timeout", config.getRecordInfoTimeout());
        putYamlScalar(lines, prefix, "stream-on-demand", config.getStreamOnDemand());
        putYamlScalar(lines, prefix, "snap-async", config.getSnapAsync());

        // 推流控制
        putYamlScalar(lines, prefix, "push-authority", config.getPushAuthority());
        putYamlScalar(lines, prefix, "record-push-live", config.getRecordPushLive());
        putYamlScalar(lines, prefix, "use-pushing-as-status", config.getUsePushingAsStatus());
        putYamlScalar(lines, prefix, "use-alias-for-group-sync", config.getUseAliasForGroupSync());

        // 国标服务
        putYamlScalar(lines, prefix, "device-id-strict", config.getDeviceIdStrict());
        putYamlScalar(lines, prefix, "senior-sdp", config.getSeniorSdp());
        putYamlScalar(lines, prefix, "sip-log", config.getSipLog());
        putYamlScalar(lines, prefix, "sip-use-source-ip-as-remote-address",
                config.getSipUseSourceIpAsRemoteAddress());
        putYamlScalar(lines, prefix, "record-sip", config.getRecordSip());
        putYamlScalar(lines, prefix, "sync-channel-on-device-online", config.getSyncChannelOnDeviceOnline());
        putYamlScalar(lines, prefix, "send-to-platforms-when-id-lost", config.getSendToPlatformsWhenIdLost());
        putYamlScalar(lines, prefix, "refuse-channel-status-channel-form-notify",
                config.getRefuseChannelStatusChannelFormNotify());
        putYamlScalar(lines, prefix, "ssrc-random", config.getSsrcRandom());
        putYamlScalar(lines, prefix, "gb-device-online", config.getGbDeviceOnline());
        putYamlScalar(lines, prefix, "sip-cache-server-connections", config.getSipCacheServerConnections());
        putYamlScalar(lines, prefix, "disable-date-header", config.getDisableDateHeader());
        putYamlScalar(lines, prefix, "group-sync-device-template", config.getGroupSyncDeviceTemplate());
        putYamlScalar(lines, prefix, "subscribe-mobile-position", config.getSubscribeMobilePosition());
        putYamlScalar(lines, prefix, "device-register-time-max-count", config.getDeviceRegisterTimeMaxCount());
        putYamlScalar(lines, prefix, "device-register-time-ttl-hours", config.getDeviceRegisterTimeTtlHours());
        putYamlScalar(lines, prefix, "device-keepalive-time-max-count", config.getDeviceKeepaliveTimeMaxCount());
        putYamlScalar(lines, prefix, "device-keepalive-time-ttl-hours", config.getDeviceKeepaliveTimeTtlHours());

        // 国标级联
        putYamlScalar(lines, prefix, "platform-play-timeout", config.getPlatformPlayTimeout());
        putYamlScalar(lines, prefix, "use-custom-ssrc-for-parent-invite",
                config.getUseCustomSsrcForParentInvite());
        putYamlScalar(lines, prefix, "broadcast-for-platform", config.getBroadcastForPlatform());
        putYamlScalar(lines, prefix, "register-again-after-time", config.getRegisterAgainAfterTime());
        putYamlScalar(lines, prefix, "register-keep-int-dialog", config.getRegisterKeepIntDialog());
        putYamlScalar(lines, prefix, "auto-register-platform", config.getAutoRegisterPlatform());
        putYamlScalar(lines, prefix, "send-position-on-demand", config.getSendPositionOnDemand());

        // 报警控制
        putYamlScalar(lines, prefix, "alarm-catch-size", config.getAlarmCatchSize());
        putYamlScalar(lines, prefix, "alarm-snap-by-stream", config.getAlarmSnapByStream());
        putYamlList(lines, new String[]{prefix, "allowed-alarm-type"}, config.getAllowedAlarmType());
        putYamlScalar(lines, prefix, "alarm-keep-days", config.getAlarmKeepDays());

        writeConfigLines(file, lines);
        log.info("[系统设置] 平台策略配置已保存: {}", file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    @Override
    public List<Map<String, String>> queryAlarmTypes() {
        List<Map<String, String>> result = new ArrayList<>();
        for (AlarmType type : AlarmType.values()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("name", type.name());
            item.put("description", type.getDescription());
            result.add(item);
        }
        return result;
    }

    /**
     * 定位当前生效的配置文件(application-{profile}.yml), 未找到时回退到主配置文件
     */
    private ConfigEntry resolveTargetConfigEntry() {
        ConfigSource source = resolveConfigSource();
        if (source.isNone()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),
                    "未找到可用的配置文件, 请使用 --spring.config.location 指定配置目录");
        }
        ConfigEntry mainConfig = resolveMainConfig(source);
        String activeId = resolveActiveProfileId(mainConfig);
        if (!ObjectUtils.isEmpty(activeId)) {
            ConfigEntry entry = findById(listProfileEntries(source), activeId);
            if (entry != null) {
                return entry;
            }
            log.warn("[系统设置] 未找到当前生效的配置文件: application-{}.yml", activeId);
        }
        if (mainConfig != null) {
            return mainConfig;
        }
        throw new ControllerException(ErrorCode.ERROR100.getCode(),
                "未找到当前生效的配置文件, 请使用 --spring.config.location 指定配置目录");
    }

    private boolean isMainConfig(ConfigEntry entry) {
        if (entry == null || entry.getFileName() == null) {
            return false;
        }
        for (String name : MAIN_CONFIG_NAMES) {
            if (name.equalsIgnoreCase(entry.getFileName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 读取运行中实际生效的服务端口, 未配置时按 Spring Boot 默认值 8080
     */
    private Integer readRuntimePort() {
        Integer port = environment.getProperty("server.port", Integer.class);
        return port != null ? port : 8080;
    }

    /**
     * 按路径读取嵌套的配置值
     */
    @SuppressWarnings("unchecked")
    private Object readNested(Map<String, Object> root, String... path) {
        if (root == null) {
            return null;
        }
        Object current = root;
        for (String key : path) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(key);
        }
        return current;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(value.toString().trim());
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 读取运行中实际生效的 SIP 端口
     */
    private Integer readRuntimeSipPort() {
        if (sipConfig != null) {
            return sipConfig.getPort();
        }
        return environment.getProperty("sip.port", Integer.class);
    }

    /**
     * 读取运行中实际生效的国标命令超时时间, 未注入 sipConfig 时回退到 environment / 默认值
     */
    private Long readRuntimeSipTimeout() {
        if (sipConfig != null) {
            return sipConfig.getTimeout();
        }
        return environment.getProperty("sip.timeout", Long.class, DEFAULT_SIP_TIMEOUT);
    }

    /**
     * 读取运行中实际生效的国标alarm开关, 未注入 sipConfig 时回退到 environment / 默认值
     */
    private Boolean readRuntimeSipAlarm() {
        if (sipConfig != null) {
            return sipConfig.isAlarm();
        }
        return environment.getProperty("sip.alarm", Boolean.class, DEFAULT_SIP_ALARM);
    }

    private Boolean readBoolean(Map<String, Object> root, String[] path, Boolean fallback) {
        Boolean value = toBoolean(readNested(root, path));
        return value != null ? value : fallback;
    }

    private Integer readInteger(Map<String, Object> root, String[] path, Integer fallback) {
        Integer value = toInteger(readNested(root, path));
        return value != null ? value : fallback;
    }

    private Long readLongValue(Map<String, Object> root, String[] path, Long fallback) {
        Long value = toLong(readNested(root, path));
        return value != null ? value : fallback;
    }

    private String readString(Map<String, Object> root, String[] path, String fallback) {
        String value = toStringValue(readNested(root, path));
        return value != null ? value : fallback;
    }

    /**
     * 读取列表值, 支持 YAML 列表与逗号分隔的字符串
     */
    @SuppressWarnings("unchecked")
    private List<String> readStringList(Map<String, Object> root, String[] path, List<String> fallback) {
        Object value = readNested(root, path);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        if (value instanceof String) {
            List<String> result = new ArrayList<>();
            for (String item : ((String) value).split(",")) {
                if (!item.trim().isEmpty()) {
                    result.add(item.trim());
                }
            }
            return result;
        }
        return fallback != null ? fallback : new ArrayList<>();
    }

    private List<String> alarmTypeNames(List<AlarmType> types) {
        if (types == null) {
            return new ArrayList<>();
        }
        return types.stream().map(Enum::name).collect(Collectors.toList());
    }

    private UserSetting userSettingOrDefaults() {
        return userSetting != null ? userSetting : new UserSetting();
    }

    /**
     * 校验 WEB服务配置
     */
    private void validateWebServerConfig(WebServerConfig config) {
        Integer port = config.getPort();
        if (port == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "服务端口不能为空");
        }
        if (port < 1 || port > 65535) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "服务端口必须在 1-65535 之间");
        }
        if (Boolean.TRUE.equals(config.getSslEnabled())) {
            if (ObjectUtils.isEmpty(config.getSslCertificate())) {
                throw new ControllerException(ErrorCode.ERROR400.getCode(), "开启HTTPS时必须指定PEM证书文件");
            }
            if (ObjectUtils.isEmpty(config.getSslCertificatePrivateKey())) {
                throw new ControllerException(ErrorCode.ERROR400.getCode(), "开启HTTPS时必须指定私钥文件");
            }
            File certificate = new File(config.getSslCertificate());
            if (!certificate.isFile() || !certificate.canRead()) {
                throw new ControllerException(ErrorCode.ERROR400.getCode(),
                        "证书文件不存在或不可读: " + config.getSslCertificate());
            }
            File privateKey = new File(config.getSslCertificatePrivateKey());
            if (!privateKey.isFile() || !privateKey.canRead()) {
                throw new ControllerException(ErrorCode.ERROR400.getCode(),
                        "私钥文件不存在或不可读: " + config.getSslCertificatePrivateKey());
            }
        }
    }

    /**
     * 校验国标 SIP 服务配置
     */
    private void validateSipServerConfig(SipServerConfig config) {
        Integer port = config.getPort();
        if (port == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "SIP服务端口不能为空");
        }
        if (port < 1 || port > 65535) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "SIP服务端口必须在 1-65535 之间");
        }
        if (ObjectUtils.isEmpty(config.getId())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "国标编号不能为空");
        }
        if (!config.getId().trim().matches("\\d{20}")) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "国标编号必须为20位数字");
        }
        if (!ObjectUtils.isEmpty(config.getDomain()) && !config.getDomain().trim().matches("\\d{10}")) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "国标域必须为10位数字");
        }
        if (config.getTimeout() != null && config.getTimeout() <= 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "命令超时时间必须大于0");
        }
    }

    /**
     * 校验部标服务配置
     */
    private void validateJtServerConfig(JtServerConfig config) {
        validatePort(config.getFtpPort(), "FTP服务端口");
        validatePort(config.getJt1078Port(), "JT1078服务端口");
        if (config.getFtpPassivePorts() != null && !config.getFtpPassivePorts().trim().isEmpty()) {
            String value = config.getFtpPassivePorts().trim();
            for (String item : value.split(",")) {
                if (!item.trim().matches("\\d+(-\\d+)?")) {
                    throw new ControllerException(ErrorCode.ERROR400.getCode(),
                            "FTP被动模式端口范围格式不正确, 应为 10000-10500");
                }
            }
        }
        if (config.getJt1078ReaderIdleTime() != null && config.getJt1078ReaderIdleTime() < 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "IDLE超时时间不能小于0");
        }
    }

    /**
     * 校验流媒体配置
     */
    /**
     * 校验平台策略配置
     */
    private void validateUserSettingConfig(UserSettingConfig config) {
        validatePositive(config.getLoginTimeout(), "登录超时时间");
        validatePositive(config.getPlayTimeout(), "点播等待超时时间");
        validatePositive(config.getRecordInfoTimeout(), "录像查询超时时间");
        validatePositive(config.getPlatformPlayTimeout(), "上级点播等待超时时间");
        validateNotNegative(config.getRegisterAgainAfterTime(), "级联重注册间隔");
        validateNotNegative(config.getMaxNotifyCountQueue(), "notify缓存队列长度");
        validateNotNegative(config.getAlarmCatchSize(), "报警通道缓存大小");
        validateNotNegative(config.getAlarmKeepDays(), "报警记录保留天数");
        validateNotNegative(config.getDeviceRegisterTimeMaxCount(), "设备注册时间最大条数");
        validateNotNegative(config.getDeviceRegisterTimeTtlHours(), "设备注册时间过期时间");
        validateNotNegative(config.getDeviceKeepaliveTimeMaxCount(), "设备心跳时间最大条数");
        validateNotNegative(config.getDeviceKeepaliveTimeTtlHours(), "设备心跳时间过期时间");
        if (config.getGbDeviceOnline() != null && config.getGbDeviceOnline() != 0 && config.getGbDeviceOnline() != 1) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "设备离线上线策略只能是 0 或 1");
        }
        if (!ObjectUtils.isEmpty(config.getBroadcastForPlatform())) {
            String mode = config.getBroadcastForPlatform().trim();
            if (!"UDP".equalsIgnoreCase(mode) && !"TCP-ACTIVE".equalsIgnoreCase(mode)
                    && !"TCP-PASSIVE".equalsIgnoreCase(mode)) {
                throw new ControllerException(ErrorCode.ERROR400.getCode(),
                        "喊话发流模式只能是 UDP / TCP-ACTIVE / TCP-PASSIVE");
            }
        }
        if (config.getAllowedAlarmType() != null) {
            for (String name : config.getAllowedAlarmType()) {
                if (ObjectUtils.isEmpty(name)) {
                    continue;
                }
                try {
                    AlarmType.valueOf(name.trim());
                } catch (IllegalArgumentException e) {
                    throw new ControllerException(ErrorCode.ERROR400.getCode(), "非法的报警类型: " + name);
                }
            }
        }
    }

    private void validatePort(Integer port, String name) {
        validatePort(port, name, false);
    }

    private void validatePort(Integer port, String name, boolean allowZero) {
        if (port == null) {
            return;
        }
        int min = allowZero ? 0 : 1;
        if (port < min || port > 65535) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(),
                    name + "必须在 " + min + "-65535 之间");
        }
    }

    private void validatePositive(Number value, String name) {
        if (value != null && value.longValue() <= 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), name + "必须大于0");
        }
    }

    private void validateNotNegative(Number value, String name) {
        if (value != null && value.longValue() < 0) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), name + "不能小于0");
        }
    }

    /**
     * 定位当前生效的配置文件并要求其可写
     */
    private File resolveWritableConfigFile() {
        ConfigEntry entry = resolveTargetConfigEntry();
        if (!entry.isWritable() || entry.getFile() == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),
                    "当前配置文件位于jar内部或不可写, 无法修改。请使用 --spring.config.location 指定外部配置目录");
        }
        return entry.getFile();
    }

    private List<String> readConfigLines(File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "读取配置文件失败: " + e.getMessage());
        }
    }

    private void writeConfigLines(File file, List<String> lines) {
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "写入配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 写入单个配置项(值为 null 时跳过)
     */
    private void putYamlScalar(List<String> lines, String prefix, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String) {
            setYamlScalar(lines, new String[]{prefix, key}, quoteYamlString((String) value));
        } else {
            setYamlScalar(lines, new String[]{prefix, key}, String.valueOf(value));
        }
    }

    /**
     * 对可能被 YAML 解析为非字符串(数字/布尔/null)或含特殊字符的字符串值加双引号
     */
    private String quoteYamlString(String value) {
        if (value.isEmpty()) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value;
        }
        boolean ambiguous = value.matches("(?i)true|false|null|~|yes|no|on|off")
                || value.matches("[-+]?(\\d[\\d_]*)(\\.\\d+)?")
                || value.matches("[-+]?\\.(inf|nan)")
                || value.matches("0[xX][0-9a-fA-F_]+")
                || value.matches("0[oO]?[0-7_]+");
        boolean special = value.matches(".*[:#\\[\\]{},&*!|>'\"%@`].*")
                || !value.equals(value.trim());
        if (ambiguous || special) {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return value;
    }

    /**
     * 写入列表配置项(值为 null 时跳过)
     */
    private void putYamlList(List<String> lines, String[] path, List<String> values) {
        if (values == null) {
            return;
        }
        setYamlList(lines, path, values);
    }

    /**
     * 将 WEB服务配置写入当前生效的配置文件, 保留原有注释与格式
     */
    private void writeServerConfig(File file, WebServerConfig config) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "读取配置文件失败: " + e.getMessage());
        }

        setYamlScalar(lines, new String[]{"server", "port"}, String.valueOf(config.getPort()));
        setYamlScalar(lines, new String[]{"server", "ssl", "enabled"},
                String.valueOf(Boolean.TRUE.equals(config.getSslEnabled())));
        if (!ObjectUtils.isEmpty(config.getSslCertificate())) {
            setYamlScalar(lines, new String[]{"server", "ssl", "certificate"}, config.getSslCertificate());
        }
        if (!ObjectUtils.isEmpty(config.getSslCertificatePrivateKey())) {
            setYamlScalar(lines, new String[]{"server", "ssl", "certificate-private-key"},
                    config.getSslCertificatePrivateKey());
        }

        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "写入配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 在 YAML 行集合中设置嵌套键的值(保留注释与缩进), 键或所在的块不存在时自动创建
     *
     * @param lines 行集合(原地修改)
     * @param path  键路径, 如 [server, ssl, enabled]
     * @param value 值(原样写入, 不做转义)
     */
    private void setYamlScalar(List<String> lines, String[] path, String value) {
        int parentIndent = -2;
        int rangeStart = 0;
        int rangeEnd = lines.size();
        for (int i = 0; i < path.length; i++) {
            String key = path[i];
            boolean last = i == path.length - 1;
            int keyIndent = parentIndent + 2;

            int found = findKeyLine(lines, rangeStart, rangeEnd, key, keyIndent);
            if (last) {
                String rendered = renderYamlScalar(key, value);
                if (found >= 0) {
                    String comment = extractTrailingComment(lines.get(found));
                    lines.set(found, repeatSpace(keyIndent) + rendered + comment);
                } else {
                    lines.add(rangeEnd, repeatSpace(keyIndent) + rendered);
                }
                return;
            }

            if (found >= 0) {
                int childEnd = findBlockEnd(lines, found, rangeEnd, keyIndent);
                parentIndent = keyIndent;
                rangeStart = found + 1;
                rangeEnd = childEnd;
            } else {
                lines.add(rangeEnd, repeatSpace(keyIndent) + key + ":");
                parentIndent = keyIndent;
                rangeStart = rangeEnd + 1;
                rangeEnd = rangeEnd + 1;
            }
        }
    }

    /**
     * 在 YAML 行集合中设置列表键的值(保留注释与缩进), 键或所在的块不存在时自动创建
     *
     * @param lines  行集合(原地修改)
     * @param path   键路径, 如 [user-settings, allowed-origins]
     * @param values 列表值
     */
    private void setYamlList(List<String> lines, String[] path, List<String> values) {
        int parentIndent = -2;
        int rangeStart = 0;
        int rangeEnd = lines.size();
        for (int i = 0; i < path.length - 1; i++) {
            String parentKey = path[i];
            int keyIndent = parentIndent + 2;
            int found = findKeyLine(lines, rangeStart, rangeEnd, parentKey, keyIndent);
            if (found >= 0) {
                rangeEnd = findBlockEnd(lines, found, rangeEnd, keyIndent);
                rangeStart = found + 1;
            } else {
                lines.add(rangeEnd, repeatSpace(keyIndent) + parentKey + ":");
                rangeStart = rangeEnd + 1;
                rangeEnd = rangeStart;
            }
            parentIndent = keyIndent;
        }

        String key = path[path.length - 1];
        int keyIndent = parentIndent + 2;
        int found = findKeyLine(lines, rangeStart, rangeEnd, key, keyIndent);

        List<String> replacement = new ArrayList<>();
        if (values.isEmpty()) {
            replacement.add(repeatSpace(keyIndent) + key + ": []");
        } else {
            replacement.add(repeatSpace(keyIndent) + key + ":");
            for (String value : values) {
                replacement.add(repeatSpace(keyIndent + 2) + "- " + value);
            }
        }

        if (found >= 0) {
            String comment = extractTrailingComment(lines.get(found));
            int childEnd = findBlockEnd(lines, found, rangeEnd, keyIndent);
            lines.set(found, replacement.get(0) + comment);
            if (childEnd > found + 1) {
                lines.subList(found + 1, childEnd).clear();
            }
            if (replacement.size() > 1) {
                lines.addAll(found + 1, replacement.subList(1, replacement.size()));
            }
        } else {
            lines.addAll(rangeEnd, replacement);
        }
    }

    /**
     * 渲染单个配置项的文本, 空值时输出 "key:"
     */
    private String renderYamlScalar(String key, String value) {
        if (value == null || value.isEmpty()) {
            return key + ":";
        }
        return key + ": " + value;
    }

    /**
     * 在[rangeStart, rangeEnd)范围内查找指定缩进且键名匹配的行
     */
    private int findKeyLine(List<String> lines, int rangeStart, int rangeEnd, String key, int keyIndent) {
        String prefix = key + ":";
        for (int i = rangeStart; i < rangeEnd; i++) {
            String content = stripComment(lines.get(i));
            if (content.trim().isEmpty()) {
                continue;
            }
            int indent = countIndent(content);
            if (indent < keyIndent) {
                break;
            }
            if (indent == keyIndent && content.trim().startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 计算块内容的结束位置(不包含块尾部的空行/注释行)
     */
    private int findBlockEnd(List<String> lines, int blockLine, int rangeEnd, int blockIndent) {
        int lastContent = blockLine;
        for (int i = blockLine + 1; i < rangeEnd; i++) {
            String content = stripComment(lines.get(i));
            if (content.trim().isEmpty()) {
                continue;
            }
            if (countIndent(content) <= blockIndent) {
                break;
            }
            lastContent = i;
        }
        return lastContent + 1;
    }

    /**
     * 根据ID定位配置文件
     * <p>
     * 仅通过ID与已扫描到的配置列表匹配, 不使用外部输入拼接路径
     */
    private ConfigEntry resolveEntry(String profileId) {
        if (ObjectUtils.isEmpty(profileId)) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "配置文件ID不能为空");
        }
        ConfigSource source = resolveConfigSource();
        if (source.isNone()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),
                    "spring.config.location 指定的配置路径不可用");
        }
        ConfigEntry entry = findById(listProfileEntries(source), profileId);
        if (entry == null) {
            // 可能是主配置文件(未配置 spring.profiles 时只返回该项)
            ConfigEntry mainConfig = resolveMainConfig(source);
            if (mainConfig != null && profileId.equals(parseProfileId(mainConfig.getFileName()))) {
                entry = mainConfig;
            }
        }
        if (entry == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "配置文件不存在: " + profileId);
        }
        return entry;
    }

    // ------------------------------------------------------------------
    // 配置来源
    // ------------------------------------------------------------------

    /**
     * 配置文件来源
     */
    private static class ConfigSource {
        private boolean classpath;
        private boolean none;
        private File dir;

        static ConfigSource ofClasspath() {
            ConfigSource source = new ConfigSource();
            source.classpath = true;
            return source;
        }

        static ConfigSource ofDir(File dir) {
            if (dir == null) {
                return none();
            }
            ConfigSource source = new ConfigSource();
            source.dir = dir;
            return source;
        }

        static ConfigSource none() {
            ConfigSource source = new ConfigSource();
            source.none = true;
            return source;
        }

        boolean isClasspath() {
            return classpath;
        }

        boolean isNone() {
            return none;
        }

        File getDir() {
            return dir;
        }

        String describe() {
            if (classpath) {
                return "classpath(jar内)";
            }
            if (none) {
                return "未找到(spring.config.location 指定的路径不可用)";
            }
            return String.valueOf(dir);
        }
    }

    /**
     * 判定配置文件来源
     * <ol>
     *     <li>启动时指定了 spring.config.location / additional-location, 则使用该路径;</li>
     *     <li>未指定时, 定位实际加载的 application.yml 所在目录(默认打包会将配置文件输出到jar同级目录);</li>
     * </ol>
     */
    private ConfigSource resolveConfigSource() {
        String location = environment.getProperty(LOCATION_PROPERTY);
        String additional = environment.getProperty(ADDITIONAL_LOCATION_PROPERTY);
        log.info("[系统设置] spring.config.location={}, spring.config.additional-location={}", location, additional);
        if (ObjectUtils.isEmpty(location)) {
            location = additional;
        }
        if (!ObjectUtils.isEmpty(location)) {
            return resolveFromLocation(location);
        }
        // 未指定 config.location: 通过实际加载的 application.yml 定位
        ConfigSource fromEnvironment = resolveSourceFromEnvironment();
        if (fromEnvironment != null) {
            log.info("[系统设置] 由配置来源定位到配置: {}", fromEnvironment.describe());
            return fromEnvironment;
        }
        // 回退: 按 Spring Boot 默认搜索路径查找
        ConfigSource fromDefault = resolveSourceFromDefaultLocations();
        if (fromDefault != null) {
            log.info("[系统设置] 按默认路径找到配置: {}", fromDefault.describe());
            return fromDefault;
        }
        log.warn("[系统设置] 未找到任何配置文件(application.yml), 工作目录: {}", System.getProperty("user.dir"));
        return ConfigSource.none();
    }

    /**
     * 按 Spring Boot 默认搜索路径查找 application.yml
     */
    private ConfigSource resolveSourceFromDefaultLocations() {
        for (String candidate : DEFAULT_LOCATION_CANDIDATES) {
            ConfigSource source = tryLocation(candidate);
            if (source != null) {
                return source;
            }
        }
        return null;
    }

    /**
     * 尝试解析单个配置位置, 不包含主配置文件时返回 null
     */
    private ConfigSource tryLocation(String location) {
        String path = location.trim();
        if (path.startsWith("optional:")) {
            path = path.substring("optional:".length());
        }
        if (path.startsWith("classpath:")) {
            for (String name : mainConfigNames()) {
                if (new ClassPathResource(name).exists()) {
                    return ConfigSource.ofClasspath();
                }
            }
            return null;
        }
        File dir = toConfigDir(path);
        if (dir == null && path.contains("*")) {
            dir = resolveWildcardDir(path);
        }
        if (dir == null || !containsMainConfig(dir)) {
            return null;
        }
        return ConfigSource.ofDir(dir);
    }

    /**
     * 解析带有通配符的目录(例如 config 目录下的子目录), 返回第一个包含主配置文件的子目录
     */
    private File resolveWildcardDir(String path) {
        String prefix = path.substring(0, path.indexOf('*'));
        int lastSlash = prefix.lastIndexOf('/');
        String parentPath = lastSlash > 0 ? prefix.substring(0, lastSlash) : prefix;
        File parent = new File(parentPath.isEmpty() ? "." : parentPath);
        if (!parent.isAbsolute()) {
            parent = new File(System.getProperty("user.dir"), parentPath);
        }
        if (!parent.isDirectory()) {
            return null;
        }
        File[] children = parent.listFiles();
        if (children == null) {
            return null;
        }
        for (File child : children) {
            if (child.isDirectory() && containsMainConfig(child)) {
                return child;
            }
        }
        return null;
    }

    private boolean containsMainConfig(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        for (String name : mainConfigNames()) {
            if (new File(dir, name).isFile()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析 --spring.config.location 指定的路径
     */
    private ConfigSource resolveFromLocation(String location) {
        boolean classpathRequested = false;
        for (String item : location.split(",")) {
            String path = item.trim();
            if (path.startsWith("optional:")) {
                path = path.substring("optional:".length());
            }
            if (path.startsWith("classpath:")) {
                classpathRequested = true;
                continue;
            }
            File dir = toConfigDir(path);
            if (dir != null) {
                log.info("[系统设置] 使用 spring.config.location 指定目录: {}", dir.getAbsolutePath());
                return ConfigSource.ofDir(dir);
            }
        }
        if (classpathRequested) {
            log.info("[系统设置] spring.config.location 仅包含 classpath, 使用jar内配置");
            return ConfigSource.ofClasspath();
        }
        log.warn("[系统设置] spring.config.location 指定的路径不可用: {}", location);
        return ConfigSource.none();
    }

    /**
     * 从环境中定位实际加载的 application.yml
     * <p>
     * Spring 会将配置文件来源记录在 PropertySource 名称中, 例如:<br/>
     * Config resource 'file [/opt/wvp/application.yml] via location 'optional:file:./''<br/>
     * Config resource 'class path resource [application.yml] via location 'optional:classpath:/''
     */
    private ConfigSource resolveSourceFromEnvironment() {
        Pattern mainPattern = mainConfigNamePattern();
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            String name = propertySource.getName();
            if (ObjectUtils.isEmpty(name) || !mainPattern.matcher(name).find()) {
                continue;
            }
            // jar 内部的配置文件(开发环境)
            if (name.contains("class path resource")) {
                return ConfigSource.ofClasspath();
            }
            // jar 之外的配置文件
            Matcher fileMatcher = FILE_ORIGIN_PATTERN.matcher(name);
            if (fileMatcher.find()) {
                File dir = toExistingDir(fileMatcher.group(1));
                if (dir != null) {
                    return ConfigSource.ofDir(dir);
                }
            }
        }
        log.info("[系统设置] 未能从配置来源中定位 application.yml, 已检查 {} 个属性源",
                environment.getPropertySources().size());
        return null;
    }

    /**
     * 从来源名称中解析出文件
     * <p>
     * 注意来源中可能是相对路径(如 application.yml 或 ./application.yml), 需要基于工作目录解析
     */
    private File toExistingFile(String path) {
        if (ObjectUtils.isEmpty(path)) {
            return null;
        }
        File file = new File(path.trim());
        if (!file.isAbsolute()) {
            String workingDir = System.getProperty("user.dir");
            if (ObjectUtils.isEmpty(workingDir)) {
                return null;
            }
            file = new File(workingDir, path.trim());
        }
        return file.isFile() ? file : null;
    }

    /**
     * 解析来源中文件所在的目录
     */
    private File toExistingDir(String path) {
        File file = toExistingFile(path);
        if (file == null) {
            return null;
        }
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            log.warn("[系统设置] 解析配置路径失败: {} ({})", path, e.getMessage());
            return null;
        }
        File parent = file.getParentFile();
        return parent != null && parent.isDirectory() ? parent : null;
    }

    /**
     * 主配置文件名称, 受 spring.config.name 影响
     */
    private String[] mainConfigNames() {
        String configName = environment.getProperty("spring.config.name");
        if (ObjectUtils.isEmpty(configName)) {
            return MAIN_CONFIG_NAMES;
        }
        return new String[]{configName + ".yml", configName + ".yaml"};
    }

    /**
     * 主配置文件名称匹配(不匹配 application-xxx.yml)
     */
    private Pattern mainConfigNamePattern() {
        String configName = environment.getProperty("spring.config.name");
        String base = ObjectUtils.isEmpty(configName) ? "application" : configName;
        return Pattern.compile(Pattern.quote(base) + "\\.ya?ml", Pattern.CASE_INSENSITIVE);
    }

    /**
     * 将 spring.config.location 中的一项转换为目录
     * <p>
     * 支持 file:/xxx/config/ 、/xxx/config/ 等写法
     */
    private File toConfigDir(String location) {
        if (ObjectUtils.isEmpty(location)) {
            return null;
        }
        String path = location.trim();
        if (path.startsWith("optional:")) {
            path = path.substring("optional:".length());
        }
        if (path.startsWith("classpath:")) {
            return null;
        }
        if (path.startsWith("file:")) {
            path = path.substring("file:".length());
        }
        if (ObjectUtils.isEmpty(path)) {
            return null;
        }
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.dir"), path);
        }
        // 允许直接指定单个配置文件
        File dir = file.isFile() ? file.getParentFile() : file;
        if (dir == null || !dir.isDirectory()) {
            log.warn("[系统设置] 配置目录不存在: {}", location);
            return null;
        }
        return dir;
    }

    // ------------------------------------------------------------------
    // 配置文件扫描
    // ------------------------------------------------------------------

    /**
     * 扫描所有可切换的配置文件 application-{id}.yml
     */
    private List<ConfigEntry> listProfileEntries(ConfigSource source) {
        List<ConfigEntry> entries = source.isClasspath()
                ? listClasspathEntries()
                : listDirEntries(source.getDir());
        entries.sort((a, b) -> a.getFileName().compareTo(b.getFileName()));
        return entries;
    }

    private List<ConfigEntry> listDirEntries(File dir) {
        List<ConfigEntry> result = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) {
            return result;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return result;
        }
        for (File file : files) {
            if (!file.isFile() || !PROFILE_FILE_PATTERN.matcher(file.getName()).matches()) {
                continue;
            }
            ConfigEntry entry = new ConfigEntry();
            entry.setId(parseProfileId(file.getName()));
            entry.setFileName(file.getName());
            entry.setPath(file.getAbsolutePath());
            entry.setFile(file);
            entry.setContent(readFileBytes(file));
            entry.setClasspathResource(false);
            result.add(entry);
        }
        return result;
    }

    private List<ConfigEntry> listClasspathEntries() {
        List<ConfigEntry> result = new ArrayList<>();
        Set<String> addedNames = new LinkedHashSet<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String pattern : CLASSPATH_PATTERNS) {
            Resource[] resources;
            try {
                resources = resolver.getResources(pattern);
            } catch (IOException e) {
                log.warn("[系统设置] 扫描classpath配置失败: {} ({})", pattern, e.getMessage());
                continue;
            }
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (ObjectUtils.isEmpty(fileName) || !PROFILE_FILE_PATTERN.matcher(fileName).matches()) {
                    continue;
                }
                if (!addedNames.add(fileName)) {
                    continue;
                }
                ConfigEntry entry = new ConfigEntry();
                entry.setId(parseProfileId(fileName));
                entry.setFileName(fileName);
                entry.setClasspathResource(true);
                entry.setFile(toFile(resource));
                entry.setPath(entry.getFile() != null ? entry.getFile().getAbsolutePath() : toUrl(resource));
                entry.setContent(readResourceBytes(resource));
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * 定位主配置文件 application.yml
     */
    private ConfigEntry resolveMainConfig(ConfigSource source) {
        if (!source.isClasspath()) {
            for (String name : mainConfigNames()) {
                File file = new File(source.getDir(), name);
                if (file.isFile()) {
                    return buildMainConfigEntry(file, name, false);
                }
            }
            return null;
        }
        for (String name : mainConfigNames()) {
            ClassPathResource resource = new ClassPathResource(name);
            if (resource.exists()) {
                return buildMainConfigEntry(toFile(resource), name, true);
            }
        }
        return null;
    }

    private ConfigEntry buildMainConfigEntry(File file, String fileName, boolean classpath) {
        ConfigEntry entry = new ConfigEntry();
        entry.setFileName(fileName);
        entry.setPath(file != null ? file.getAbsolutePath() : "classpath:" + fileName);
        entry.setFile(file);
        entry.setClasspathResource(classpath);
        entry.setContent(file != null ? readFileBytes(file) : new byte[0]);
        entry.setId(null);
        return entry;
    }

    // ------------------------------------------------------------------
    // 当前生效的profile
    // ------------------------------------------------------------------

    /**
     * 解析当前生效的profile ID
     * <p>
     * 优先取Spring环境中的 spring.profiles.active, 取不到时从主配置文件中读取
     */
    private String resolveActiveProfileId(ConfigEntry mainConfig) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles != null && activeProfiles.length > 0 && !ObjectUtils.isEmpty(activeProfiles[0])) {
            return activeProfiles[0];
        }
        String property = environment.getProperty("spring.profiles.active");
        if (!ObjectUtils.isEmpty(property)) {
            for (String item : property.split(",")) {
                if (!item.trim().isEmpty()) {
                    return item.trim();
                }
            }
        }
        // 回退: 直接读取主配置文件中的 spring.profiles.active
        if (mainConfig != null) {
            return readProfilesActive(mainConfig.getContent());
        }
        return null;
    }

    /**
     * 判断配置内容中是否存在 spring.profiles 配置
     */
    @SuppressWarnings("unchecked")
    private boolean hasProfilesConfig(byte[] content) {
        if (content == null || content.length == 0) {
            return false;
        }
        Object profiles = extractProfilesNode(content);
        return profiles instanceof Map && !((Map<?, ?>) profiles).isEmpty();
    }

    /**
     * 读取 spring.profiles.active 的值
     */
    @SuppressWarnings("unchecked")
    private String readProfilesActive(byte[] content) {
        Object profiles = extractProfilesNode(content);
        if (!(profiles instanceof Map)) {
            return null;
        }
        Object active = ((Map<String, Object>) profiles).get("active");
        if (active instanceof String) {
            String value = ((String) active).trim();
            if (value.isEmpty()) {
                return null;
            }
            int comma = value.indexOf(',');
            return comma > 0 ? value.substring(0, comma).trim() : value;
        }
        if (active instanceof Collection) {
            for (Object item : (Collection<Object>) active) {
                if (item != null && !item.toString().trim().isEmpty()) {
                    return item.toString().trim();
                }
            }
        }
        return null;
    }

    /**
     * 取出 spring.profiles 节点
     */
    @SuppressWarnings("unchecked")
    private Object extractProfilesNode(byte[] content) {
        Map<String, Object> root = loadYaml(content);
        if (root == null) {
            return null;
        }
        Object spring = root.get("spring");
        if (!(spring instanceof Map)) {
            return null;
        }
        return ((Map<String, Object>) spring).get("profiles");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        try {
            Object loaded = new Yaml().load(new String(content, StandardCharsets.UTF_8));
            if (loaded instanceof Map) {
                return (Map<String, Object>) loaded;
            }
        } catch (Exception e) {
            log.warn("[系统设置] 解析配置文件失败: {}", e.getMessage());
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 写入 spring.profiles.active
    // ------------------------------------------------------------------

    /**
     * 修改主配置文件中的 spring.profiles.active, 保留原有注释与格式
     */
    private void writeActiveProfile(ConfigEntry mainConfig, String profileId) {
        List<String> lines;
        if (mainConfig.getFile() != null) {
            try {
                lines = Files.readAllLines(mainConfig.getFile().toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "读取配置文件失败: " + e.getMessage());
            }
        } else {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "配置文件不可写");
        }

        int springIndex = -1;
        int profilesIndex = -1;
        int activeIndex = -1;
        int springIndent = -1;
        int profilesIndent = -1;
        for (int i = 0; i < lines.size(); i++) {
            String content = stripComment(lines.get(i));
            if (content.trim().isEmpty()) {
                continue;
            }
            int indent = countIndent(content);
            String trimmed = content.trim();
            if (springIndex < 0) {
                if ("spring:".equals(trimmed) && indent == 0) {
                    springIndex = i;
                    springIndent = indent;
                }
                continue;
            }
            // 已离开 spring 作用域
            if (indent <= springIndent) {
                break;
            }
            if (profilesIndex < 0) {
                if ("profiles:".equals(trimmed)) {
                    profilesIndex = i;
                    profilesIndent = indent;
                }
                continue;
            }
            if (indent <= profilesIndent) {
                break;
            }
            if (trimmed.startsWith("active:")) {
                activeIndex = i;
                break;
            }
        }

        if (activeIndex >= 0) {
            String indentText = repeatSpace(countIndent(stripComment(lines.get(activeIndex))));
            String comment = extractTrailingComment(lines.get(activeIndex));
            lines.set(activeIndex, indentText + "active: " + profileId + comment);
        } else if (profilesIndex >= 0) {
            lines.add(profilesIndex + 1, repeatSpace(profilesIndent + 2) + "active: " + profileId);
        } else if (springIndex >= 0) {
            String indentText = repeatSpace(springIndent + 2);
            lines.add(springIndex + 1, indentText + "profiles:");
            lines.add(springIndex + 2, indentText + "  active: " + profileId);
        } else {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "配置文件中未找到 spring 配置项");
        }

        try {
            Files.write(mainConfig.getFile().toPath(), lines, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "写入配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 去掉行内注释(引号内的 # 不视为注释)
     */
    private String stripComment(String line) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '#' && !inSingleQuote && !inDoubleQuote) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private String extractTrailingComment(String line) {
        String stripped = stripComment(line);
        if (stripped.length() < line.length()) {
            return " " + line.substring(stripped.length()).trim();
        }
        return "";
    }

    private int countIndent(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private String repeatSpace(int count) {
        return " ".repeat(Math.max(count, 0));
    }

    // ------------------------------------------------------------------
    // 工具方法
    // ------------------------------------------------------------------

    private ProfileInfo toProfileInfo(ConfigEntry entry, boolean active, boolean writable) {
        String createTime = null;
        String updateTime = null;
        if (entry.getFile() != null && entry.getFile().isFile()) {
            try {
                BasicFileAttributes attributes = Files.readAttributes(entry.getFile().toPath(),
                        BasicFileAttributes.class);
                createTime = formatTime(attributes.creationTime().toInstant());
                updateTime = formatTime(attributes.lastModifiedTime().toInstant());
            } catch (IOException e) {
                log.warn("[系统设置] 读取文件属性失败: {}", entry.getFileName());
            }
        }
        String id = entry.getId() != null ? entry.getId() : parseProfileId(entry.getFileName());
        ProfileInfo info = new ProfileInfo(id, entry.getFileName(), active, createTime, updateTime, entry.getPath());
        info.setWritable(writable);
        return info;
    }

    private ConfigEntry findById(List<ConfigEntry> entries, String id) {
        if (ObjectUtils.isEmpty(id)) {
            return null;
        }
        for (ConfigEntry entry : entries) {
            if (id.equals(entry.getId())) {
                return entry;
            }
        }
        return null;
    }

    /**
     * application-xxx.yml -> xxx
     */
    private String parseProfileId(String fileName) {
        Matcher matcher = PROFILE_FILE_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private String formatTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
    }

    private byte[] readFileBytes(File file) {
        if (file == null || !file.isFile()) {
            return new byte[0];
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            log.warn("[系统设置] 读取配置文件失败: {} ({})", file.getName(), e.getMessage());
            return new byte[0];
        }
    }

    private byte[] readResourceBytes(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            log.warn("[系统设置] 读取配置文件失败: {} ({})", resource.getFilename(), e.getMessage());
            return new byte[0];
        }
    }

    /**
     * classpath 资源位于jar内时无法解析为文件, 此时返回 null(不可写)
     */
    private File toFile(Resource resource) {
        try {
            return resource.getFile();
        } catch (IOException e) {
            return null;
        }
    }

    private String toUrl(Resource resource) {
        try {
            return resource.getURL().toString();
        } catch (IOException e) {
            return "classpath:" + resource.getFilename();
        }
    }

    /**
     * 预留: 读取配置文件内容
     */
    protected Map<String, Object> loadConfig(ConfigEntry entry) {
        Map<String, Object> loaded = loadYaml(entry.getContent());
        return loaded != null ? loaded : new LinkedHashMap<>();
    }
}
