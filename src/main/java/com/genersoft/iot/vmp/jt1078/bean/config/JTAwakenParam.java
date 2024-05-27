package com.genersoft.iot.vmp.jt1078.bean.config;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 终端休眠唤醒模式设置
 */
public class JTAwakenParam implements JTDeviceSubConfig{

    /**
     * 休眠唤醒模式-条件唤醒
     */
    private boolean wakeUpModeByCondition;

    /**
     * 休眠唤醒模式-定时唤醒
     */
    private boolean wakeUpModeByTime;

    /**
     * 休眠唤醒模式-手动唤醒
     */
    private boolean wakeUpModeByManual;

    /**
     * 唤醒条件类型-紧急报警
     */
    private boolean wakeUpConditionsByAlarm;

    /**
     * 唤醒条件类型-碰撞侧翻报警
     */
    private boolean wakeUpConditionsByRollover;

    /**
     * 唤醒条件类型-车辆开门
     */
    private boolean wakeUpConditionsByOpenTheDoor;

    /**
     * 定时唤醒日设置-周一
     */
    private boolean awakeningDayForMonday;

    /**
     * 定时唤醒日设置-周二
     */
    private boolean awakeningDayForTuesday;

    /**
     * 定时唤醒日设置-周三
     */
    private boolean awakeningDayForWednesday;

    /**
     * 定时唤醒日设置-周四
     */
    private boolean awakeningDayForThursday;

    /**
     * 定时唤醒日设置-周五
     */
    private boolean awakeningDayForFriday;

    /**
     * 定时唤醒日设置-周六
     */
    private boolean awakeningDayForSaturday;

    /**
     * 定时唤醒日设置-周日
     */
    private boolean awakeningDayForSunday;

    /**
     * 日定时唤醒-启用时间段1
     */
    private boolean time1Enable;

    /**
     * 日定时唤醒-时间段1开始时间
     */
    private String time1StartTime;

    /**
     * 日定时唤醒-时间段1结束时间
     */
    private String time1EndTime;

    /**
     * 日定时唤醒-启用时间段2
     */
    private boolean time2Enable;

    /**
     * 日定时唤醒-时间段2开始时间
     */
    private String time2StartTime;

    /**
     * 日定时唤醒-时间段2结束时间
     */
    private String time2EndTime;

    /**
     * 日定时唤醒-启用时间段3
     */
    private boolean time3Enable;

    /**
     * 日定时唤醒-时间段3开始时间
     */
    private String time3StartTime;

    /**
     * 日定时唤醒-时间段3结束时间
     */
    private String time3EndTime;

    /**
     * 日定时唤醒-启用时间段4
     */
    private boolean time4Enable;

    /**
     * 日定时唤醒-时间段4开始时间
     */
    private String time4StartTime;

    /**
     * 日定时唤醒-时间段4结束时间
     */
    private String time4EndTime;

    public boolean isWakeUpModeByCondition() {
        return wakeUpModeByCondition;
    }

    public void setWakeUpModeByCondition(boolean wakeUpModeByCondition) {
        this.wakeUpModeByCondition = wakeUpModeByCondition;
    }

    public boolean isWakeUpModeByTime() {
        return wakeUpModeByTime;
    }

    public void setWakeUpModeByTime(boolean wakeUpModeByTime) {
        this.wakeUpModeByTime = wakeUpModeByTime;
    }

    public boolean isWakeUpModeByManual() {
        return wakeUpModeByManual;
    }

    public void setWakeUpModeByManual(boolean wakeUpModeByManual) {
        this.wakeUpModeByManual = wakeUpModeByManual;
    }

    public boolean isWakeUpConditionsByAlarm() {
        return wakeUpConditionsByAlarm;
    }

    public void setWakeUpConditionsByAlarm(boolean wakeUpConditionsByAlarm) {
        this.wakeUpConditionsByAlarm = wakeUpConditionsByAlarm;
    }

    public boolean isWakeUpConditionsByRollover() {
        return wakeUpConditionsByRollover;
    }

    public void setWakeUpConditionsByRollover(boolean wakeUpConditionsByRollover) {
        this.wakeUpConditionsByRollover = wakeUpConditionsByRollover;
    }

    public boolean isWakeUpConditionsByOpenTheDoor() {
        return wakeUpConditionsByOpenTheDoor;
    }

    public void setWakeUpConditionsByOpenTheDoor(boolean wakeUpConditionsByOpenTheDoor) {
        this.wakeUpConditionsByOpenTheDoor = wakeUpConditionsByOpenTheDoor;
    }

    public boolean isAwakeningDayForMonday() {
        return awakeningDayForMonday;
    }

    public void setAwakeningDayForMonday(boolean awakeningDayForMonday) {
        this.awakeningDayForMonday = awakeningDayForMonday;
    }

    public boolean isAwakeningDayForTuesday() {
        return awakeningDayForTuesday;
    }

