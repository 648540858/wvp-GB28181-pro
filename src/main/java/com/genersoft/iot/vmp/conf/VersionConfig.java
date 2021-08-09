package com.genersoft.iot.vmp.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "version")
public class VersionConfig {

    private String version;
    private String artifactId;
    private String description;

    public void setVersion(String version) {
        this.version = version;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getDescription() {
        return description;
    }
}
