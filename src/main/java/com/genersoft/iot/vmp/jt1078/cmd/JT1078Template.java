package com.genersoft.iot.vmp.jt1078.cmd;

import com.genersoft.iot.vmp.jt1078.proc.entity.Cmd;
import com.genersoft.iot.vmp.jt1078.proc.response.*;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;

import java.util.Random;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:58
 * @email qingtaij@163.com
 */
public class JT1078Template {

    private final Random random = new Random();

    private static final String H9101 = "9101";
    private static final String H9102 = "9102";
    private static final String H9201 = "9201";
    private static final String H9202 = "9202";
    private static final String H9205 = "9205";

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

    private Long randomInt() {
        return (long) random.nextInt(1000) + 1;
    }
}