    public void setAwakeningDayForTuesday(boolean awakeningDayForTuesday) {
        this.awakeningDayForTuesday = awakeningDayForTuesday;
    }

    public boolean isAwakeningDayForWednesday() {
        return awakeningDayForWednesday;
    }

    public void setAwakeningDayForWednesday(boolean awakeningDayForWednesday) {
        this.awakeningDayForWednesday = awakeningDayForWednesday;
    }

    public boolean isAwakeningDayForThursday() {
        return awakeningDayForThursday;
    }

    public void setAwakeningDayForThursday(boolean awakeningDayForThursday) {
        this.awakeningDayForThursday = awakeningDayForThursday;
    }

    public boolean isAwakeningDayForFriday() {
        return awakeningDayForFriday;
    }

    public void setAwakeningDayForFriday(boolean awakeningDayForFriday) {
        this.awakeningDayForFriday = awakeningDayForFriday;
    }

    public boolean isAwakeningDayForSaturday() {
        return awakeningDayForSaturday;
    }

    public void setAwakeningDayForSaturday(boolean awakeningDayForSaturday) {
        this.awakeningDayForSaturday = awakeningDayForSaturday;
    }

    public boolean isAwakeningDayForSunday() {
        return awakeningDayForSunday;
    }

    public void setAwakeningDayForSunday(boolean awakeningDayForSunday) {
        this.awakeningDayForSunday = awakeningDayForSunday;
    }

    public boolean isTime1Enable() {
        return time1Enable;
    }

    public void setTime1Enable(boolean time1Enable) {
        this.time1Enable = time1Enable;
    }

    public String getTime1StartTime() {
        return time1StartTime;
    }

    public void setTime1StartTime(String time1StartTime) {
        this.time1StartTime = time1StartTime;
    }

    public String getTime1EndTime() {
        return time1EndTime;
    }

    public void setTime1EndTime(String time1EndTime) {
        this.time1EndTime = time1EndTime;
    }

    public boolean isTime2Enable() {
        return time2Enable;
    }

    public void setTime2Enable(boolean time2Enable) {
        this.time2Enable = time2Enable;
    }

    public String getTime2StartTime() {
        return time2StartTime;
    }

    public void setTime2StartTime(String time2StartTime) {
        this.time2StartTime = time2StartTime;
    }

    public String getTime2EndTime() {
        return time2EndTime;
    }

    public void setTime2EndTime(String time2EndTime) {
        this.time2EndTime = time2EndTime;
    }

    public boolean isTime3Enable() {
        return time3Enable;
    }

    public void setTime3Enable(boolean time3Enable) {
        this.time3Enable = time3Enable;
    }

    public String getTime3StartTime() {
        return time3StartTime;
    }

    public void setTime3StartTime(String time3StartTime) {
        this.time3StartTime = time3StartTime;
    }

    public String getTime3EndTime() {
        return time3EndTime;
    }

    public void setTime3EndTime(String time3EndTime) {
        this.time3EndTime = time3EndTime;
    }

    public boolean isTime4Enable() {
        return time4Enable;
    }

    public void setTime4Enable(boolean time4Enable) {
        this.time4Enable = time4Enable;
    }

    public String getTime4StartTime() {
        return time4StartTime;
    }

    public void setTime4StartTime(String time4StartTime) {
        this.time4StartTime = time4StartTime;
    }

    public String getTime4EndTime() {
        return time4EndTime;
    }

