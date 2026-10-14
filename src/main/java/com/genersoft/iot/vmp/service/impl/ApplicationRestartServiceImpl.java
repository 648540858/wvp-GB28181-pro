package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.service.IApplicationRestartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ApplicationRestartServiceImpl implements IApplicationRestartService {

    /**
     * 重启脚本名称, 位于 jar 同级目录
     */
    private static final String[] SCRIPT_CANDIDATES = {"restart.sh"};

    /**
     * 延迟执行时间(秒)
     * <p>
     * 脚本会杀掉当前进程, 需要留出时间让HTTP响应返回给前端
     */
    private static final long DELAY_SECONDS = 3;

    @Override
    public boolean restart() {
        File script = locateScript();
        if (script == null) {
            log.error("[系统设置] 未找到重启脚本, 请确认 {} 位于jar同级目录", Arrays.toString(SCRIPT_CANDIDATES));
            return false;
        }
        if (!script.canExecute()) {
            log.warn("[系统设置] 重启脚本没有执行权限, 尝试自动添加: {}", script.getAbsolutePath());
            if (!script.setExecutable(true, false)) {
                log.error("[系统设置] 无法为重启脚本添加执行权限: {}", script.getAbsolutePath());
                return false;
            }
        }

        final String scriptPath = script.getAbsolutePath();
        final String workDir = script.getParentFile().getAbsolutePath();
        log.info("[系统设置] {}秒后执行重启脚本: {}", DELAY_SECONDS, scriptPath);

        // 异步延迟执行, 保证当前HTTP响应能正常返回
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(DELAY_SECONDS);
                // 使用 bash 执行, 避免脚本缺失执行权限或shebang异常
                List<String> command = new ArrayList<>();
                command.add("/bin/bash");
                command.add(scriptPath);
                command.add("restart");

                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(new File(workDir));
                // 合并输出便于排查, 脚本内部已负责重定向日志
                processBuilder.redirectErrorStream(true);
                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                Process process = processBuilder.start();
                log.info("[系统设置] 重启脚本已启动, pid: {}", process.pid());
            } catch (Exception e) {
                log.error("[系统设置] 执行重启脚本失败: {}", e.getMessage(), e);
            }
        });
        return true;
    }

    /**
     * 定位重启脚本, 优先使用 jar 包所在目录
     */
    private File locateScript() {
        List<File> searchDirs = new ArrayList<>();
        File jarDir = resolveJarDir();
        if (jarDir != null) {
            searchDirs.add(jarDir);
        }
        // 兜底: 当前工作目录
        searchDirs.add(new File(System.getProperty("user.dir")));

        for (File dir : searchDirs) {
            for (String name : SCRIPT_CANDIDATES) {
                File script = new File(dir, name);
                if (script.isFile()) {
                    return script;
                }
            }
        }
        return null;
    }

    /**
     * 获取 jar 包所在目录
     * <p>
     * 按以下顺序定位, 任一成功即返回:
     * <ol>
     *     <li>Spring Boot 可执行 jar 启动时, {@code java.class.path} 即为 fat jar 自身;</li>
     *     <li>通过类的来源(CodeSource)解析, 仅处理 file 协议, 避免 nested jar 的
     *         "URI is not hierarchical" 异常;</li>
     *     <li>兜底: 当前工作目录(脚本启动时已 cd 到 jar 同级目录)。</li>
     * </ol>
     */
    private File resolveJarDir() {
        // 1. Spring Boot fat jar: java.class.path 就是 jar 本身
        File fromClassPath = resolveFromClassPath();
        if (fromClassPath != null) {
            return fromClassPath;
        }
        // 2. 通过类来源定位
        try {
            URL url = ApplicationRestartServiceImpl.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            if (url != null && "file".equalsIgnoreCase(url.getProtocol())) {
                File location = new File(url.toURI());
                return location.isFile() ? location.getParentFile() : location;
            }
        } catch (Exception e) {
            log.debug("[系统设置] 通过类来源定位jar所在目录失败: {}", e.getMessage());
        }
        // 3. 兜底: 当前工作目录
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isEmpty()) {
            return new File(userDir);
        }
        return null;
    }

    /**
     * 从 java.class.path 中解析 jar 所在目录
     * <p>
     * 仅当 classpath 中可唯一定位到一个 jar 时才返回, 否则返回 null
     */
    private File resolveFromClassPath() {
        String classPath = System.getProperty("java.class.path");
        if (classPath == null || classPath.isEmpty()) {
            return null;
        }
        File jar = null;
        for (String entry : classPath.split(Pattern.quote(File.pathSeparator))) {
            String path = entry.trim();
            if (path.isEmpty() || !path.toLowerCase().endsWith(".jar")) {
                continue;
            }
            File candidate = new File(path);
            if (!candidate.isFile()) {
                continue;
            }
            if (jar != null) {
                // 存在多个 jar, 无法确定唯一目录
                return null;
            }
            jar = candidate;
        }
        return jar != null ? jar.getParentFile() : null;
    }
}
