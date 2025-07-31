package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IPTZService;
import com.genersoft.iot.vmp.gb28181.service.ISourcePTZService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service(ChannelDataType.PTZ_SERVICE + ChannelDataType.GB28181)
public class SourcePTZServiceForGbImpl implements ISourcePTZService {

    @Autowired
    private IPTZService ptzService;

    @Override
    public void ptz(CommonGBChannel channel, FrontEndControlCodeForPTZ frontEndControlCode, ErrorCallback<String> callback) {
        try {
            int cmdCode = 0;
            int panSpeed = 0;
            int titleSpeed = 0;
            int zoomSpeed = 0;
            if (frontEndControlCode != null) {
                if (frontEndControlCode.getPan() != null) {
                    if (frontEndControlCode.getPan() == 0) {
                        cmdCode = cmdCode | 1 << 1;
                    } else if (frontEndControlCode.getPan() == 1) {
                        cmdCode = cmdCode | 1;
                    }
                }
                if (frontEndControlCode.getTilt() != null) {
                    if (frontEndControlCode.getTilt() == 0) {
                        cmdCode = cmdCode | 1 << 3;
                    } else if (frontEndControlCode.getTilt() == 1) {
                        cmdCode = cmdCode | 1 << 2;
                    }
                }

                if (frontEndControlCode.getZoom() != null) {
                    if (frontEndControlCode.getZoom() == 0) {
                        cmdCode = cmdCode | 1 << 5;
                    } else if (frontEndControlCode.getZoom() == 1) {
                        cmdCode = cmdCode | 1 << 4;
                    }
                }
                if (frontEndControlCode.getPanSpeed() == null) {
                    callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                    return;
                }
                if (frontEndControlCode.getTiltSpeed() == null) {
                    callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                    return;
                }
                if (frontEndControlCode.getZoomSpeed() == null) {
                    callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                    return;
                }
                panSpeed = (int)(frontEndControlCode.getPanSpeed()/100D* 255);
                titleSpeed = (int)(frontEndControlCode.getTiltSpeed()/100D* 255);;
                zoomSpeed = (int)(frontEndControlCode.getZoomSpeed()/100D* 16);
            }
            ptzService.frontEndCommand(channel, cmdCode, panSpeed, titleSpeed, zoomSpeed);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
        }catch (Exception e) {
            log.error("[云台控制失败] ", e);
            callback.run(ErrorCode.ERROR100.getCode(), e.getMessage(), null);
        }
    }

    @Override
    public void preset(CommonGBChannel channel, FrontEndControlCodeForPreset frontEndControlCode, ErrorCallback<String> callback) {
        try {
            int cmdCode = 0;
            int parameter1 = 0;
            int parameter2 = 0;
            int parameter3 = 0;
            if (frontEndControlCode != null) {
                if (frontEndControlCode.getCode() != null) {
                    if (frontEndControlCode.getCode() == 1) {
                        cmdCode = 0x81;
                    } else if (frontEndControlCode.getCode() == 2) {
                        cmdCode = 0x82;
                    }else if (frontEndControlCode.getCode() == 3) {
                        cmdCode = 0x83;
                    }
                }
                if (frontEndControlCode.getPresetId() == null) {
                    callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                    return;
                }
                parameter2 = frontEndControlCode.getPresetId();
            }
            ptzService.frontEndCommand(channel, cmdCode, parameter1, parameter2, parameter3);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
        }catch (Exception e) {
            log.error("[预置位控制失败] ", e);
            callback.run(ErrorCode.ERROR100.getCode(), e.getMessage(), null);
        }
    }

