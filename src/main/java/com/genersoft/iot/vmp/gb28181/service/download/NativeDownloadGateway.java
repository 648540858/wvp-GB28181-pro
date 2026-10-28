package com.genersoft.iot.vmp.gb28181.service.download;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

import java.util.function.Consumer;

/** 分段调度只复用原生录像和终止 owner，不另造 SIP 或 RTP 控制流程。 */
public interface NativeDownloadGateway {
    /** 固定媒体节点并交出原 Dialog；仅返回FALSE证明尚未调用SIP发送，NULL或TRUE都不能推定未建流。 */
    Boolean startNativeDownload(MediaServer media, Device device, DeviceChannel channel, String start, String end,
                             int speed, Consumer<SsrcTransaction> dialogReady, ErrorCallback<StreamInfo> callback);

    /** 查询真实子段，不经过逻辑父任务的公开进度投影。 */
    StreamInfo getNativeDownloadInfo(Device device, DeviceChannel channel, String stream);

    /** 只结束原 Dialog，禁止按父任务流名扩大停止范围。 */
    void stopNativeDownload(Device device, DeviceChannel channel, SsrcTransaction dialog);

    /** 媒体节点成功返回空列表才代表已消失，网络错误不作为重试许可。 */
    boolean isNativeDownloadMediaGone(MediaServer media, String stream);
}
