package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.service.IApplicationRestartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ApplicationRestartServiceImpl implements IApplicationRestartService {

    /**
     * 重启脚本名称, 位于 jar 同级目录
     */
    private static final String[] SCRIPT_CANDIDATES = {"restart.sh", "wvp.sh"};

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
     */
    private File resolveJarDir() {
        try {
            File location = new File(ApplicationRestartServiceImpl.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (location.isFile()) {
                // 以jar方式运行
                return location.getParentFile();
            }
            // 以classes目录运行(开发环境), 上溯到项目根目录
            return location;
        } catch (Exception e) {
            log.warn("[系统设置] 获取jar所在目录失败: {}", e.getMessage());
            return null;
        }
    }
}
