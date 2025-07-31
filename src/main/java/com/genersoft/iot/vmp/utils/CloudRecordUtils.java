package com.genersoft.iot.vmp.utils;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import lombok.experimental.UtilityClass;

/**
 * 云录像工具类
 *
 * @author 648540858
 */
@UtilityClass
public class CloudRecordUtils {


    /**
     * 修复原始工具类中的格式化问题
     *
     * @param mediaServerItem 媒体服务器配置
     * @param filePath        文件路径（可能包含%等特殊字符）
     * @return 修复后的下载信息
     */
    public static DownloadFileInfo getDownloadFilePath(MediaServer mediaServerItem, String filePath) {
        // 将filePath作为独立参数传入，避免%符号解析问题
        String pathTemplate = "%s://%s:%s/index/api/downloadFile?file_path=%s";

        DownloadFileInfo info = new DownloadFileInfo();

        // filePath作为第4个参数
        info.setHttpPath(String.format(pathTemplate,
                "http",
                mediaServerItem.getStreamIp(),
                mediaServerItem.getHttpPort(),
                filePath));

        // 同样作为第4个参数
        if (mediaServerItem.getHttpSSlPort() > 0) {
            info.setHttpsPath(String.format(pathTemplate,
                    "https",
                    mediaServerItem.getStreamIp(),
                    mediaServerItem.getHttpSSlPort(),
                    filePath));
        }
        return info;
    }
}
