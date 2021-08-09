package com.genersoft.iot.vmp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 一个优秀的颓废程序猿
 */
@Component
public class JarFileUtils {
    private static Logger log = LoggerFactory.getLogger(JarFileUtils.class);
    private static Map<String, String> map = new HashMap<>();

    public  Map<String, String> readJarFile() {
        JarFile jarFile = null;
        BufferedReader br = null;
        try {
            // 获取jar的运行路径，因linux下jar的路径为”file:/app/.../test.jar!/BOOT-INF/class!/“这种格式，所以需要去掉”file:“和”!/BOOT-INF/class!/“
            String jarFilePath = ClassUtils.getDefaultClassLoader().getResource("").getPath().replace("!/BOOT-INF/classes!/", "");
            if (jarFilePath.startsWith("file")) {
                jarFilePath = jarFilePath.substring(5);
            }
            log.debug("jarFilePath:" + jarFilePath);
            // 通过JarFile的getJarEntry方法读取META-INF/MANIFEST.MF
            jarFile = new JarFile(jarFilePath);
            JarEntry entry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            log.info("读取的内容:" + entry.toString());
            // 如果读取到MANIFEST.MF文件内容，则转换为string
            if (entry != null) {
                InputStream in = jarFile.getInputStream(entry);

                StringBuilder sb = new StringBuilder();
                br = new BufferedReader(new InputStreamReader(in));
                String line = "";
                while ((line = br.readLine()) != null) {
                    if (line != null && line.contains(":")) {
                        int index = line.indexOf(":");
                        map.put(line.substring(0, index).trim(), line.substring(index + 1, line.length()).trim());
                    }
                }
                return map;
            }
        } catch (IOException e) {
            log.debug("读取MANIFEST.MF文件异常:" + e.getMessage());
        } finally {
            try {
                if (null != br) {
                    br.close();
                }
                if (null != jarFile) {
                    jarFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return map;

    }

}
