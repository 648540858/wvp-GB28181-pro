package com.genersoft.iot.vmp.service.bean;

/**
 * 全局错误码
 */
public enum InviteErrorCode {
    SUCCESS(0, "成功"),
    FAIL(-100, "失败"),
    ERROR_FOR_SIGNALLING_TIMEOUT(-1, "信令超时"),
    ERROR_FOR_STREAM_TIMEOUT(-2, "收流超时"),
    ERROR_FOR_RESOURCE_EXHAUSTION(-3, "资源耗尽"),
    ERROR_FOR_CATCH_DATA(-4, "缓存数据异常"),
    ERROR_FOR_SIGNALLING_ERROR(-5, "收到信令错误"),
    ERROR_FOR_STREAM_PARSING_EXCEPTIONS(-6, "流地址解析错误"),
    ERROR_FOR_SDP_PARSING_EXCEPTIONS(-7, "SDP信息解析失败"),
    ERROR_FOR_SSRC_UNAVAILABLE(-8, "SSRC不可用"),
    ERROR_FOR_RESET_SSRC(-9, "重新设置收流信息失败"),
    ERROR_FOR_SIP_SENDING_FAILED(-10, "命令发送失败"),
    ERROR_FOR_ASSIST_NOT_READY(-11, "没有可用的assist服务"),
    ERROR_FOR_PARAMETER_ERROR(-13, "参数异常");

    private final int code;
    private final String msg;

    InviteErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
