package com.genersoft.iot.vmp.jt1078;

import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.codec.netty.TcpServer;
import com.genersoft.iot.vmp.jt1078.proc.response.J9102;
import com.genersoft.iot.vmp.jt1078.proc.response.J9201;
import com.genersoft.iot.vmp.jt1078.proc.response.J9202;
import com.genersoft.iot.vmp.jt1078.proc.response.J9205;

import java.util.Scanner;

/**
 * @author QingtaiJiang
 * @date 2023/4/28 14:22
 * @email qingtaij@163.com
 */
public class JT1078ServerTest {

    private static final JT1078Template jt1078Template = new JT1078Template();

    public static void main(String[] args) {
        System.out.println("Starting jt1078 server...");
        TcpServer tcpServer = new TcpServer(21078);
        tcpServer.start();
        System.out.println("Start jt1078 server success!");


        Scanner s = new Scanner(System.in);
        while (true) {
            String code = s.nextLine();
            switch (code) {
                case "1":
                    test9102();
                    break;
                case "2":
                    test9201();
                    break;
                case "3":
                    test9202();
                    break;
                case "4":
                    test9205();
                    break;
                default:
                    break;
            }
        }
    }

    private static void test9102() {
        J9102 j9102 = new J9102();
        j9102.setChannel(1);
        j9102.setCommand(0);
        j9102.setCloseType(0);
        j9102.setStreamType(0);

        String s = jt1078Template.stopLive("18864197066", j9102, 6);
        System.out.println(s);
    }

    private static void test9201() {
        J9201 j9201 = new J9201();
        j9201.setIp("192.168.1.1");
        j9201.setChannel(1);
        j9201.setTcpPort(7618);
        j9201.setUdpPort(7618);
        j9201.setType(0);
        j9201.setRate(0);
        j9201.setStorageType(0);
        j9201.setPlaybackType(0);
        j9201.setPlaybackSpeed(0);
        j9201.setStartTime("230428134100");
        j9201.setEndTime("230428134200");

        String s = jt1078Template.startBackLive("18864197066", j9201, 6);
        System.out.println(s);
    }

    private static void test9202() {
        J9202 j9202 = new J9202();

        j9202.setChannel(1);
        j9202.setPlaybackType(2);
        j9202.setPlaybackSpeed(0);
        j9202.setPlaybackTime("230428134100");

        String s = jt1078Template.controlBackLive("18864197066", j9202, 6);
        System.out.println(s);
    }

    private static void test9205() {
        J9205 j9205 = new J9205();
        j9205.setChannelId(1);
        j9205.setStartTime("230428134100");
        j9205.setEndTime("230428134100");
        j9205.setMediaType(0);
        j9205.setStreamType(0);
        j9205.setStorageType(0);

        String s = jt1078Template.queryBackTime("18864197066", j9205, 6);
        System.out.println(s);
    }
}
