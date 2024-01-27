package com.genersoft.iot.vmp.gb28181.bean.command;

/**
 * PTZ指令
 */
public class PTZCommand implements ICommandInfo{

    /**
     * 镜头变倍: 缩小
     */
    private boolean out;

    /**
     * 镜头变倍: 放大
     */
    private boolean in;

    /**
     * 云台垂直方向控制: 上
     */
    private boolean up;

    /**
     * 云台垂直方向控制: 下
     */
    private boolean down;

    /**
     * 云台垂直方向控制: 左
     */
    private boolean left;

    /**
     * 云台垂直方向控制: 右
     */
    private boolean right;

    /**
     * 水平控制速度相对值
     */
    private int xSpeed;

    /**
     * 垂直控制速度相对值
     */
    private int ySpeed;

    /**
     * 变倍控制速度相对值（地址高4位）
     */
    private int zSpeed;

    @Override
    public CommandType getType() {
        return CommandType.PTZ;
    }

    public static PTZCommand getInstance(String command) {
        PTZCommand ptzCommand = new PTZCommand();
        int byte4 = Integer.parseInt(command.substring(6, 8), 16);
        int byte5 = Integer.parseInt(command.substring(8, 10), 16);
        int byte6 = Integer.parseInt(command.substring(10, 12), 16);
        int byte7 = Integer.parseInt(command.substring(12, 14), 16);
        ptzCommand.setOut((byte4 >> 5 & 1) == 1);
        ptzCommand.setIn((byte4 >> 4 & 1) == 1);
        ptzCommand.setUp((byte4 >> 3 & 1) == 1);
        ptzCommand.setDown((byte4 >> 2 & 1) == 1);
        ptzCommand.setLeft((byte4 >> 1 & 1) == 1);
        ptzCommand.setRight((byte4 & 1) == 1);
        ptzCommand.setxSpeed(byte5);
        ptzCommand.setySpeed(byte6);
        // 取高四位
        ptzCommand.setzSpeed(byte7 & 0xf0 >> 4);
        return ptzCommand;
    }

    public boolean isOut() {
        return out;
    }

    public void setOut(boolean out) {
        this.out = out;
    }

    public boolean isIn() {
        return in;
    }

    public void setIn(boolean in) {
        this.in = in;
    }

    public boolean isUp() {
        return up;
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public boolean isDown() {
        return down;
    }

    public void setDown(boolean down) {
        this.down = down;
    }

    public boolean isLeft() {
        return left;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public boolean isRight() {
        return right;
    }

    public void setRight(boolean right) {
        this.right = right;
    }

    public int getxSpeed() {
        return xSpeed;
    }

    public void setxSpeed(int xSpeed) {
        this.xSpeed = xSpeed;
    }

    public int getySpeed() {
        return ySpeed;
    }

    public void setySpeed(int ySpeed) {
        this.ySpeed = ySpeed;
    }

    public int getzSpeed() {
        return zSpeed;
    }

    public void setzSpeed(int zSpeed) {
        this.zSpeed = zSpeed;
    }
}
