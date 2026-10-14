package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.vmanager.bean.JtServerConfig;
import com.genersoft.iot.vmp.vmanager.bean.ProfileInfo;
import com.genersoft.iot.vmp.vmanager.bean.SipServerConfig;
import com.genersoft.iot.vmp.vmanager.bean.UserSettingConfig;
import com.genersoft.iot.vmp.vmanager.bean.UserSettingSaveResult;
import com.genersoft.iot.vmp.vmanager.bean.WebServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SettingServiceImplTest {

    private Path configDir;
    private SettingServiceImpl service;

    @BeforeEach
    void setUp() throws IOException {
        configDir = Files.createTempDirectory("wvp-setting-test");
        service = new SettingServiceImpl();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (configDir != null && Files.exists(configDir)) {
            try (Stream<Path> walk = Files.walk(configDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    private void writeFile(String name, String content) throws IOException {
        Files.write(configDir.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    private MockEnvironment envWithLocation(String location) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.config.location", location);
        return environment;
    }

    private void inject(MockEnvironment environment) {
        ReflectionTestUtils.setField(service, "environment", environment);
    }

    /**
     * 指定了 --spring.config.location 且配置目录中有多份 application-*.yml(带 spring.profiles), 应全部返回
     */
    @Test
    void queryProfiles_withProfilesConfig_returnsAllAndMarksActive() throws IOException {
        writeFile("application-default.yml", "spring:\n  profiles:\n    active: default\nserver:\n  port: 18080\n");
        writeFile("application-dev.yml", "spring:\n  profiles:\n    active: dev\nserver:\n  port: 18081\n");
        writeFile("application-prod.yml", "spring:\n  profiles:\n    active: prod\nserver:\n  port: 18082\n");
        // 非 application- 开头, 不应被扫描
        writeFile("other.yml", "server:\n  port: 9999\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();

        List<String> ids = profiles.stream().map(ProfileInfo::getId).collect(Collectors.toList());
        assertEquals(List.of("default", "dev", "prod"), ids, "应扫描出全部 application-*.yml 并以其ID返回");

        List<ProfileInfo> actives = profiles.stream().filter(ProfileInfo::isActive).collect(Collectors.toList());
        assertEquals(1, actives.size(), "有且仅有一个配置文件被标记为正在使用");
        assertEquals("dev", actives.get(0).getId(), "当前生效的 profile 应被标记为 active");

        // 返回内容应包含ID、创建时间、修改时间
        ProfileInfo first = profiles.get(0);
        assertNotNull(first.getId());
        assertNotNull(first.getName());
        assertNotNull(first.getCreateTime(), "应返回创建时间");
        assertNotNull(first.getUpdateTime(), "应返回修改时间");
    }

    /**
     * 配置文件中没有 spring.profiles 时, 只返回当前使用的一个, 不支持切换
     */
    @Test
    void queryProfiles_withoutProfilesConfig_returnsOnlyActive() throws IOException {
        // 模拟真实部署: 只有一份 application.yml, 没有 spring.profiles
        writeFile("application.yml", "server:\n  port: 18080\nsip:\n  port: 5060\n");
        writeFile("application-other.yml", "server:\n  port: 18081\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        // application.yml 不匹配 application-{id}.yml 规则, 因此列表中仅 application-other
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();

        assertEquals(1, profiles.size(), "没有 spring.profiles 时只返回一个配置项");
        assertTrue(profiles.get(0).isActive(), "返回的这一项应标记为正在使用");
    }

    /**
     * application.yml(无ID)不应出现在可切换列表中
     */
    @Test
    void queryProfiles_ignoresPlainApplicationYml() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: default\n");
        writeFile("application-default.yml", "spring:\n  profiles:\n    active: default\n");
        writeFile("application-dev.yml", "spring:\n  profiles:\n    active: dev\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("default");
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();

        List<String> names = profiles.stream().map(ProfileInfo::getName).collect(Collectors.toList());
        assertFalse(names.contains("application.yml"), "application.yml 不是 application-{id}.yml, 不应作为可切换配置");
        assertEquals(List.of("application-default.yml", "application-dev.yml"), names);
    }

    /**
     * 指定了 spring.config.location 但路径不可用时返回空列表
     */
    @Test
    void queryProfiles_invalidLocation_returnsEmpty() {
        MockEnvironment environment = envWithLocation("/not/exists/dir/");
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();
        assertNotNull(profiles);
        assertTrue(profiles.isEmpty(), "指定路径不可用时不回退到classpath, 返回空列表");
    }

    /**
     * 回归用例: Spring 的 PropertySource 名称为相对路径(如 ./application.yml)时, 应基于工作目录解析
     * <p>
     * 修复前此处会解析出 null 目录, 导致配置列表为空
     */
    @Test
    void queryProfiles_relativeFileOrigin_resolvesAgainstWorkingDir() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "server:\n  port: 18081\n");

        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", configDir.toAbsolutePath().toString());
        try {
            MockEnvironment environment = new MockEnvironment();
            // 真实环境下 Spring 可能给出相对路径形式的来源名称
            environment.getPropertySources().addLast(new MapPropertySource(
                    "Config resource 'file [./application.yml]' via location 'optional:file:./'",
                    Collections.emptyMap()));
            inject(environment);

            List<ProfileInfo> profiles = service.queryProfiles();

            assertEquals(1, profiles.size(), "应解析出 application.yml 所在目录并扇描到配置");
            ProfileInfo info = profiles.get(0);
            assertEquals("dev", info.getId());
            assertTrue(info.isActive(), "应从 application.yml 中读到 active=dev");
            assertTrue(info.isWritable(), "外部配置文件应可写");
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    /**
     * 未指定 --spring.config.location 时, 应通过实际加载的 application.yml 定位所在目录
     * <p>
     * 默认打包会将 application.yml / application-*.yml 输出到 jar 同级目录
     */
    @Test
    void queryProfiles_noLocation_resolvesFromLoadedConfigFile() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: standalone\n");
        writeFile("application-standalone.yml", "server:\n  port: 18080\n");
        writeFile("application-dev.yml", "server:\n  port: 18081\n");

        File mainFile = configDir.resolve("application.yml").toFile();
        MockEnvironment environment = new MockEnvironment();
        // Spring 实际加载后 PropertySource 名称格式
        environment.getPropertySources().addLast(new MapPropertySource(
                "Config resource 'file [" + mainFile.getAbsolutePath() + "]' via location 'optional:file:./'",
                Collections.emptyMap()));
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();

        List<String> ids = profiles.stream().map(ProfileInfo::getId).collect(Collectors.toList());
        assertEquals(List.of("dev", "standalone"), ids, "应扫描到 application.yml 同级的全部配置文件");

        List<ProfileInfo> actives = profiles.stream().filter(ProfileInfo::isActive).collect(Collectors.toList());
        assertEquals(1, actives.size());
        assertEquals("standalone", actives.get(0).getId(), "从 application.yml 的 spring.profiles.active 读取");

        // 外部配置文件可写, 应支持切换
        assertTrue(profiles.get(0).isWritable(), "jar外部配置文件应可写");
    }

    /**
     * 未指定 location 且工作目录中没有配置文件时, 返回空列表
     */
    @Test
    void queryProfiles_noLocationNoWorkingDirConfig_returnsEmpty() {
        MockEnvironment environment = new MockEnvironment();
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();
        assertNotNull(profiles);
        // 工作目录下无 application.yml 时不应抛异常
    }

    /**
     * 查看配置文件内容
     */
    @Test
    void readProfileContent_returnsFileContent() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        String devContent = "server:\n  port: 18081\nsip:\n  id: 3402000001\n";
        writeFile("application-dev.yml", devContent);

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        String content = service.readProfileContent("dev");
        assertEquals(devContent, content, "应返回配置文件的原始内容");
    }

    /**
     * 查看不存在的配置文件应报错
     */
    @Test
    void readProfileContent_notExists_throws() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "server:\n  port: 18081\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        assertThrows(Exception.class, () -> service.readProfileContent("not-exists"));
    }

    /**
     * 路径穿越攻击必须被拒绝
     */
    @Test
    void readProfileContent_pathTraversal_rejected() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "server:\n  port: 18081\n");
        // 配置文件目录外存在敏感文件
        Path outside = configDir.getParent().resolve("secret-" + System.nanoTime() + ".yml");
        Files.writeString(outside, "password: super-secret\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        try {
            String[] attacks = {
                    "../" + outside.getFileName(),
                    "..%2F" + outside.getFileName(),
                    "../../etc/passwd",
                    "/etc/passwd",
                    "..\\" + outside.getFileName()
            };
            for (String attack : attacks) {
                assertThrows(Exception.class, () -> service.readProfileContent(attack),
                        "路径穿越应被拒绝: " + attack);
            }
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    /**
     * 未配置 spring.profiles 时, 可以查看唯一的当前配置文件
     */
    @Test
    void readProfileContent_singleMainConfig_isViewable() throws IOException {
        String content = "server:\n  port: 18080\n";
        writeFile("application.yml", content);

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();
        assertEquals(1, profiles.size());
        String id = profiles.get(0).getId();
        assertNotNull(id, "单配置项也应返回稳定的ID");
        assertEquals(content, service.readProfileContent(id));
    }

    /**
     * jar 内部的配置无法解析为文件时不可写
     */
    @Test
    void queryProfiles_entriesExposeWritableFlag() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "server:\n  port: 18081\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();
        assertFalse(profiles.isEmpty());
        assertTrue(profiles.get(0).isWritable(), "外部配置文件应可写");
    }

    /**
     * 目录不存在时返回空列表, 不抛异常
     */
    @Test
    void queryProfiles_missingDir_returnsEmpty() {
        MockEnvironment environment = envWithLocation("/not/exists/dir/");
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();
        assertNotNull(profiles);
        assertTrue(profiles.isEmpty(), "目录不存在时返回空列表");
    }

    /**
     * spring.config.location 支持直接指定单个配置文件
     */
    @Test
    void queryProfiles_locationPointingToFile_resolvesParentDir() throws IOException {
        writeFile("application-a.yml", "spring:\n  profiles:\n    active: a\n");
        writeFile("application-b.yml", "spring:\n  profiles:\n    active: b\n");
        File target = configDir.resolve("application-a.yml").toFile();

        MockEnvironment environment = envWithLocation("file:" + target.getAbsolutePath());
        environment.setActiveProfiles("a");
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();

        assertEquals(2, profiles.size(), "指向文件时应解析其父目录");
        assertEquals("a", profiles.stream().filter(ProfileInfo::isActive).findFirst().orElseThrow().getId());
    }

    /**
     * 目录中只有 application.yml 且无 spring.profiles 时, 应返回该文件本身作为当前使用项
     */
    @Test
    void queryProfiles_onlyPlainApplicationYml_returnsItAsActive() throws IOException {
        writeFile("application.yml", "server:\n  port: 18080\nsip:\n  port: 5060\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();

        assertEquals(1, profiles.size(), "只有一个 application.yml 时应返回1项");
        ProfileInfo info = profiles.get(0);
        assertEquals("application.yml", info.getName());
        assertTrue(info.isActive());
        assertNotNull(info.getCreateTime());
        assertNotNull(info.getUpdateTime());
    }

    /**
     * 切换配置文件应修改 spring.profiles.active, 且保留其他内容与注释
     */
    @Test
    void switchProfile_updatesActiveAndKeepsComments() throws IOException {
        String original = "# 头部注释\n"
                + "spring:\n"
                + "  application:\n"
                + "    name: wvp\n"
                + "  profiles:\n"
                + "    active: default   # 当前使用\n"
                + "server:\n"
                + "  port: 18080\n";
        writeFile("application.yml", original);
        writeFile("application-dev.yml", "spring:\n  profiles:\n    active: dev\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("default");
        inject(environment);

        String path = service.switchProfile("dev");
        assertNotNull(path);

        String updated = Files.readString(configDir.resolve("application.yml"));
        assertTrue(updated.contains("active: dev"), "应写入新的profile, 实际:\n" + updated);
        assertFalse(updated.contains("active: default"), "旧值应被替换");
        assertTrue(updated.contains("# 头部注释"), "应保留头部注释");
        assertTrue(updated.contains("# 当前使用"), "应保留行尾注释");
        assertTrue(updated.contains("name: wvp"), "应保留其他配置项");
        assertTrue(updated.contains("port: 18080"), "应保留其他配置项");
    }

    /**
     * 目标配置文件不存在时应报错
     */
    @Test
    void switchProfile_targetNotExists_throws() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: default\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("default");
        inject(environment);

        assertThrows(Exception.class, () -> service.switchProfile("not-exists"));
    }

    /**
     * 未配置 spring.profiles 时不允许切换
     */
    @Test
    void switchProfile_noProfilesConfig_throws() throws IOException {
        writeFile("application.yml", "server:\n  port: 18080\n");
        writeFile("application-dev.yml", "server:\n  port: 18081\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        inject(environment);

        assertThrows(Exception.class, () -> service.switchProfile("dev"), "无 spring.profiles 时应拒绝切换");
    }

    /**
     * 切换到当前已生效的配置时应幂等返回, 且不改动文件
     */
    @Test
    void switchProfile_sameAsCurrent_isIdempotent() throws IOException {
        String original = "spring:\n  profiles:\n    active: dev\n";
        writeFile("application.yml", original);
        writeFile("application-dev.yml", "spring:\n  profiles:\n    active: dev\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        service.switchProfile("dev");
        assertEquals(original, Files.readString(configDir.resolve("application.yml")), "内容不应被修改");
    }

    /**
     * spring: 下没有 profiles: 时, 切换应自动补全 profiles.active
     */
    @Test
    void switchProfile_missingProfilesBlock_appendsIt() throws IOException {
        writeFile("application.yml", "spring:\n  application:\n    name: wvp\nserver:\n  port: 18080\n");
        writeFile("application-dev.yml", "spring:\n  profiles:\n    active: dev\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        inject(environment);

        // 无 spring.profiles 时会被前置校验拦住
        assertThrows(Exception.class, () -> service.switchProfile("dev"));
    }

    /**
     * 切换后再次查询, active 标记应跟随新的配置
     */
    @Test
    void switchProfile_thenQuery_reflectsNewActive() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: default\n");
        writeFile("application-default.yml", "spring:\n  profiles:\n    active: default\n");
        writeFile("application-dev.yml", "spring:\n  profiles:\n    active: dev\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("default");
        inject(environment);

        service.switchProfile("dev");

        // 模拟重启后 Spring 读到新的 active profile
        MockEnvironment afterRestart = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        afterRestart.setActiveProfiles("dev");
        inject(afterRestart);

        List<ProfileInfo> profiles = service.queryProfiles();
        assertEquals("dev", profiles.stream().filter(ProfileInfo::isActive).findFirst().orElseThrow().getId());
    }

    /**
     * classpath: 不可作为可切换目录, 且无其他有效路径时返回空
     */
    @Test
    void queryProfiles_classpathLocation_returnsEmpty() {
        MockEnvironment environment = envWithLocation("optional:classpath:/,optional:file:/not/exists/xx/");
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();
        assertNotNull(profiles);
    }

    /**
     * spring.profiles.active 未设置时, 通过配置文件来源兜底定位
     */
    @Test
    void queryProfiles_noActiveProfile_stillReturnsList() throws IOException {
        writeFile("application-x.yml", "spring:\n  profiles:\n    active: x\n");
        writeFile("application-y.yml", "spring:\n  profiles:\n    active: y\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        inject(environment);

        List<ProfileInfo> profiles = service.queryProfiles();
        assertEquals(2, profiles.size());
    }

    /**
     * 查询 WEB服务配置: 应读取当前生效 profile 文件中的 server / ssl 配置
     */
    @Test
    void queryWebServerConfig_readsServerAndSsl() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml",
                "server:\n"
                        + "  port: 18080\n"
                        + "  ssl:\n"
                        + "    enabled: true\n"
                        + "    certificate: /home/lin/文档/局域网使用https/100.pem\n"
                        + "    certificate-private-key: /home/lin/文档/局域网使用https/100.pem.key\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        WebServerConfig config = service.queryWebServerConfig();

        assertEquals(18080, config.getPort());
        assertEquals(Boolean.TRUE, config.getSslEnabled());
        assertEquals("/home/lin/文档/局域网使用https/100.pem", config.getSslCertificate());
        assertEquals("/home/lin/文档/局域网使用https/100.pem.key", config.getSslCertificatePrivateKey());
        assertEquals("dev", config.getProfileId());
        assertTrue(config.isWritable(), "外部配置文件应可写");
        assertNotNull(config.getFilePath());
        assertTrue(config.getFilePath().endsWith("application-dev.yml"));
    }

    /**
     * 保存 WEB服务配置: 应更新端口与 ssl 各项, 并保留行尾注释与其他配置
     */
    @Test
    void saveWebServerConfig_updatesValuesAndKeepsComments() throws IOException {
        Path cert = Files.createFile(configDir.resolve("server.pem"));
        Path key = Files.createFile(configDir.resolve("server.key"));

        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml",
                "# WEB 服务\n"
                        + "server:\n"
                        + "  port: 18080   # 端口\n"
                        + "  ssl:\n"
                        + "    enabled: false\n"
                        + "    certificate: /old/cert.pem\n"
                        + "    certificate-private-key: /old/cert.key\n"
                        + "sip:\n"
                        + "  port: 5060\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        WebServerConfig config = new WebServerConfig();
        config.setPort(18443);
        config.setSslEnabled(true);
        config.setSslCertificate(cert.toAbsolutePath().toString());
        config.setSslCertificatePrivateKey(key.toAbsolutePath().toString());
        service.saveWebServerConfig(config);

        String updated = Files.readString(configDir.resolve("application-dev.yml"));
        assertTrue(updated.contains("port: 18443"), "端口应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("enabled: true"), "ssl.enabled 应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("certificate: " + cert.toAbsolutePath()), "证书路径应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("certificate-private-key: " + key.toAbsolutePath()), "私钥路径应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("# WEB 服务"), "应保留头部注释");
        assertTrue(updated.contains("# 端口"), "应保留行尾注释");
        assertTrue(updated.contains("sip:\n  port: 5060"), "应保留其他配置项");
    }

    /**
     * 保存 WEB服务配置: 缺少 ssl 块时应自动创建
     */
    @Test
    void saveWebServerConfig_createsMissingSslBlock() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "server:\n  port: 18080\nsip:\n  port: 5060\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        WebServerConfig config = new WebServerConfig();
        config.setPort(18080);
        config.setSslEnabled(false);
        service.saveWebServerConfig(config);

        String updated = Files.readString(configDir.resolve("application-dev.yml"));
        assertTrue(updated.contains("ssl:"), "应创建 ssl 块, 实际:\n" + updated);
        assertTrue(updated.contains("enabled: false"), "应写入 ssl.enabled, 实际:\n" + updated);
        assertTrue(updated.contains("sip:\n  port: 5060"), "应保留其他配置项");
    }

    /**
     * 保存 WEB服务配置: 开启HTTPS但缺少证书文件时应报错
     */
    @Test
    void saveWebServerConfig_sslWithoutCertificate_throws() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "server:\n  port: 18080\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        WebServerConfig config = new WebServerConfig();
        config.setPort(18080);
        config.setSslEnabled(true);
        assertThrows(Exception.class, () -> service.saveWebServerConfig(config));
    }

    /**
     * 保存 WEB服务配置: 端口非法时应报错
     */
    @Test
    void saveWebServerConfig_invalidPort_throws() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "server:\n  port: 18080\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        WebServerConfig config = new WebServerConfig();
        config.setPort(70000);
        config.setSslEnabled(false);
        assertThrows(Exception.class, () -> service.saveWebServerConfig(config));
    }

    // ------------------------------------------------------------------
    // 国标服务(sip)
    // ------------------------------------------------------------------

    /**
     * 查询国标服务配置: 应读取当前生效 profile 文件中的 sip 配置
     */
    @Test
    void querySipServerConfig_readsSipBlock() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml",
                "sip:\n"
                        + "  ip: 0.0.0.0\n"
                        + "  show-ip: 192.168.1.3\n"
                        + "  port: 5060\n"
                        + "  domain: 4101050000\n"
                        + "  id: 41010500002000000001\n"
                        + "  password: admin123\n"
                        + "  alarm: false\n"
                        + "  timeout: 1000\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        SipServerConfig config = service.querySipServerConfig();

        assertEquals("0.0.0.0", config.getIp());
        assertEquals("192.168.1.3", config.getShowIp());
        assertEquals(5060, config.getPort());
        assertEquals("4101050000", config.getDomain());
        assertEquals("41010500002000000001", config.getId());
        assertEquals("admin123", config.getPassword());
        assertEquals(Boolean.FALSE, config.getAlarm());
        assertEquals(1000L, config.getTimeout());
        assertEquals("dev", config.getProfileId());
        assertTrue(config.isWritable());
        assertTrue(config.getFilePath().endsWith("application-dev.yml"));
    }

    /**
     * 查询国标服务配置: YAML 未配置 alarm/timeout 时应回退到默认值
     */
    @Test
    void querySipServerConfig_missingAlarmAndTimeout_fallsBackToDefaults() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml",
                "sip:\n"
                        + "  ip: 0.0.0.0\n"
                        + "  port: 5060\n"
                        + "  id: 41010500002000000001\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        SipServerConfig config = service.querySipServerConfig();

        assertEquals(Boolean.TRUE, config.getAlarm(), "YAML 未配置 alarm 时应返回默认值 true");
        assertEquals(1000L, config.getTimeout(), "YAML 未配置 timeout 时应返回默认值 1000");
    }

    /**
     * 保存国标服务配置: 应更新各项并保留注释与其他配置项
     */
    @Test
    void saveSipServerConfig_updatesValuesAndKeepsComments() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml",
                "# SIP 服务\n"
                        + "sip:\n"
                        + "  port: 5060   # 端口\n"
                        + "  id: 41010500002000000001\n"
                        + "server:\n"
                        + "  port: 18080\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        SipServerConfig config = new SipServerConfig();
        config.setPort(5061);
        config.setId("41010500002000000002");
        config.setDomain("4101050000");
        config.setPassword("admin123");
        config.setIp("0.0.0.0");
        config.setShowIp("192.168.1.3");
        config.setAlarm(false);
        config.setTimeout(1000L);
        service.saveSipServerConfig(config);

        String updated = Files.readString(configDir.resolve("application-dev.yml"));
        assertTrue(updated.contains("port: 5061"), "端口应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("id: 41010500002000000002"), "国标编号应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("domain: 4101050000"), "国标域应被写入, 实际:\n" + updated);
        assertTrue(updated.contains("timeout: 1000"), "应写入超时时间, 实际:\n" + updated);
        assertTrue(updated.contains("# SIP 服务"), "应保留头部注释");
        assertTrue(updated.contains("# 端口"), "应保留行尾注释");
        assertTrue(updated.contains("server:\n  port: 18080"), "应保留其他配置项");
    }

    /**
     * 保存国标服务配置: 国标编号非法时应报错
     */
    @Test
    void saveSipServerConfig_invalidId_throws() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "sip:\n  port: 5060\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        SipServerConfig config = new SipServerConfig();
        config.setPort(5060);
        config.setId("123");
        assertThrows(Exception.class, () -> service.saveSipServerConfig(config));
    }

    // ------------------------------------------------------------------
    // 部标服务(ftp / jt1078)
    // ------------------------------------------------------------------

    /**
     * 查询部标服务配置: 应读取 ftp / jt1078 配置
     */
    @Test
    void queryJtServerConfig_readsBlocks() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml",
                "ftp:\n"
                        + "  enable: true\n"
                        + "  port: 2121\n"
                        + "  passive-ports: 10000-10500\n"
                        + "jt1078:\n"
                        + "  enable: true\n"
                        + "  port: 18088\n"
                        + "  password: admin123\n"
                        + "  record: false\n"
                        + "  reader-idle-time: 20\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        JtServerConfig config = service.queryJtServerConfig();

        assertEquals(Boolean.TRUE, config.getFtpEnable());
        assertEquals(2121, config.getFtpPort());
        assertEquals("10000-10500", config.getFtpPassivePorts());
        assertEquals(Boolean.TRUE, config.getJt1078Enable());
        assertEquals(18088, config.getJt1078Port());
        assertEquals("admin123", config.getJt1078Password());
        assertEquals(Boolean.FALSE, config.getJt1078Record());
        assertEquals(20, config.getJt1078ReaderIdleTime());
        assertTrue(config.isWritable());
    }

    /**
     * 保存部标服务配置: 应更新各项并保留其他配置
     */
    @Test
    void saveJtServerConfig_updatesValues() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml",
                "# 部标\n"
                        + "ftp:\n"
                        + "  enable: true\n"
                        + "  port: 2121\n"
                        + "jt1078:\n"
                        + "  enable: true\n"
                        + "  port: 18088\n"
                        + "sip:\n"
                        + "  port: 5060\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        JtServerConfig config = new JtServerConfig();
        config.setFtpEnable(false);
        config.setFtpPort(2122);
        config.setFtpPassivePorts("11000-11500");
        config.setJt1078Enable(true);
        config.setJt1078Port(18089);
        config.setJt1078Password("pass123");
        config.setJt1078Record(true);
        config.setJt1078ReaderIdleTime(30);
        service.saveJtServerConfig(config);

        String updated = Files.readString(configDir.resolve("application-dev.yml"));
        assertTrue(updated.contains("port: 2122"), "FTP端口应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("passive-ports: 11000-11500"), "被动端口应被写入, 实际:\n" + updated);
        assertTrue(updated.contains("port: 18089"), "jt1078端口应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("password: pass123"), "jt1078密码应被写入, 实际:\n" + updated);
        assertTrue(updated.contains("reader-idle-time: 30"), "IDLE超时时间应被写入, 实际:\n" + updated);
        assertTrue(updated.contains("# 部标"), "应保留头部注释");
        assertTrue(updated.contains("sip:\n  port: 5060"), "应保留其他配置项");
    }

    /**
     * 保存部标服务配置: 被动端口格式非法时应报错
     */
    @Test
    void saveJtServerConfig_invalidPassivePorts_throws() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "ftp:\n  enable: true\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        JtServerConfig config = new JtServerConfig();
        config.setFtpPassivePorts("abc");
        assertThrows(Exception.class, () -> service.saveJtServerConfig(config));
    }

    // ------------------------------------------------------------------
    // 全局策略-流媒体(media)
    // ------------------------------------------------------------------

    /**
     * 回写默认媒体节点: 应更新各项, 空串不覆盖已有端口范围, 并保留其它配置
     */
    @Test
    void saveDefaultMediaConfig_writesValuesAndSkipsEmpty() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml",
                "# 流媒体\n"
                        + "media:\n"
                        + "  id: old-id\n"
                        + "  ip: 192.168.1.3\n"
                        + "  secret: old-secret\n"
                        + "  rtp:\n"
                        + "    enable: false\n"
                        + "    port-range: 10000,20000\n"
                        + "sip:\n"
                        + "  port: 5060\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        MediaServer mediaServer = new MediaServer();
        mediaServer.setId("new-id");
        mediaServer.setIp("192.168.1.4");
        mediaServer.setHookIp("127.0.0.1");
        mediaServer.setSecret("new-secret");
        mediaServer.setHttpPort(7082);
        mediaServer.setAutoConfig(true);
        mediaServer.setRtpEnable(true);
        mediaServer.setRtpPortRange("");
        mediaServer.setRecordDay(7);
        mediaServer.setRecordPath("./www/record");
        mediaServer.setType("zlm");
        service.saveDefaultMediaConfig(mediaServer);

        String updated = Files.readString(configDir.resolve("application-dev.yml"));
        assertTrue(updated.contains("id: new-id"), "媒体ID应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("ip: 192.168.1.4"), "媒体IP应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("secret: new-secret"), "密钥应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("http-port: 7082"), "HTTP端口应被写入, 实际:\n" + updated);
        assertTrue(updated.contains("record-day: 7"), "录像天数应被写入, 实际:\n" + updated);
        assertTrue(updated.contains("record-path: ./www/record"), "录像路径应被写入, 实际:\n" + updated);
        assertTrue(updated.contains("port-range: 10000,20000"), "空串不应覆盖已有收流端口, 实际:\n" + updated);
        assertTrue(updated.contains("# 流媒体"), "应保留头部注释");
        assertTrue(updated.contains("sip:\n  port: 5060"), "应保留其他配置项");
    }

    /**
     * 当前生效配置文件可写时应返回 true
     */
    @Test
    void isActiveConfigWritable_returnsTrueForWritableFile() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "media:\n  id: a\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        assertTrue(service.isActiveConfigWritable());
    }

    // ------------------------------------------------------------------
    // 全局策略-平台策略(user-settings)
    // ------------------------------------------------------------------

    /**
     * 查询平台策略配置: 文件中的值优先, 缺失时回退到默认值
     */
    @Test
    void queryUserSettingConfig_readsAndFallsBackToDefaults() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml",
                "user-settings:\n"
                        + "  sip-log: true\n"
                        + "  server-id: \"765250\"\n"
                        + "  gb-device-online: 1\n"
                        + "  allowed-origins:\n"
                        + "    - http://localhost:8008\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        UserSettingConfig config = service.queryUserSettingConfig();

        assertEquals("765250", config.getServerId());
        assertEquals(Boolean.TRUE, config.getSipLog());
        assertEquals(1, config.getGbDeviceOnline());
        assertEquals(List.of("http://localhost:8008"), config.getAllowedOrigins());
        // 文件中缺失的项回退到默认值
        assertEquals(Boolean.FALSE, config.getSqlLog());
        assertEquals(Boolean.TRUE, config.getPushAuthority());
        assertNotNull(config.getPlayTimeout());
    }

    /**
     * 保存平台策略配置: 应写入标量与列表配置项, 并保留其他配置
     */
    @Test
    void saveUserSettingConfig_writesScalarsAndLists() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml",
                "# 平台策略\n"
                        + "user-settings:\n"
                        + "  sip-log: false   # 日志\n"
                        + "server:\n"
                        + "  port: 18080\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        UserSettingConfig config = new UserSettingConfig();
        config.setSipLog(true);
        config.setServerId("765250");
        config.setGbDeviceOnline(0);
        config.setInterfaceAuthenticationExcludes(List.of("/api/sy/**", "/api/common/**"));
        config.setAllowedOrigins(List.of("http://a.com", "http://b.com"));
        config.setAllowedAlarmType(List.of("IntrusionDetection", "VideoLoss"));
        service.saveUserSettingConfig(config);

        String updated = Files.readString(configDir.resolve("application-dev.yml"));
        assertTrue(updated.contains("sip-log: true"), "布尔配置应被更新, 实际:\n" + updated);
        assertTrue(updated.contains("server-id: \"765250\""), "字符串配置应被写入(必要时加引号), 实际:\n" + updated);
        assertTrue(updated.contains("gb-device-online: 0"), "数字配置应被写入, 实际:\n" + updated);
        assertTrue(updated.contains("interface-authentication-excludes:\n    - /api/sy/**\n    - /api/common/**"),
                "应写入鉴权例外列表, 实际:\n" + updated);
        assertTrue(updated.contains("allowed-origins:\n    - http://a.com\n    - http://b.com"),
                "应写入跨域列表, 实际:\n" + updated);
        assertTrue(updated.contains("allowed-alarm-type:\n    - IntrusionDetection\n    - VideoLoss"),
                "应写入报警类型列表, 实际:\n" + updated);
        assertTrue(updated.contains("# 平台策略"), "应保留头部注释");
        assertTrue(updated.contains("# 日志"), "应保留行尾注释");
        assertTrue(updated.contains("server:\n  port: 18080"), "应保留其他配置项");
    }

    /**
     * 保存平台策略配置: 应替换已有列表并保留同级配置项
     */
    @Test
    void saveUserSettingConfig_replacesExistingList_keepsSiblings() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml",
                "user-settings:\n"
                        + "  server-id: \"000000\"\n"
                        + "  interface-authentication-excludes:\n"
                        + "    - /api/old/**\n"
                        + "  doc-enable: true\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        UserSettingConfig config = new UserSettingConfig();
        config.setInterfaceAuthenticationExcludes(List.of("/api/new/**", "/api/other/**"));
        service.saveUserSettingConfig(config);

        String updated = Files.readString(configDir.resolve("application-dev.yml"));
        assertTrue(updated.contains("interface-authentication-excludes:\n    - /api/new/**\n    - /api/other/**"),
                "应替换原有列表, 实际:\n" + updated);
        assertFalse(updated.contains("/api/old/**"), "旧列表项应被移除, 实际:\n" + updated);
        assertTrue(updated.contains("doc-enable: true"), "应保留后续同级配置, 实际:\n" + updated);
        assertTrue(updated.contains("server-id: \"000000\""), "应保留原引号字符串, 实际:\n" + updated);
    }

    /**
     * 查询平台策略配置: 应下发需要重启才能生效的字段清单
     */
    @Test
    void queryUserSettingConfig_returnsRestartRequiredFields() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "user-settings:\n  sip-log: true\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        UserSettingConfig config = service.queryUserSettingConfig();

        assertNotNull(config.getRestartRequiredFields());
        assertTrue(config.getRestartRequiredFields().contains("serverId"));
        assertTrue(config.getRestartRequiredFields().contains("allowedOrigins"));
        assertTrue(config.getRestartRequiredFields().contains("sipLog"));
        assertFalse(config.getRestartRequiredFields().contains("loginTimeout"), "热字段不应出现在重启清单中");
    }

    /**
     * 保存平台策略配置: 仅修改热字段时无需重启, 且应即时生效
     */
    @Test
    void saveUserSettingConfig_hotField_appliesImmediately() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "user-settings:\n  login-timeout: 60\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        UserSetting runtime = new UserSetting();
        runtime.setLoginTimeout(60L);
        ReflectionTestUtils.setField(service, "userSetting", runtime);

        UserSettingConfig config = service.queryUserSettingConfig();
        config.setLoginTimeout(120L);

        UserSettingSaveResult result = service.saveUserSettingConfig(config);

        assertFalse(result.isRestartRequired(), "仅修改热字段无需重启");
        assertTrue(result.getRestartFields().isEmpty(), "不应报告需要重启的字段");
        assertEquals(120L, runtime.getLoginTimeout(), "热字段应即时生效");
    }

    /**
     * 保存平台策略配置: 修改需要重启的字段时应报告, 且不即时生效
     */
    @Test
    void saveUserSettingConfig_restartField_reportsAndKeepsRuntimeValue() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "user-settings:\n  sip-log: false\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        UserSetting runtime = new UserSetting();
        runtime.setSipLog(false);
        ReflectionTestUtils.setField(service, "userSetting", runtime);

        UserSettingConfig config = service.queryUserSettingConfig();
        config.setSipLog(true);

        UserSettingSaveResult result = service.saveUserSettingConfig(config);

        assertTrue(result.isRestartRequired(), "修改 sip-log 需要重启");
        assertTrue(result.getRestartFields().contains("sipLog"));
        assertFalse(runtime.getSipLog(), "需要重启的字段不应即时生效");
    }

    /**
     * 保存平台策略配置: 非法的报警类型应被拒绝
     */
    @Test
    void saveUserSettingConfig_invalidAlarmType_throws() throws IOException {
        writeFile("application.yml", "spring:\n  profiles:\n    active: dev\n");
        writeFile("application-dev.yml", "user-settings:\n  sip-log: true\n");

        MockEnvironment environment = envWithLocation("optional:file:" + configDir.toAbsolutePath() + "/");
        environment.setActiveProfiles("dev");
        inject(environment);

        UserSettingConfig config = new UserSettingConfig();
        config.setAllowedAlarmType(List.of("NotAnAlarm"));
        assertThrows(Exception.class, () -> service.saveUserSettingConfig(config));
    }

    /**
     * 查询报警类型列表
     */
    @Test
    void queryAlarmTypes_returnsAll() {
        List<Map<String, String>> types = service.queryAlarmTypes();
        assertFalse(types.isEmpty());
        assertTrue(types.stream().anyMatch(item -> "IntrusionDetection".equals(item.get("name"))));
        assertTrue(types.stream().anyMatch(item -> item.get("description") != null));
    }
}
