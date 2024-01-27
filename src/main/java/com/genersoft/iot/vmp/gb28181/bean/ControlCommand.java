package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.bean.command.*;
import org.springframework.util.ObjectUtils;

/**
 * 国标28181控制指令
 */
public class ControlCommand {

    public static ICommandInfo analysisCommand(String command){
        if (ObjectUtils.isEmpty(command)) {
            return null;
        }
        String byte4Str = command.substring(6, 8);
        int byte4Int = Integer.parseInt(byte4Str, 16);
        switch (byte4Int) {
            case 0x81:
                // 预置位指令: 设置
                return PresetCommand.getInstance(PresetCommand.Type.SET, command);
            case 0x82:
                // 预置位指令: 调用
                return PresetCommand.getInstance(PresetCommand.Type.CALL, command);
            case 0x83:
                // 预置位指令: 删除
                return PresetCommand.getInstance(PresetCommand.Type.DELETE, command);
            case 0x84:
                // 巡航指令: 加入巡航点
                return CruiseCommand.getInstance(CruiseCommand.Type.ADD_POINT, command);
            case 0x85:
                // 巡航指令: 删除一个巡航点
                return CruiseCommand.getInstance(CruiseCommand.Type.DELETE_POINT, command);
            case 0x86:
                // 巡航指令: 设置巡航速度
                return CruiseCommand.getInstance(CruiseCommand.Type.SET_SPEED, command);
            case 0x87:
                // 巡航指令: 设置巡航停留时间
                return CruiseCommand.getInstance(CruiseCommand.Type.SET_TIME, command);
            case 0x88:
                // 巡航指令: 开始巡航
                return CruiseCommand.getInstance(CruiseCommand.Type.START, command);
            case 0x89:
                // 扫描指令
                String byte6Str = command.substring(10, 12);
                int byte6Int = Integer.parseInt(byte6Str, 16);
                switch (byte6Int){
                    case 0x00:
                        // 开始自动扫描
                        return ScanCommand.getInstance(ScanCommand.Type.START, command);
                    case 0x01:
                        // 设置自动扫描左边界
                        return ScanCommand.getInstance(ScanCommand.Type.SET_LEFT, command);
                    case 0x02:
                        // 设置自动扫描右边界
                        return ScanCommand.getInstance(ScanCommand.Type.SET_RIGHT, command);
                    default:
                        return null;
                }
            case 0x8A:
                // 扫描指令-设置自动扫描速度
                return ScanCommand.getInstance(ScanCommand.Type.SET_SPEED, command);
            case 0x8C:
                // 辅助开关-开
                return AuxiliaryCommand.getInstance(AuxiliaryCommand.Type.ON, command);
            case 0x8D:
                // 助开关-关
                return AuxiliaryCommand.getInstance(AuxiliaryCommand.Type.OFF, command);
            default:
                int byte4ForBit6 = byte4Int >> 6 & 1;
                if (byte4ForBit6 == 0) {
                    // PTZ指令
                    return  PTZCommand.getInstance(command);
                }else {
                    // FI指令
                    return  FICommand.getInstance(command);
                }
        }
    }
}
