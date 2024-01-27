package com.genersoft.iot.vmp.gb28181.bean.command;

/**
 * 预置位
 */
public class ScanCommand implements ICommandInfo{

    public static enum Type{
        START, SET_LEFT, SET_RIGHT, SET_SPEED
    }

    /**
     * 指令类型： 开始自动扫描, 设置自动扫描左边界, 设置自动扫描右边界, 设置自动扫描速度
     */
    private Type type;

    /**
     * 扫描组号
     */
    private int no;

    /**
     * 自动扫描速度
     */
    private int speed;

    @Override
    public CommandType getType() {
        return CommandType.SCAN;
    }

    public static ScanCommand getInstance(Type type, String command) {
        ScanCommand scanCommand  = new ScanCommand();
        scanCommand.setType(type);
        scanCommand.setNo(Integer.parseInt(command.substring(8, 10), 16));
        if (type == Type.SET_SPEED) {
            scanCommand.setSpeed(Integer.parseInt(command.substring(12, 14), 16) & 0xf0 >> 4);
        }
        return scanCommand;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }
}
