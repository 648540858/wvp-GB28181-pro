package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JT 终端类型
 */
@Schema(description = "JT终端参数设置")
public class JTDeviceType {

    /**
     * 适用客运车辆
     */
    private boolean passengerVehicles;

    /**
     * 适用危险品车辆
     */
    private boolean dangerousGoodsVehicles;

    /**
     * 普通货运车辆
     */
    private boolean freightVehicles;

    /**
     * 出租车辆
     */
    private boolean rentalVehicles;

    /**
     * 支持硬盘录像
     */
    private boolean hardDiskRecording;

    /**
     * false：一体机 ,true：分体机
     */
    private boolean splittingMachine;

    /**
     * 适用挂车
     */
    private boolean trailer;

    public static JTDeviceType getInstance(int content) {
        boolean passengerVehicles = (content & 1) == 1;
        boolean dangerousGoodsVehicles = (content >>> 1 & 1) == 1;
        boolean freightVehicles = (content >>> 2 & 1) == 1;
        boolean rentalVehicles = (content >>> 3 & 1) == 1;
        boolean hardDiskRecording = (content >>> 6 & 1) == 1;
        boolean splittingMachine = (content >>> 7 & 1) == 1;
        boolean trailer = (content >>> 8 & 1) == 1;
        return new JTDeviceType(passengerVehicles, dangerousGoodsVehicles, freightVehicles, rentalVehicles, hardDiskRecording, splittingMachine, trailer);
    }

    public JTDeviceType(boolean passengerVehicles, boolean dangerousGoodsVehicles, boolean freightVehicles, boolean rentalVehicles, boolean hardDiskRecording, boolean splittingMachine, boolean trailer) {
        this.passengerVehicles = passengerVehicles;
        this.dangerousGoodsVehicles = dangerousGoodsVehicles;
        this.freightVehicles = freightVehicles;
        this.rentalVehicles = rentalVehicles;
        this.hardDiskRecording = hardDiskRecording;
        this.splittingMachine = splittingMachine;
        this.trailer = trailer;
    }

    public boolean isPassengerVehicles() {
        return passengerVehicles;
    }

    public void setPassengerVehicles(boolean passengerVehicles) {
        this.passengerVehicles = passengerVehicles;
    }

    public boolean isDangerousGoodsVehicles() {
        return dangerousGoodsVehicles;
    }

    public void setDangerousGoodsVehicles(boolean dangerousGoodsVehicles) {
        this.dangerousGoodsVehicles = dangerousGoodsVehicles;
    }

    public boolean isFreightVehicles() {
        return freightVehicles;
    }

    public void setFreightVehicles(boolean freightVehicles) {
        this.freightVehicles = freightVehicles;
    }

    public boolean isRentalVehicles() {
        return rentalVehicles;
    }

    public void setRentalVehicles(boolean rentalVehicles) {
        this.rentalVehicles = rentalVehicles;
    }

    public boolean isHardDiskRecording() {
        return hardDiskRecording;
    }

    public void setHardDiskRecording(boolean hardDiskRecording) {
        this.hardDiskRecording = hardDiskRecording;
    }

    public boolean isSplittingMachine() {
        return splittingMachine;
    }

    public void setSplittingMachine(boolean splittingMachine) {
        this.splittingMachine = splittingMachine;
    }

    public boolean isTrailer() {
        return trailer;
    }

    public void setTrailer(boolean trailer) {
        this.trailer = trailer;
    }

    @Override
    public String toString() {
        return "JTDeviceType{" +
                "passengerVehicles=" + passengerVehicles +
                ", dangerousGoodsVehicles=" + dangerousGoodsVehicles +
                ", freightVehicles=" + freightVehicles +
                ", rentalVehicles=" + rentalVehicles +
                ", hardDiskRecording=" + hardDiskRecording +
                ", splittingMachine=" + splittingMachine +
                ", trailer=" + trailer +
                '}';
    }
}
