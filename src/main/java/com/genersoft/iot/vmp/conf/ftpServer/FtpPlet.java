package com.genersoft.iot.vmp.conf.ftpServer;

import com.genersoft.iot.vmp.jt1078.event.FtpUploadEvent;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.DefaultFtpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class FtpPlet extends DefaultFtplet {

    private FtpletContext ftpletContext;

    @Value("${ftp.username}")
    private String username;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
        if (request.getCommand().equalsIgnoreCase("USER") && !username.equals(request.getArgument())) {
            return FtpletResult.DISCONNECT;
        }
        super.beforeCommand(session, request);
//        if (request.getCommand().equalsIgnoreCase("STOR") ) {
//            FtpUploadEvent ftpUploadEvent = new FtpUploadEvent(this);
//            ftpUploadEvent.setFileName(request.getArgument());
//            applicationEventPublisher.publishEvent(ftpUploadEvent);
//        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onUploadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        DefaultFtpSession ftpSession = (DefaultFtpSession) session;
        return super.onUploadStart(session, request);
    }

    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return super.onUploadEnd(session, request);
    }

    @Override
    public FtpletResult onDownloadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return super.onDownloadStart(session, request);
    }
}
