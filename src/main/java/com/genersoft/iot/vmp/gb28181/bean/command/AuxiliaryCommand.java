package com.genersoft.iot.vmp.gb28181.bean.command;

/**
 * 辅助开关
 */
public class AuxiliaryCommand implements ICommandInfo{

    public static enum Type{
        ON, OFF
    }

    /**
     * 指令类型： 开, 关
     */
    private Type type;

    /**
     * 辅助开关编号，取值为“1”表示雨刷控制
     */
    private int no;

    @Override
    public CommandType getType() {
        return CommandType.AUXILIARY;
    }

    public static AuxiliaryCommand getInstance(Type type, String command) {
        AuxiliaryCommand auxiliaryCommand  = new AuxiliaryCommand();
        auxiliaryCommand.setType(type);
        auxiliaryCommand.setNo(Integer.parseInt(command.substring(8, 10), 16));
        return auxiliaryCommand;
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
