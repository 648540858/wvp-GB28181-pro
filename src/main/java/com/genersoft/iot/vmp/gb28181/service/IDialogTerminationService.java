package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;

/**
 * SIP Dialog 终止服务。
 *
 * 统一负责从 CALL_ID/STREAM 两个 Hash 中收集事务、按方向和类型发送 BYE，
 * 录像事务在持久化结束凭据后释放活动索引，协议确认后再删除结束记录。
 */
public interface IDialogTerminationService {

    /**
     * 停止设备侧事务。
     *
     * @param type   为 null 时停止该通道全部设备侧类型
     * @param stream 为 null 时按设备+通道+类型扫描；不为 null 时额外按流过滤
     * @return 发送失败的 BYE 数量
     */
    int stopDevice(Device device, DeviceChannel channel, InviteSessionType type, String stream);

    /**
     * 按流停止设备侧事务，用于媒体流离开事件。
     */
    int stopDeviceByStream(Device device, DeviceChannel channel, String app, String stream);

    /** 结束已确定归属的单个录像对话，不按流名扩收集或关闭其他轮次的接收器。 */
    boolean stopDeviceDialog(Device device, DeviceChannel channel, SsrcTransaction transaction);

    /** 设备主动BYE仅确认原Call-ID的结束记录，不发送BYE或按逻辑流清理其他会话。 */
    boolean confirmDeviceBye(String callId);

    /** 自然设备BYE经协议入口校验并完成媒体收口后，持久化原快照的确认结果并清理本地索引。 */
    boolean confirmDeviceRecordBye(SsrcTransaction snapshot);

    /** 原录像Dialog有精确匹配的协议确认凭据且本地活动/待关闭索引均清理后才返回true。 */
    boolean isDeviceRecordDialogTerminated(SsrcTransaction snapshot);

    /**
     * 停止平台侧事务。
     *
     * @param type   为 null 时停止该通道全部平台侧类型
     * @param stream 为 null 时按平台+通道+类型扫描
     * @return 发送失败的 BYE 数量
     */
    int stopPlatform(Platform platform, CommonGBChannel channel, InviteSessionType type, String stream);
}
