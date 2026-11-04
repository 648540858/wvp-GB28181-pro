package com.genersoft.iot.vmp.gb28181.service.download;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 持久化一轮设备分段下载的身份、物理对话和已接收文件，不把逻辑任务当作物理媒体流。 */
@Data
public class SegmentedDownloadTask {
    private String id;
    private String stream;
    private String deviceId;
    private String serverId;
    private String workerId;
    private String startTime;
    private String endTime;
    private String mediaServerId;
    private String childStream;
    private String error;
    private String artifactToken;
    private Integer channelId;
    private int downloadSpeed;
    private int segmentIndex;
    private int attempt;
    private long version;
    private long createdAt;
    private long updatedAt;
    private long deadlineAt;
    private long nextActionAt;
    private long childStartedAt;
    private boolean safeToStart;
    private boolean cancelRequested;
    private State state;
    private SsrcTransaction dialog;
    // 最近一次真实INVITE成功的身份只用于恢复投影，不能伪装成仍活动的SIP会话。
    private SsrcTransaction lastDialog;
    private StreamInfo childInfo;
    private DownloadFileInfo finalFile;
    private List<Segment> segments = new ArrayList<>();

    /** 描述分段下载与可靠关闭的阶段；失败或取消后仍可能等待设备释放。 */
    public enum State {
        QUEUED, STARTING, DOWNLOADING, CLOSING, RETRY_WAIT, MERGING, COMPLETED, FAILED, CANCELLED
    }

    /** 只判断逻辑任务是否结束，设备是否释放由独立的安全标志证明。 */
    public boolean isTerminal() {
        return state == State.COMPLETED || state == State.FAILED || state == State.CANCELLED;
    }

    /** 执行中的父任务或未确认释放的终态都不能被同区间新请求覆盖。 */
    public boolean isBusy() {
        return !isTerminal() || !safeToStart;
    }

    /** 云端合并不再占摄像机名额；未知关闭与子段间隙仍保留设备调度权。 */
    public boolean occupiesDevice() {
        return !safeToStart || (!isTerminal() && state != State.MERGING);
    }

    /** 保存单个已完成分段的原始区间和文件身份，供最终合并核验。 */
    @Data
    public static class Segment {
        private String stream;
        private String startTime;
        private String endTime;
        private StreamInfo info;
    }
}
