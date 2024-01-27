package com.genersoft.iot.vmp.gb28181.bean.command;

/**
 * FI指令
 */
public class FICommand implements ICommandInfo{

    /**
     * 光圈: 缩小
     */
    private boolean IirisOut;

    /**
     * 光圈: 放大
     */
    private boolean IirisIn;

    /**
     * 聚焦: 近
     */
    private boolean focusNear;

    /**
     * 聚焦: 远
     */
    private boolean focusFar;

    /**
     * 光圈速度
     */
    private int IirisSpeed;

    /**
     * 聚焦速度
     */
    private int focusSpeed;

    @Override
    public CommandType getType() {
        return CommandType.FI;
    }

    public static FICommand getInstance(String command) {
        FICommand fiCommand  = new FICommand();
        int byte4 = Integer.parseInt(command.substring(6, 8), 16);
        int byte5 = Integer.parseInt(command.substring(8, 10), 16);
        int byte6 = Integer.parseInt(command.substring(10, 12), 16);
        fiCommand.setIirisOut((byte4 >> 3 & 1) == 1);
        fiCommand.setIirisIn((byte4 >> 2 & 1) == 1);
        fiCommand.setFocusNear((byte4 >> 1 & 1) == 1);
        fiCommand.setFocusFar((byte4 & 1) == 1);
        fiCommand.setFocusSpeed(byte5);
        fiCommand.setIirisSpeed(byte6);
        return fiCommand;
    }

    public boolean isIirisOut() {
        return IirisOut;
    }

    public void setIirisOut(boolean iirisOut) {
        IirisOut = iirisOut;
    }

    public boolean isIirisIn() {
        return IirisIn;
    }

    public void setIirisIn(boolean iirisIn) {
        IirisIn = iirisIn;
    }

    public boolean isFocusNear() {
        return focusNear;
    }

    public void setFocusNear(boolean focusNear) {
        this.focusNear = focusNear;
    }

    public boolean isFocusFar() {
        return focusFar;
    }

    public void setFocusFar(boolean focusFar) {
        this.focusFar = focusFar;
    }

    public int getIirisSpeed() {
        return IirisSpeed;
    }

    public void setIirisSpeed(int iirisSpeed) {
        IirisSpeed = iirisSpeed;
    }

    public int getFocusSpeed() {
        return focusSpeed;
    }

    public void setFocusSpeed(int focusSpeed) {
        this.focusSpeed = focusSpeed;
    }
}
