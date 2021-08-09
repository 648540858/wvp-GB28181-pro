package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.common.VersionPo;
import com.genersoft.iot.vmp.utils.GitUtil;
import com.genersoft.iot.vmp.utils.JarFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VersionInfo {

    @Autowired
    VersionConfig config;
    @Autowired
    GitUtil gitUtil;
    @Autowired
    JarFileUtils jarFileUtils;

    public VersionPo getVersion() {
        VersionPo versionPo = new VersionPo();
        Map<String,String> map=jarFileUtils.readJarFile();
        versionPo.setGIT_Revision(gitUtil.getGitCommitId());
        versionPo.setCreate_By(map.get("Created-By"));
        versionPo.setGIT_BRANCH(gitUtil.getBranch());
        versionPo.setGIT_URL(gitUtil.getGitUrl());
        versionPo.setBUILD_DATE(gitUtil.getBuildDate());
        versionPo.setArtifactId(config.getArtifactId());
        versionPo.setGIT_Revision_SHORT(gitUtil.getCommitIdShort());
        versionPo.setVersion(config.getVersion());
        versionPo.setProject(config.getDescription());
        versionPo.setBuild_Jdk(map.get("Build-Jdk"));

        return versionPo;
    }
}
