package com.genersoft.iot.vmp.jt1078.bean.config;

/**
 * 碰撞报警参数设置
 */
public class CollisionAlarmParams implements JTDeviceSubConfig{

    /**
     * 碰撞时间 单位为毫秒(ms)
     */
    private int collisionAlarmTime;

    /**
     * 碰撞加速度 单位为0.1g,设置范围为0~79,默认为10
     */
    private int collisionAcceleration;

    public int getCollisionAlarmTime() {
        return collisionAlarmTime;
    }

    public void setCollisionAlarmTime(int collisionAlarmTime) {
        this.collisionAlarmTime = collisionAlarmTime;
    }

    public int getCollisionAcceleration() {
        return collisionAcceleration;
    }

    public void setCollisionAcceleration(int collisionAcceleration) {
        this.collisionAcceleration = collisionAcceleration;
    }

    @Override
    public byte[] encode() {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (collisionAlarmTime & 0xff);
        bytes[1] = (byte) (collisionAcceleration & 0xff);
        return bytes;
    }
}