    @Override
    public void fi(CommonGBChannel channel, FrontEndControlCodeForFI frontEndControlCode, ErrorCallback<String> callback) {
        try {
            int cmdCode = 1 << 6;
            int focusSpeed = 0;
            int irisSpeed = 0;
            int parameter3 = 0;
            if (frontEndControlCode != null) {
                if (frontEndControlCode.getFocus() != null) {
                    if (frontEndControlCode.getFocus() == 0) {
                        cmdCode = cmdCode | 1 << 1;
                    } else if (frontEndControlCode.getFocus() == 1) {
                        cmdCode = cmdCode | 1;
                    }else {
                        log.error("[FI失败] 未知的聚焦指令 {}", frontEndControlCode.getFocus());
                        callback.run(ErrorCode.ERROR100.getCode(), "未知的指令", null);
                    }
                }
                if (frontEndControlCode.getIris() != null) {
                    if (frontEndControlCode.getIris() == 0) {
                        cmdCode = cmdCode | 1 << 3;
                    } else if (frontEndControlCode.getIris() == 1) {
                        cmdCode = cmdCode | 1 << 2;
                    }else {
                        log.error("[FI失败] 未知的光圈指令 {}", frontEndControlCode.getIris());
                        callback.run(ErrorCode.ERROR100.getCode(), "未知的指令", null);
                    }
                }
                if (frontEndControlCode.getFocusSpeed() == null) {
                    callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                    return;
                }
                if (frontEndControlCode.getIrisSpeed() == null) {
                    callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                    return;
                }
                focusSpeed = frontEndControlCode.getFocusSpeed();
                irisSpeed = frontEndControlCode.getIrisSpeed();
            }
            ptzService.frontEndCommand(channel, cmdCode, focusSpeed, irisSpeed, parameter3);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
        }catch (Exception e) {
            log.error("[云台控制失败] ", e);
            callback.run(ErrorCode.ERROR100.getCode(), e.getMessage(), null);
        }
    }