    public void setTime4EndTime(String time4EndTime) {
        this.time4EndTime = time4EndTime;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte wakeUpTypeByte = 0;
        byte wakeUpConditionsByte = 0;
        byte wakeDayByte = 0;
        if (wakeUpModeByCondition) {
            wakeUpTypeByte = (byte)(wakeUpTypeByte | 1);
        }
        if (wakeUpModeByTime) {
            wakeUpTypeByte = (byte)(wakeUpTypeByte | (1 << 1));
        }
        if (wakeUpModeByManual) {
            wakeUpTypeByte = (byte)(wakeUpTypeByte | (1 << 2));
        }
        byteBuf.writeByte(wakeUpTypeByte);
        if (wakeUpConditionsByAlarm) {
            wakeUpConditionsByte = (byte)(wakeUpConditionsByte | 1);
        }
        if (wakeUpConditionsByRollover) {
            wakeUpConditionsByte = (byte)(wakeUpConditionsByte | (1 << 1));
        }
        if (wakeUpConditionsByOpenTheDoor) {
            wakeUpConditionsByte = (byte)(wakeUpConditionsByte | (1 << 2));
        }
        byteBuf.writeByte(wakeUpConditionsByte);
        if (awakeningDayForMonday) {
            wakeDayByte = (byte)(wakeDayByte | 1);
        }
        if (awakeningDayForTuesday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 1));
        }
        if (awakeningDayForWednesday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 2));
        }
        if (awakeningDayForThursday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 3));
        }
        if (awakeningDayForFriday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 4));
        }
        if (awakeningDayForSaturday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 5));
        }
        if (awakeningDayForSunday) {
            wakeDayByte = (byte)(wakeDayByte | (1 << 6));
        }
        byteBuf.writeByte(wakeDayByte);
        byte enableByte = 0;
        if (time1Enable) {
            enableByte = (byte)(enableByte | 1);
        }
        if (time2Enable) {
            enableByte = (byte)(enableByte | (1 << 1));
        }
        if (time3Enable) {
            enableByte = (byte)(enableByte | (1 << 2));
        }
        if (time4Enable) {
            enableByte = (byte)(enableByte | (1 << 3));
        }
        byteBuf.writeByte(enableByte);
        byteBuf.writeBytes(transportTime(time1StartTime));
        byteBuf.writeBytes(transportTime(time1EndTime));
        byteBuf.writeBytes(transportTime(time2StartTime));
        byteBuf.writeBytes(transportTime(time2EndTime));
        byteBuf.writeBytes(transportTime(time3StartTime));
        byteBuf.writeBytes(transportTime(time3EndTime));
        byteBuf.writeBytes(transportTime(time4StartTime));
        byteBuf.writeBytes(transportTime(time4EndTime));
        return byteBuf;
    }

    private byte[] transportTime(String time) {
        return BCDUtil.strToBcd(time.replace(":", ""));
    }

    public static JTAwakenParam decode(ByteBuf byteBuf) {
        JTAwakenParam awakenParam = new JTAwakenParam();
        short wakeUpTypeByte = byteBuf.readUnsignedByte();
        awakenParam.wakeUpModeByCondition = ((wakeUpTypeByte & 1) == 1);
        awakenParam.wakeUpModeByTime = ((wakeUpTypeByte >>> 1 & 1) == 1);
        awakenParam.wakeUpModeByManual = ((wakeUpTypeByte >>> 2 & 1) == 1);

        short wakeUpConditionsByte = byteBuf.readUnsignedByte();
        awakenParam.wakeUpConditionsByAlarm = ((wakeUpConditionsByte & 1) == 1);
        awakenParam.wakeUpConditionsByRollover = ((wakeUpConditionsByte >>> 1 & 1) == 1);
        awakenParam.wakeUpConditionsByOpenTheDoor = ((wakeUpConditionsByte >>> 2 & 1) == 1);

        short wakeDayByte = byteBuf.readUnsignedByte();
        awakenParam.awakeningDayForMonday = ((wakeDayByte & 1) == 1);
        awakenParam.awakeningDayForTuesday = ((wakeDayByte >>> 1 & 1) == 1);
        awakenParam.awakeningDayForWednesday = ((wakeDayByte >>> 2 & 1) == 1);
        awakenParam.awakeningDayForThursday = ((wakeDayByte >>> 3 & 1) == 1);
        awakenParam.awakeningDayForFriday = ((wakeDayByte >>> 4 & 1) == 1);
        awakenParam.awakeningDayForSaturday = ((wakeDayByte >>> 5 & 1) == 1);
        awakenParam.awakeningDayForSunday = ((wakeDayByte >>> 6 & 1) == 1);
        short enableByte = byteBuf.readUnsignedByte();
        awakenParam.time1Enable = ((enableByte & 1) == 1);
        awakenParam.time2Enable = ((enableByte >>> 1 & 1) == 1);
        awakenParam.time3Enable = ((enableByte >>> 2 & 1) == 1);
        awakenParam.time4Enable = ((enableByte >>> 3 & 1) == 1);
        byte[] timeBytes = new byte[2];
        byteBuf.readBytes(timeBytes);
        awakenParam.time1StartTime = transportTime(timeBytes);
        byteBuf.readBytes(timeBytes);
        awakenParam.time1EndTime = transportTime(timeBytes);

        byteBuf.readBytes(timeBytes);
        awakenParam.time2StartTime = transportTime(timeBytes);
        byteBuf.readBytes(timeBytes);
        awakenParam.time2EndTime = transportTime(timeBytes);

        byteBuf.readBytes(timeBytes);
        awakenParam.time3StartTime = transportTime(timeBytes);
        byteBuf.readBytes(timeBytes);
        awakenParam.time3EndTime = transportTime(timeBytes);

        byteBuf.readBytes(timeBytes);
        awakenParam.time4StartTime = transportTime(timeBytes);
        byteBuf.readBytes(timeBytes);
        awakenParam.time4EndTime = transportTime(timeBytes);
        return awakenParam;
    }

    private static String transportTime(byte[] timeBytes) {
        String time1Str = BCDUtil.transform(timeBytes);
        return time1Str.replace(time1Str.substring(0, 2), time1Str.substring(0, 2) + ":");
    }
}
