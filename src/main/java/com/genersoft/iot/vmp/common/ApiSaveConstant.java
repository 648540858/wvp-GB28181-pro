package com.genersoft.iot.vmp.common;

/**
 * 为API重命名, 方便向数据库记录数据的时候展示
 * @author lin
 */
public class ApiSaveConstant {

    public static String getVal(String key) {
        String[] keyItemArray = key.split("/");
        if (keyItemArray.length <= 1 || !"api".equals(keyItemArray[1])) {
            return null;
        }
        if (keyItemArray.length >= 4) {
            switch (keyItemArray[2]) {
                case "alarm":
                    if ("delete".equals(keyItemArray[3])) {
                        return "删除报警";
                    }
                    break;
                case "device":
                    switch (keyItemArray[3]) {
                        case "config":
                            if (keyItemArray.length >= 5 && "basicParam".equals(keyItemArray[4])) {
                                return "[设备配置] 基本配置设置命令";
                            }
                            break;
                        case "control":
                            switch (keyItemArray[4]) {
                                case "teleboot":
                                    return "[设备控制] 远程启动";
                                case "record":
                                    return "[设备控制] 录像控制";
                                case "guard":
                                    return "[设备控制] 布防/撤防命令";
                                case "reset_alarm":
                                    return "[设备控制] 报警复位";
                                case "i_frame":
                                    return "[设备控制] 强制关键帧";
                                case "home_position":
                                    return "[设备控制] 看守位控制";
                                default:
                                    return "";
                            }
                            case "query":
                                if (keyItemArray.length <= 5) {
                                    return null;
                                }
                                switch (keyItemArray[4]) {
                                    case "devices":
                                        if (keyItemArray.length < 7) {
                                            return null;
                                        }
                                        switch (keyItemArray[6]) {
                                            case "sync":
                                                return "[设备查询] 同步设备通道";
                                            case "delete":
                                                return "[设备查询] 移除设备";
                                            default:
                                                return "";
                                        }
                                    case "channel":
                                        return "[设备查询] 更新通道信息";
                                    case "transport":
                                        return "[设备查询] 修改数据流传输模式";
                                    default:
                                        return "";
                                }
                        default:
                            return "";
                            }

                    break;
                case "gbStream":
                    switch (keyItemArray[3]) {
                        case "del":
                            return "移除通道与国标的关联";
                        case "add":
                            return "添加通道与国标的关联";
                        default:
                            return "";
                    }
                case "media":
                    break;
                case "position":
                    if ("subscribe".equals(keyItemArray[3])) {
                        return "订阅位置信息";
                    }
                    break;
                case "platform":
                    switch (keyItemArray[3]) {
                        case "save":
                            return "添加上级平台";
                        case "delete":
                            return "移除上级平台";
                        case "update_channel_for_gb":
                            return "向上级平台添加国标通道";
                        case "del_channel_for_gb":
                            return "从上级平台移除国标通道";
                        default:
                            return "";
                    }
                case "platform_gb_stream":
                    break;
                case "play":
                    switch (keyItemArray[3]) {
                        case "start":
                            return "开始点播";
                        case "stop":
                            return "停止点播";
                        case "convert":
                            return "转码";
                        case "convertStop":
                            return "结束转码";
                        case "broadcast":
                            return "语音广播";
                        default:
                            return "";
                    }
                case "download":
                    switch (keyItemArray[3]) {
                        case "start":
                            return "开始历史媒体下载";
                        case "stop":
                            return "停止历史媒体下载";
                        default:
                            return "";
                    }
                case "playback":
                    switch (keyItemArray[3]) {
                        case "start":
                            return "开始视频回放";
                        case "stop":
                            return "停止视频回放";
                        default:
                            return "";
                    }
                case "ptz":
                    switch (keyItemArray[3]) {
                        case "control":
                            return "云台控制";
                        case "front_end_command":
                            return "通用前端控制命令";
                        default:
                            return "";
                    }
                case "gb_record":
                    break;
                case "onvif":
                    break;
                case "server":
                    if ("restart".equals(keyItemArray[3])) {
                        return "重启流媒体服务";
                    }
                    break;
                case "proxy":
                    switch (keyItemArray[3]) {
                        case "save":
                            return "保存代理";
                        case "del":
                            return "移除代理";
                        case "start":
                            return "启用代理";
                        case "stop":
                            return "停用代理";
                        default:
                            return "";
                    }
                case "push":
                    switch (keyItemArray[3]) {
                        case "save_to_gb":
                            return "将推流添加到国标";
                        case "remove_form_gb":
                            return "将推流移出到国标";
                        default:
                            return "";
                    }
                case "user":
                    switch (keyItemArray[3]) {
                        case "login":
                            return "登录";
                        case "changePassword":
                            return "修改密码";
                        case "add":
                            return "添加用户";
                        case "delete":
                            return "删除用户";
                        default:
                            return "";
                    }
                default:
                    return "";
            }
        }
        return null;
    }
}

