package com.genersoft.iot.vmp.gb28181.bean.command;

/**
 * 预置位
 */
public class PresetCommand implements ICommandInfo{

    public static enum Type{
        SET, CALL, DELETE
    }

    /**
     * 指令类型： 设置，调用， 删除
     */
    private Type type;

    /**
     * 预置位号
     */
    private int no;

    @Override
    public CommandType getType() {
        return CommandType.PRESET;
    }

    public static PresetCommand getInstance(Type type, String command) {
        PresetCommand presetCommand  = new PresetCommand();
        presetCommand.setType(type);
        presetCommand.setNo(Integer.parseInt(command.substring(10, 13), 16));
        return presetCommand;
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
}
