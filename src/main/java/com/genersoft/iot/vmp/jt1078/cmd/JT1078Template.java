package com.genersoft.iot.vmp.jt1078.cmd;

import com.genersoft.iot.vmp.jt1078.proc.entity.Cmd;
import com.genersoft.iot.vmp.jt1078.proc.response.J9101;
import com.genersoft.iot.vmp.jt1078.proc.response.J9102;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;

import java.util.Random;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:58
 * @email qingtaij@163.com
 */
public class JT1078Template {

    private final Random random = new Random();

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
                .setMsgId("9101")
                .setRespId("0001")
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
                .setMsgId("9102")
                .setRespId("0001")
                .setRs(j9102)
                .build();
        return SessionManager.INSTANCE.request(cmd, timeOut);
    }

    private Long randomInt() {
        return (long) random.nextInt(1000) + 1;
    }
}
