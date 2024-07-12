package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.gb28181.bean.Region;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ObjectUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 启动时读取行政区划表
 */
@Slf4j
@Configuration
@Order(value=14)
public class CivilCodeFileConf implements CommandLineRunner {

    @Autowired
    @Lazy
    private UserSetting userSetting;

    private final Map<String, CivilCodePo> civilCodeMap= new ConcurrentHashMap<>();

    @Override
    public void run(String... args) throws Exception {
        if (ObjectUtils.isEmpty(userSetting.getCivilCodeFile())) {
            log.warn("[行政区划] 文件未设置，可能造成目录刷新结果不完整");
            return;
        }
        InputStream inputStream;
        if (userSetting.getCivilCodeFile().startsWith("classpath:")){
            String filePath = userSetting.getCivilCodeFile().substring("classpath:".length());
            ClassPathResource civilCodeFile = new ClassPathResource(filePath);
            if (!civilCodeFile.exists()) {
                log.warn("[行政区划] 文件<{}>不存在，可能造成目录刷新结果不完整", userSetting.getCivilCodeFile());
                return;
            }
            inputStream = civilCodeFile.getInputStream();

        }else {
            File civilCodeFile = new File(userSetting.getCivilCodeFile());
            if (!civilCodeFile.exists()) {
                log.warn("[行政区划] 文件<{}>不存在，可能造成目录刷新结果不完整", userSetting.getCivilCodeFile());
                return;
            }
            inputStream = Files.newInputStream(civilCodeFile.toPath());
        }

        BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(inputStream));
        int index = -1;
        String line;
        while ((line = inputStreamReader.readLine()) != null) {
            index ++;
            if (index == 0) {
                continue;
            }
            String[] infoArray = line.split(",");
            CivilCodePo civilCodePo = CivilCodePo.getInstance(infoArray);
            civilCodeMap.put(civilCodePo.getCode(), civilCodePo);
        }
        inputStreamReader.close();
        inputStream.close();
        if (civilCodeMap.isEmpty()) {
            log.warn("[行政区划] 文件内容为空，可能造成目录刷新结果不完整");
        }else {
            log.info("[行政区划] 加载成功，共加载数据{}条", civilCodeMap.size());
        }
    }

    public List<Region> getAllChild(String parent) {
        List<Region> result = new ArrayList<>();
        for (String key : civilCodeMap.keySet()) {
            if (parent == null) {
                if (ObjectUtils.isEmpty(civilCodeMap.get(key).getParentCode().trim())) {
                    result.add(Region.getInstance(key, civilCodeMap.get(key).getName(), civilCodeMap.get(key).getParentCode()));
                }
            }else if (civilCodeMap.get(key).getParentCode().equals(parent)) {
                result.add(Region.getInstance(key, civilCodeMap.get(key).getName(), civilCodeMap.get(key).getParentCode()));
            }
        }
        return result;
    }
}
