package com.genersoft.iot.vmp.jt1078.bean.config;

/**
 * 定时拍照控制
 */
public class CameraTimer implements JTDeviceSubConfig{
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

    public boolean isSwitchForChannel1() {
        return switchForChannel1;
    }

    public void setSwitchForChannel1(boolean switchForChannel1) {
        this.switchForChannel1 = switchForChannel1;
    }

    public boolean isSwitchForChannel2() {
        return switchForChannel2;
    }

    public void setSwitchForChannel2(boolean switchForChannel2) {
        this.switchForChannel2 = switchForChannel2;
    }

    public boolean isSwitchForChannel3() {
        return switchForChannel3;
    }

    public void setSwitchForChannel3(boolean switchForChannel3) {
        this.switchForChannel3 = switchForChannel3;
    }

    public boolean isSwitchForChannel4() {
        return switchForChannel4;
    }

    public void setSwitchForChannel4(boolean switchForChannel4) {
        this.switchForChannel4 = switchForChannel4;
    }

    public boolean isSwitchForChannel5() {
        return switchForChannel5;
    }

    public void setSwitchForChannel5(boolean switchForChannel5) {
        this.switchForChannel5 = switchForChannel5;
    }

    public boolean isStorageFlagsForChannel1() {
        return storageFlagsForChannel1;
    }

    public void setStorageFlagsForChannel1(boolean storageFlagsForChannel1) {
        this.storageFlagsForChannel1 = storageFlagsForChannel1;
    }

    public boolean isStorageFlagsForChannel2() {
        return storageFlagsForChannel2;
    }

    public void setStorageFlagsForChannel2(boolean storageFlagsForChannel2) {
        this.storageFlagsForChannel2 = storageFlagsForChannel2;
    }

    public boolean isStorageFlagsForChannel3() {
        return storageFlagsForChannel3;
    }

    public void setStorageFlagsForChannel3(boolean storageFlagsForChannel3) {
        this.storageFlagsForChannel3 = storageFlagsForChannel3;
    }

    public boolean isStorageFlagsForChannel4() {
        return storageFlagsForChannel4;
    }

    public void setStorageFlagsForChannel4(boolean storageFlagsForChannel4) {
        this.storageFlagsForChannel4 = storageFlagsForChannel4;
    }

    public boolean isStorageFlagsForChannel5() {
        return storageFlagsForChannel5;
    }

    public void setStorageFlagsForChannel5(boolean storageFlagsForChannel5) {
        this.storageFlagsForChannel5 = storageFlagsForChannel5;
    }

    public boolean isTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(boolean timeUnit) {
        this.timeUnit = timeUnit;
    }

    public Integer getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(Integer timeInterval) {
        this.timeInterval = timeInterval;
    }

    @Override
    public byte[] encode() {
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
        return bytes;
    }
}
