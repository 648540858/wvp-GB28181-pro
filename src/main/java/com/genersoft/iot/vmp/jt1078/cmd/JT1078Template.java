package com.genersoft.iot.vmp.jt1078.cmd;

import com.genersoft.iot.vmp.jt1078.proc.entity.Cmd;
import com.genersoft.iot.vmp.jt1078.proc.response.*;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:58
 * @email qingtaij@163.com
 */
@Component
public class JT1078Template {

    private final Random random = new Random();

    private static final String H9101 = "9101";
    private static final String H9102 = "9102";
    private static final String H9201 = "9201";
    private static final String H9202 = "9202";
    private static final String H9205 = "9205";
    private static final String H9206 = "9206";
    private static final String H9207 = "9207";
    private static final String H9301 = "9301";
    private static final String H9302 = "9302";
    private static final String H9303 = "9303";
    private static final String H9304 = "9304";
    private static final String H9305 = "9305";

    private static final String H0001 = "0001";
    private static final String H1205 = "1205";

    /**
     * 开启直播视频
     *
     * @param devId 设备号
     * @param j9101 开启视频参数
     */
    public String startLive(String devId, J9101 j9101, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9101)
                .setRespId(H0001)
                .setRs(j9101)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 关闭直播视频
     *
     * @param devId 设备号
     * @param j9102 关闭视频参数
     */
    public String stopLive(String devId, J9102 j9102, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9102)
                .setRespId(H0001)
                .setRs(j9102)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 查询音视频列表
     *
     * @param devId 设备号
     * @param j9205 查询音视频列表
     */
    public String queryBackTime(String devId, J9205 j9205, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9205)
                .setRespId(H1205)
                .setRs(j9205)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 开启视频回放
     *
     * @param devId 设备号
     * @param j9201 视频回放参数
     */
    public String startBackLive(String devId, J9201 j9201, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9201)
                .setRespId(H1205)
                .setRs(j9201)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 视频回放控制
     *
     * @param devId 设备号
     * @param j9202 控制视频回放参数
     */
    public String controlBackLive(String devId, J9202 j9202, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9202)
                .setRespId(H0001)
                .setRs(j9202)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 文件上传
     *
     * @param devId 设备号
     * @param j9206 文件上传参数
     */
    public String fileUpload(String devId, J9206 j9206, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9206)
                .setRespId(H0001)
                .setRs(j9206)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 文件上传控制
     *
     * @param devId 设备号
     * @param j9207 文件上传控制参数
     */
    public String fileUploadControl(String devId, J9207 j9207, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9207)
                .setRespId(H0001)
                .setRs(j9207)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 云台控制指令-云台旋转
     *
     * @param devId 设备号
     * @param j9301 云台旋转参数
     */
    public String ptzRotate(String devId, J9301 j9301, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9301)
                .setRespId(H0001)
                .setRs(j9301)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 云台控制指令-云台调整焦距控制
     *
     * @param devId 设备号
     * @param j9302 云台焦距控制参数
     */
    public String ptzZoom(String devId, J9302 j9302, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9302)
                .setRespId(H0001)
                .setRs(j9302)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 云台控制指令-云台调整光圈控制
     *
     * @param devId 设备号
     * @param j9303 云台光圈控制参数
     */
    public String ptzIris(String devId, J9303 j9303, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9303)
                .setRespId(H0001)
                .setRs(j9303)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 云台控制指令-云台雨刷控制
     *
     * @param devId 设备号
     * @param j9304 云台雨刷控制参数
     */
    public String ptzWiper(String devId, J9304 j9304, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9304)
                .setRespId(H0001)
                .setRs(j9304)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 云台控制指令-红外补光控制
     *
     * @param devId 设备号
     * @param j9305 云台红外补光控制参数
     */
    public String ptzSupplementaryLight(String devId, J9305 j9305, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9305)
                .setRespId(H0001)
                .setRs(j9305)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    private Long randomInt() {
        return (long) random.nextInt(1000) + 1;
    }
}
