package com.genersoft.iot.vmp.gb28181.service.download;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/** 按设备显式启用短段下载，默认保持所有设备的原生下载协议和接口行为。 */
@Data
@Component
@ConfigurationProperties(prefix = "record-download.segmented")
public class SegmentedDownloadSettings {
    private Set<String> devices = new HashSet<>();
    private int segmentSeconds = 60;
    private int overlapSeconds = 10;
    private int tailGuardSeconds = 20;
    private int maxAttempts = 3;
    private int maxRequestSeconds = 3600;
    private int maxWorkers = 2;
    private int childTimeoutSeconds = 180;
    private int closeTimeoutSeconds = 60;
    private int jobTimeoutSeconds = 21600;
    private int retentionSeconds = 86400;
    private long maxJobBytes = 4L * 1024 * 1024 * 1024;
    private long maxTotalBytes = 6L * 1024 * 1024 * 1024;
    private String directory = "./data/record-download";
    private String ffmpeg = "ffmpeg";
    private String ffprobe = "ffprobe";
    private String publicBaseUrl;

    /** 只有显式列出的设备进入兼容策略，不根据一次网络抖动改变正常设备的行为。 */
    public boolean appliesTo(String deviceId) {
        return devices != null && devices.contains(deviceId);
    }

    /** 建流前验证资源上限和成品出口，避免下载结束后才发现无法交付文件。 */
    public void validateEnabled() {
        if (segmentSeconds < 20 || segmentSeconds > 120 || overlapSeconds < 2 || overlapSeconds > 20
                || tailGuardSeconds < overlapSeconds || tailGuardSeconds > 30
                || maxAttempts < 1 || maxAttempts > 3 || maxRequestSeconds < segmentSeconds
                || maxRequestSeconds > 3600 || maxWorkers < 1 || maxWorkers > 4
                || childTimeoutSeconds < 30 || childTimeoutSeconds > 600
                || closeTimeoutSeconds < 10 || closeTimeoutSeconds > 300
                || jobTimeoutSeconds < childTimeoutSeconds || jobTimeoutSeconds > 21600
                || retentionSeconds < 3600 || retentionSeconds > 172800
                || maxJobBytes < 1048576 || maxJobBytes > 16L * 1024 * 1024 * 1024
                || maxTotalBytes < maxJobBytes || maxTotalBytes > 32L * 1024 * 1024 * 1024) {
            throw new IllegalStateException("分段下载参数超出资源边界");
        }
        URI uri;
        try {
            uri = URI.create(publicBaseUrl == null ? "" : publicBaseUrl);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("未配置有效的分段下载成品出口");
        }
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null || !"/record-download".equals(uri.getRawPath())) {
            throw new IllegalStateException("成品出口必须是独立的 /record-download HTTP 路径");
        }
    }
}
