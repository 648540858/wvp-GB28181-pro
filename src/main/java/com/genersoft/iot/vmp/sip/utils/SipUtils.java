package com.genersoft.iot.vmp.sip.utils;

import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.Utils;
import org.springframework.util.ObjectUtils;

import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.header.UserAgentHeader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SipUtils {

    public static int getRandomTcpPort(){
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            int localPort = serverSocket.getLocalPort();
            serverSocket.close();
            return localPort;
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getRandomUdpPort(){
        try {
            DatagramSocket datagramSocket = new DatagramSocket(0);
            int localPort = datagramSocket.getLocalPort();
            datagramSocket.close();
            return localPort;
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getNewCallId(SipProviderImpl sipProvider) {
        return Utils.getInstance().generateCallIdentifier(sipProvider.getListeningPoint()
                .getIPAddress());

    }

    public static UserAgentHeader createUserAgentHeader(GitUtil gitUtil) throws PeerUnavailableException, ParseException {
        List<String> agentParam = new ArrayList<>();
        agentParam.add("WVP-Pro ");
        if (gitUtil != null ) {
            if (!ObjectUtils.isEmpty(gitUtil.getBuildVersion())) {
                agentParam.add("v");
                agentParam.add(gitUtil.getBuildVersion() + ".");
            }
            if (!ObjectUtils.isEmpty(gitUtil.getCommitTime())) {
                agentParam.add(gitUtil.getCommitTime());
            }
        }
        return SipFactory.getInstance().createHeaderFactory().createUserAgentHeader(agentParam);
    }

    private static long cseq = 0L;

    public static long getCSEQ() {
        return cseq++;
    }

    public static  String getNewViaTag() {
        return "z9hG4bK" + System.currentTimeMillis();
    }

    public static String getNewFromTag(){
        return UUID.randomUUID().toString().replace("-", "");

//        return getNewTag();
    }

    public static String getNewTag(){
        return String.valueOf(System.currentTimeMillis());
    }
}
