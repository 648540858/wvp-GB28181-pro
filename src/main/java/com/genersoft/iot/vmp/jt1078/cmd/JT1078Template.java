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

    private static final String H8103 = "8103";
    private static final String H8104 = "8104";
    private static final String H8105 = "8105";
    private static final String H8106 = "8106";
    private static final String H8107 = "8107";
    private static final String H8201 = "8201";
    private static final String H8202 = "8202";
    private static final String H8203 = "8203";
    private static final String H8204 = "8204";
    private static final String H8300 = "8300";
    private static final String H8400 = "8400";
    private static final String H8401 = "8401";
    private static final String H8500 = "8500";
    private static final String H8600 = "8600";
    private static final String H8601 = "8601";
    private static final String H8602 = "8602";
    private static final String H8603 = "8603";
    private static final String H8604 = "8604";
    private static final String H8605 = "8605";
    private static final String H8606 = "8606";
    private static final String H8607 = "8607";
    private static final String H8608 = "8608";
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
    private static final String H9306 = "9306";

    private static final String H0001 = "0001";
    private static final String H0104 = "0104";
    private static final String H0107 = "0107";
    private static final String H0201 = "0201";
    private static final String H0500 = "0500";
    private static final String H0608 = "0608";
    private static final String H1205 = "1205";

    /**
     * 开启直播视频
     *
     * @param devId 设备号
     * @param j9101 开启视频参数
     */
    public Object startLive(String devId, J9101 j9101, Integer timeOut) {
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
    public Object stopLive(String devId, J9102 j9102, Integer timeOut) {
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
    public Object queryBackTime(String devId, J9205 j9205, Integer timeOut) {
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
    public Object startBackLive(String devId, J9201 j9201, Integer timeOut) {
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
    public Object controlBackLive(String devId, J9202 j9202, Integer timeOut) {
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
    public Object fileUpload(String devId, J9206 j9206, Integer timeOut) {
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
    public Object fileUploadControl(String devId, J9207 j9207, Integer timeOut) {
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
    public Object ptzRotate(String devId, J9301 j9301, Integer timeOut) {
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
    public Object ptzFocal(String devId, J9302 j9302, Integer timeOut) {
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
    public Object ptzIris(String devId, J9303 j9303, Integer timeOut) {
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
    public Object ptzWiper(String devId, J9304 j9304, Integer timeOut) {
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
    public Object ptzSupplementaryLight(String devId, J9305 j9305, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9305)
                .setRespId(H0001)
                .setRs(j9305)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 云台控制指令-变倍控制
     *
     * @param devId 设备号
     * @param j9306 云台变倍控制参数
     */
    public Object ptzZoom(String devId, J9306 j9306, Integer timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H9306)
                .setRespId(H0001)
                .setRs(j9306)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 查询终端参数
     *
     * @param devId 设备号
     */
    public Object getDeviceConfig(String devId, J8104 j8104, Integer timeOut) {

        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8104)
                .setRespId(H0104)
                .setRs(j8104)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 查询指定终端参数
     *
     * @param devId 设备号
     */
    public Object getDeviceSpecifyConfig(String devId, J8106 j8106, Integer timeOut) {

        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8106)
                .setRespId(H0104)
                .setRs(j8106)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 设置终端参数
     *
     * @param devId 设备号
     */
    public Object setDeviceSpecifyConfig(String devId, J8103 j8103, Integer timeOut) {

        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8103)
                .setRespId(H0001)
                .setRs(j8103)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    private Long randomInt() {
        return (long) random.nextInt(1000) + 1;
    }

    /**
     * 设备控制
     */
    public Object deviceControl(String devId, J8105 j8105, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8105)
                .setRespId(H0001)
                .setRs(j8105)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 查询终端属性
     */
    public Object deviceAttribute(String devId, J8107 j8107, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8107)
                .setRespId(H0107)
                .setRs(j8107)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    /**
     * 位置信息查询
     */
    public Object queryPositionInfo(String devId, J8201 j8201, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8201)
                .setRespId(H0201)
                .setRs(j8201)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object tempPositionTrackingControl(String devId, J8202 j8202, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8202)
                .setRespId(H0001)
                .setRs(j8202)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object confirmationAlarmMessage(String devId, J8203 j8203, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8203)
                .setRespId(H0001)
                .setRs(j8203)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object linkDetection(String devId, J8204 j8204, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8204)
                .setRespId(H0001)
                .setRs(j8204)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object textMessage(String devId, J8300 j8300, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8300)
                .setRespId(H0001)
                .setRs(j8300)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object telephoneCallback(String devId, J8400 j8400, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8400)
                .setRespId(H0001)
                .setRs(j8400)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object setPhoneBook(String devId, J8401 j8401, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8401)
                .setRespId(H0001)
                .setRs(j8401)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object vehicleControl(String devId, J8500 j8500, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8500)
                .setRespId(H0500)
                .setRs(j8500)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object setAreaForCircle(String devId, J8600 j8600, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8600)
                .setRespId(H0001)
                .setRs(j8600)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object deleteAreaForCircle(String devId, J8601 j8601, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8601)
                .setRespId(H0001)
                .setRs(j8601)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object setAreaForRectangle(String devId, J8602 j8602, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8602)
                .setRespId(H0001)
                .setRs(j8602)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object deleteAreaForRectangle(String devId, J8603 j8603, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8603)
                .setRespId(H0001)
                .setRs(j8603)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object setAreaForPolygon(String devId, J8604 j8604, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8604)
                .setRespId(H0001)
                .setRs(j8604)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object deleteAreaForPolygon(String devId, J8605 j8605, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8605)
                .setRespId(H0001)
                .setRs(j8605)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object setRoute(String devId, J8606 j8606, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8606)
                .setRespId(H0001)
                .setRs(j8606)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object deleteRoute(String devId, J8607 j8607, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8607)
                .setRespId(H0001)
                .setRs(j8607)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    public Object queryAreaOrRoute(String devId, J8608 j8608, int timeOut) {
        Cmd cmd = new Cmd.Builder()
                .setDevId(devId)
                .setPackageNo(randomInt())
                .setMsgId(H8608)
                .setRespId(H0608)
                .setRs(j8608)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }
}
