package com.genersoft.iot.vmp.conf.ftpServer;

import org.apache.ftpserver.ftplet.*;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class FtpPlet implements Ftplet {

    private FtpletContext ftpletContext;

    @Override
    public void init(FtpletContext ftpletContext) throws FtpException {
        this.ftpletContext = ftpletContext;
        System.out.println("ftp-init");
    }

    @Override
    public void destroy() {

    }

    @Override
    public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
        System.out.println("ftp-beforeCommand");
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException {
        System.out.println("ftp-afterCommand");
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
        System.out.println("ftp-onConnect");
        System.out.println(session.getSessionId());
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
        System.out.println("ftp-session");
        System.out.println(session.getSessionId());
        return FtpletResult.DEFAULT;
    }
}
