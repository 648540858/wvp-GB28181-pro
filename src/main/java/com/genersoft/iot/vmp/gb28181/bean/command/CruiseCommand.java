package com.genersoft.iot.vmp.gb28181.bean.command;

/**
 * 巡航指令
 */
public class CruiseCommand implements ICommandInfo{

    public static enum Type{
        ADD_POINT, DELETE_POINT, SET_SPEED, SET_TIME, START
    }

    /**
     * 指令类型： 加入巡航点，删除一个巡航点， 设置巡航速度, 设置巡航停留时间, 开始巡航
     */
    private Type type;

    /**
     * 巡航组号
     */
    private int cruiseGroupNo;

    /**
     * 巡航速度
     */
    private int cruiseSpeed;

    /**
     * 巡航停留时间 是秒(s)
     */
    private int cruiseTime;

    /**
     * 预置位号
     */
    private int presetNo;

    @Override
    public CommandType getType() {
        return CommandType.CRUISE;
    }

    public static CruiseCommand getInstance(Type type, String command) {
        CruiseCommand presetCommand  = new CruiseCommand();
        presetCommand.setType(type);
        presetCommand.setCruiseGroupNo(Integer.parseInt(command.substring(8, 10), 16));
        presetCommand.setPresetNo(Integer.parseInt(command.substring(10, 12), 16));
        int value = Integer.parseInt(command.substring(10, 12), 16) & 0xf0 >> 4;
        if (type == Type.SET_SPEED) {
            presetCommand.setCruiseSpeed(value);
        }
        if (type == Type.SET_TIME) {
            presetCommand.setCruiseTime(value);
        }
        return presetCommand;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getCruiseGroupNo() {
        return cruiseGroupNo;
    }

    public void setCruiseGroupNo(int cruiseGroupNo) {
        this.cruiseGroupNo = cruiseGroupNo;
    }

    public int getCruiseSpeed() {
        return cruiseSpeed;
    }

    public void setCruiseSpeed(int cruiseSpeed) {
        this.cruiseSpeed = cruiseSpeed;
    }

    public int getCruiseTime() {
        return cruiseTime;
    }

    public void setCruiseTime(int cruiseTime) {
        this.cruiseTime = cruiseTime;
    }

    public int getPresetNo() {
        return presetNo;
    }

    public void setPresetNo(int presetNo) {
        this.presetNo = presetNo;
    }
}
