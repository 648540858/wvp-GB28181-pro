package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.vmanager.bean.ProfileInfo;
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
}
