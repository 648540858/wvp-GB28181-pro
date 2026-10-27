package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;

/**
 * SIP Dialog 终止服务。
 *
 * 统一负责从 CALL_ID/STREAM 两个 Hash 中收集事务、按方向和类型发送 BYE，
 * 并在发送成功后删除事务，避免各业务入口重复实现。
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

    /** 设备主动BYE仅确认原Call-ID的结束记录，不发送BYE或按逻辑流清理其他会话。 */
    boolean confirmDeviceBye(String callId);

    /**
     * 停止平台侧事务。
     *
     * @param type   为 null 时停止该通道全部平台侧类型
     * @param stream 为 null 时按平台+通道+类型扫描
     * @return 发送失败的 BYE 数量
     */
    int stopPlatform(Platform platform, CommonGBChannel channel, InviteSessionType type, String stream);
}
