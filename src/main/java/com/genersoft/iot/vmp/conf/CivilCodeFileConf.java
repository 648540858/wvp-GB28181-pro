package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.utils.CivilCodeUtil;
import lombok.extern.slf4j.Slf4j;
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
        List<CivilCodePo> civilCodePoList = new ArrayList<>();
        while ((line = inputStreamReader.readLine()) != null) {
            index ++;
            if (index == 0) {
                continue;
            }
            String[] infoArray = line.split(",");
            CivilCodePo civilCodePo = CivilCodePo.getInstance(infoArray);
            civilCodePoList.add(civilCodePo);
        }
        CivilCodeUtil.INSTANCE.add(civilCodePoList);
        inputStreamReader.close();
        inputStream.close();
        if (civilCodePoList.isEmpty()) {
            log.warn("[行政区划] 文件内容为空，可能造成目录刷新结果不完整");
        }else {
            log.info("[行政区划] 加载成功，共加载数据{}条", civilCodePoList.size());
        }
    }
}
