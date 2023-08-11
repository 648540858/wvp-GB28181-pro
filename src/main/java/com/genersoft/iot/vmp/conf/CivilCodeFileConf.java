package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.common.CivilCodePo;
import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Map;

/**
 * 启动时读取行政区划表
 */
@Configuration
@Order(value=14)
public class CivilCodeFileConf implements CommandLineRunner {

    private final static Logger logger = LoggerFactory.getLogger(CivilCodeFileConf.class);

    private final Map<String, CivilCodePo> civilCodeMap= new ConcurrentHashMap<>();

    @Autowired
    @Lazy
    private UserSetting userSetting;

    @Override
    public void run(String... args) throws Exception {
        if (ObjectUtils.isEmpty(userSetting.getCivilCodeFile())) {
            logger.warn("[行政区划] 文件未设置，可能造成目录刷新结果不完整");
            return;
        }
        InputStream inputStream;
        if (userSetting.getCivilCodeFile().startsWith("classpath:")){
            String filePath = userSetting.getCivilCodeFile().substring("classpath:".length());
            ClassPathResource civilCodeFile = new ClassPathResource(filePath);
            if (!civilCodeFile.exists()) {
                logger.warn("[行政区划] 文件<{}>不存在，可能造成目录刷新结果不完整", userSetting.getCivilCodeFile());
                return;
            }
            inputStream = civilCodeFile.getInputStream();

        }else {
            File civilCodeFile = new File(userSetting.getCivilCodeFile());
            if (!civilCodeFile.exists()) {
                logger.warn("[行政区划] 文件<{}>不存在，可能造成目录刷新结果不完整", userSetting.getCivilCodeFile());
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
        if (civilCodeMap.size() == 0) {
            logger.warn("[行政区划] 文件内容为空，可能造成目录刷新结果不完整");
        }else {
            logger.info("[行政区划] 加载成功，共加载数据{}条", civilCodeMap.size());
        }
    }

    public CivilCodePo getParentCode(String code) {
        if (code.length() > 8) {
            return null;
        }
        if (code.length() == 8) {
            String parentCode = code.substring(0, 6);
            return civilCodeMap.get(parentCode);
        }else {
            CivilCodePo civilCodePo = civilCodeMap.get(code);
            if (civilCodePo == null){
                return null;
            }
            String parentCode = civilCodePo.getParentCode();
            if (parentCode == null) {
                return null;
            }
            return civilCodeMap.get(parentCode);
        }

    }

}
