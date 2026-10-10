package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.service.ISettingService;
import com.genersoft.iot.vmp.service.bean.ConfigEntry;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ProfileInfo;
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
