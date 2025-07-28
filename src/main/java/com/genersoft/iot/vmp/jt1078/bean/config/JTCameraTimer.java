package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * 定时拍照控制
 */
@Setter
@Getter
public class JTCameraTimer implements JTDeviceSubConfig{
    /**
     * 摄像通道1 定时拍照开关标志
     */
    private boolean switchForChannel1;
    /**
     * 摄像通道2 定时拍照开关标志
     */
    private boolean switchForChannel2;
    /**
     * 摄像通道3 定时拍照开关标志
     */
    private boolean switchForChannel3;
    /**
     * 摄像通道4 定时拍照开关标志
     */
    private boolean switchForChannel4;
    /**
     * 摄像通道5 定时拍照开关标志
     */
    private boolean switchForChannel5;

    /**
     * 摄像通道1 定时拍照存储标志, true: 上传， false： 存储
     */
    private boolean storageFlagsForChannel1;

    /**
     * 摄像通道2 定时拍照存储标志 true: 上传， false： 存储
     */
    private boolean storageFlagsForChannel2;

    /**
     * 摄像通道3 定时拍照存储标志 true: 上传， false： 存储
     */
    private boolean storageFlagsForChannel3;

    /**
     * 摄像通道4 定时拍照存储标志 true: 上传， false： 存储
     */
    private boolean storageFlagsForChannel4;

    /**
     * 摄像通道5 定时拍照存储标志 true: 上传， false： 存储
     */
    private boolean storageFlagsForChannel5;

    /**
     * 定时时间单位,true: 分， false： 秒，当数值小于5s时，终端按5s处理
     */
    private boolean timeUnit;

    /**
     * 定时时间间隔
     */
    private Integer timeInterval;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte[] bytes = new byte[4];
        bytes[0] = 0;
        if (switchForChannel1) {
            bytes[0] = (byte)(bytes[0] | 1);
        }
        if (switchForChannel2) {
            bytes[0] = (byte)(bytes[0] | 2);
        }
        if (switchForChannel3) {
            bytes[0] = (byte)(bytes[0] | 4);
        }
        if (switchForChannel4) {
            bytes[0] = (byte)(bytes[0] | 8);
        }
        if (switchForChannel5) {
            bytes[0] = (byte)(bytes[0] | 16);
        }
        bytes[1] = 0;
        if (storageFlagsForChannel1) {
            bytes[1] = (byte)(bytes[1] | 1);
        }
        if (storageFlagsForChannel2) {
            bytes[1] = (byte)(bytes[1] | 2);
        }
        if (storageFlagsForChannel3) {
            bytes[1] = (byte)(bytes[1] | 4);
        }
        if (storageFlagsForChannel4) {
            bytes[1] = (byte)(bytes[1] | 8);
        }
        if (storageFlagsForChannel5) {
            bytes[1] = (byte)(bytes[1] | 16);
        }
        bytes[3] = (byte)(timeInterval & 0xfe);
        if (timeUnit) {
            bytes[3] = (byte)(bytes[3] | 1);
        }
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }
}