    @Override
    public void tour(CommonGBChannel channel, FrontEndControlCodeForTour frontEndControlCode, ErrorCallback<String> callback) {
        try {
            int cmdCode = 0;
            int parameter1 = 0;
            int parameter2 = 0;
            int parameter3 = 0;
            if (frontEndControlCode != null) {
                if (frontEndControlCode.getCode() != null) {
                    if (frontEndControlCode.getCode() == 1) {
                        cmdCode = 0x84;
                        if (frontEndControlCode.getPresetId() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter2 = frontEndControlCode.getPresetId();
                    } else if (frontEndControlCode.getCode() == 2) {
                        cmdCode = 0x85;
                        if (frontEndControlCode.getPresetId() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter2 = frontEndControlCode.getPresetId();
                    }else if (frontEndControlCode.getCode() == 3) {
                        cmdCode = 0x86;
                        if (frontEndControlCode.getPresetId() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter2 = frontEndControlCode.getPresetId();
                        if (frontEndControlCode.getTourSpeed() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter3 = frontEndControlCode.getTourSpeed();
                    }else if (frontEndControlCode.getCode() == 4) {
                        cmdCode = 0x87;
                        if (frontEndControlCode.getPresetId() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter2 = frontEndControlCode.getPresetId();
                        if (frontEndControlCode.getTourTime() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter3 = frontEndControlCode.getTourTime();
                    }else if (frontEndControlCode.getCode() == 5) {
                        cmdCode = 0x88;
                    }else if (frontEndControlCode.getCode() == 6) {
                    }else {
                        log.error("[巡航控制失败] 未知的指令 {}", frontEndControlCode.getCode());
                        callback.run(ErrorCode.ERROR100.getCode(), "未知的指令", null);
                    }
                    if (frontEndControlCode.getTourId() == null) {
                        callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                        return;
                    }
                    parameter1 = frontEndControlCode.getTourId();
                }

            }
            ptzService.frontEndCommand(channel, cmdCode, parameter1, parameter2, parameter3);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
        }catch (Exception e) {
            log.error("[巡航控制失败] ", e);
            callback.run(ErrorCode.ERROR100.getCode(), e.getMessage(), null);
        }
    }

    @Override
    public void scan(CommonGBChannel channel, FrontEndControlCodeForScan frontEndControlCode, ErrorCallback<String> callback) {
        try {
            int cmdCode = 0;
            int parameter1 = 0;
            int parameter2 = 0;
            int parameter3 = 0;
            if (frontEndControlCode != null) {
                if (frontEndControlCode.getCode() != null) {
                    if (frontEndControlCode.getCode() == 1) {
                        cmdCode = 0x89;
                        if (frontEndControlCode.getScanId() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter1 = frontEndControlCode.getScanId();
                    } else if (frontEndControlCode.getCode() == 2) {
                        cmdCode = 0x89;
                        if (frontEndControlCode.getScanId() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter1 = frontEndControlCode.getScanId();
                        parameter2 = 1;
                    }else if (frontEndControlCode.getCode() == 3) {
                        cmdCode = 0x89;
                        if (frontEndControlCode.getScanId() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter1 = frontEndControlCode.getScanId();
                        parameter2 = 2;
                    }else if (frontEndControlCode.getCode() == 4) {
                        cmdCode = 0x8A;
                        if (frontEndControlCode.getScanId() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        if (frontEndControlCode.getScanSpeed() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter1 = frontEndControlCode.getScanId();
                        parameter2 = frontEndControlCode.getScanSpeed();
                    }else if (frontEndControlCode.getCode() == 5) {
                    }else {
                        log.error("[巡航控制失败] 未知的指令 {}", frontEndControlCode.getCode());
                        callback.run(ErrorCode.ERROR100.getCode(), "未知的指令", null);
                    }
                }
            }
            ptzService.frontEndCommand(channel, cmdCode, parameter1, parameter2, parameter3);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
        }catch (Exception e) {
            log.error("[巡航控制失败] ", e);
            callback.run(ErrorCode.ERROR100.getCode(), e.getMessage(), null);
        }
    }

    @Override
    public void auxiliary(CommonGBChannel channel, FrontEndControlCodeForAuxiliary frontEndControlCode, ErrorCallback<String> callback) {
        try {
            int cmdCode = 0;
            int parameter1 = 0;
            int parameter2 = 0;
            int parameter3 = 0;
            if (frontEndControlCode != null) {
                if (frontEndControlCode.getCode() != null) {
                    if (frontEndControlCode.getCode() == 1) {
                        cmdCode = 0x8C;
                        if (frontEndControlCode.getAuxiliaryId() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter1 = frontEndControlCode.getAuxiliaryId();
                    } else if (frontEndControlCode.getCode() == 2) {
                        cmdCode = 0x8D;
                        if (frontEndControlCode.getAuxiliaryId() == null) {
                            callback.run(ErrorCode.ERROR100.getCode(), "参数异常", null);
                            return;
                        }
                        parameter1 = frontEndControlCode.getAuxiliaryId();
                    }else {
                        log.error("[辅助开关失败] 未知的指令 {}", frontEndControlCode.getCode());
                        callback.run(ErrorCode.ERROR100.getCode(), "未知的指令", null);
                    }
                }
            }
            ptzService.frontEndCommand(channel, cmdCode, parameter1, parameter2, parameter3);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
        }catch (Exception e) {
            log.error("[辅助开关失败] ", e);
            callback.run(ErrorCode.ERROR100.getCode(), e.getMessage(), null);
        }
    }

    @Override
    public void wiper(CommonGBChannel channel, FrontEndControlCodeForWiper frontEndControlCode, ErrorCallback<String> callback) {
        try {
            int cmdCode = 0;
            int parameter1 = 1;
            int parameter2 = 0;
            int parameter3 = 0;
            if (frontEndControlCode != null) {
                if (frontEndControlCode.getCode() != null) {
                    if (frontEndControlCode.getCode() == 1) {
                        cmdCode = 0x8C;
                    } else if (frontEndControlCode.getCode() == 2) {
                        cmdCode = 0x8D;
                    }else {
                        log.error("[雨刷开关失败] 未知的指令 {}", frontEndControlCode.getCode());
                        callback.run(ErrorCode.ERROR100.getCode(), "未知的指令", null);
                    }
                }
            }
            ptzService.frontEndCommand(channel, cmdCode, parameter1, parameter2, parameter3);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
        }catch (Exception e) {
            log.error("[雨刷开关失败] ", e);
            callback.run(ErrorCode.ERROR100.getCode(), e.getMessage(), null);
        }
    }

    @Override
    public void queryPreset(CommonGBChannel channel, ErrorCallback<List<Preset>> callback) {
        ptzService.queryPresetList(channel, callback);
    }
}
