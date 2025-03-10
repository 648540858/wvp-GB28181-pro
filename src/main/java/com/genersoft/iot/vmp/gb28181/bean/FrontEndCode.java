package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 解析收到的前端控制指令
 */
@Data
public class FrontEndCode {


    public static String encode(IFrontEndControlCode frontEndControlCode){
        return frontEndControlCode.encode();
    }

    public static IFrontEndControlCode decode(@NotNull String cmdStr) {
        if (cmdStr.length() != 16) {
            return null;
        }
        String cmdCodeStr = cmdStr.substring(6, 8);
        int cmdCode = Integer.parseInt(cmdCodeStr, 16);
        if (cmdCode < 39) {
            // PTZ指令
            FrontEndControlCodeForPTZ codeForPTZ = new FrontEndControlCodeForPTZ();
            int zoomOut = cmdCode >> 5 & 1;
            if (zoomOut == 1) {
                codeForPTZ.setZoom(0);
            }
            int zoomIn = cmdCode >> 4 & 1;
            if (zoomIn == 1) {
                codeForPTZ.setZoom(1);
            }
            int tiltUp = cmdCode >> 3 & 1;
            if (tiltUp == 1) {
                codeForPTZ.setTilt(0);
            }
            int tiltDown = cmdCode >> 2 & 1;
            if (tiltDown == 1) {
                codeForPTZ.setTilt(1);
            }
            int panLeft = cmdCode >> 1 & 1;
            if (panLeft == 1) {
                codeForPTZ.setPan(0);
            }
            int panRight = cmdCode & 1;
            if (panRight == 1) {
                codeForPTZ.setPan(1);
            }
            String param1Str = cmdStr.substring(8, 10);
            codeForPTZ.setPanSpeed(Integer.parseInt(param1Str, 16));
            String param2Str = cmdStr.substring(10, 12);
            codeForPTZ.setTiltSpeed(Integer.parseInt(param2Str, 16));
            String param3Str = cmdStr.substring(12, 13);
            codeForPTZ.setZoomSpeed(Integer.parseInt(param3Str, 16));
            return codeForPTZ;
        }else if (cmdCode < 74) {
            // FI指令
            FrontEndControlCodeForFI codeForFI = new FrontEndControlCodeForFI();
            int irisOut = cmdCode >> 3 & 1;
            if (irisOut == 1) {
                codeForFI.setIris(0);
            }
            int irisIn = cmdCode >> 2 & 1;
            if (irisIn == 1) {
                codeForFI.setIris(1);
            }
            int focusNear = cmdCode >> 1 & 1;
            if (focusNear == 1) {
                codeForFI.setFocus(0);
            }
            int focusFar = cmdCode & 1;
            if (focusFar == 1) {
                codeForFI.setFocus(1);
            }

            String param1Str = cmdStr.substring(8, 10);
            codeForFI.setFocusSpeed(Integer.parseInt(param1Str, 16));
            String param2Str = cmdStr.substring(10, 12);
            codeForFI.setIrisSpeed(Integer.parseInt(param2Str, 16));
            return codeForFI;
        }else if (cmdCode < 131) {
            // 预置位指令
            FrontEndControlCodeForPreset  codeForPreset = new FrontEndControlCodeForPreset();
            switch (cmdCode) {
                case 0x81: // 设置预置位
                    codeForPreset.setCode(1);
                    break;
                case 0x82: // 调用预置位
                    codeForPreset.setCode(2);
                    break;
                case 0x83: // 删除预置位
                    codeForPreset.setCode(3);
                    break;
                default:
                    return null;
            }
            // 预置位编号
            String param2Str = cmdStr.substring(10, 12);
            codeForPreset.setPresetId(Integer.parseInt(param2Str, 16));
            return codeForPreset;
        }else if (cmdCode < 136) {
            // 巡航指令
            FrontEndControlCodeForTour  codeForTour = new FrontEndControlCodeForTour();
            String param3Str = cmdStr.substring(12, 13);
            switch (cmdCode) {
                case 0x84: // 加入巡航点
                    codeForTour.setCode(1);
                    break;
                case 0x85: // 删除一个巡航点
                    codeForTour.setCode(2);
                    break;
                case 0x86: // 设置巡航速度
                    codeForTour.setCode(3);
                    codeForTour.setTourSpeed(Integer.parseInt(param3Str, 16));
                    break;
                case 0x87: // 设置巡航停留时间
                    codeForTour.setCode(4);
                    codeForTour.setTourTime(Integer.parseInt(param3Str, 16));
                    break;
                case 0x88: // 开始巡航
                    codeForTour.setCode(5);
                    break;
                default:
                    return null;
            }
            String param1Str = cmdStr.substring(8, 10);
            codeForTour.setTourId(Integer.parseInt(param1Str, 16));
            String param2Str = cmdStr.substring(10, 12);
            codeForTour.setPresetId(Integer.parseInt(param2Str, 16));
            return codeForTour;
        }else if (cmdCode < 138) {
            // 扫描指令
            FrontEndControlCodeForScan  controlCodeForScan = new FrontEndControlCodeForScan();
            String param2Str = cmdStr.substring(10, 11);
            int param2Code = Integer.parseInt(param2Str, 16);
            switch (cmdCode) {
                case 0x89:
                    switch (param2Code) {
                        case 0x00: // 开始自动扫描
                            controlCodeForScan.setCode(1);
                            break;
                        case 0x01: // 设置自动扫描左边界
                            controlCodeForScan.setCode(2);
                            break;
                        case 0x02: // 设置自动扫描右边界
                            controlCodeForScan.setCode(3);
                            break;
                    }
                    break;
                case 0x8A: // 删除一个巡航点
                    controlCodeForScan.setCode(4);
                    String param3Str = cmdStr.substring(12, 13);
                    controlCodeForScan.setScanSpeed(Integer.parseInt(param3Str, 16));
                    break;
                default:
                    return null;
            }
            String param1Str = cmdStr.substring(8, 10);
            controlCodeForScan.setScanId(Integer.parseInt(param1Str, 16));
            return controlCodeForScan;
        }else if (cmdCode < 141) {
            // 辅助开关
            FrontEndControlCodeForAuxiliary  codeForAuxiliary = new FrontEndControlCodeForAuxiliary();
            switch (cmdCode) {
                case 0x8C: // 开
                    codeForAuxiliary.setCode(1);
                    break;
                case 0x8D: // 关
                    codeForAuxiliary.setCode(2);
                    break;
                default:
                    return null;
            }
            // 预置位编号
            String param2Str = cmdStr.substring(10, 12);
            codeForAuxiliary.setAuxiliaryId(Integer.parseInt(param2Str, 16));
            return codeForAuxiliary;
        }else {
            return null;
        }
    }
}
