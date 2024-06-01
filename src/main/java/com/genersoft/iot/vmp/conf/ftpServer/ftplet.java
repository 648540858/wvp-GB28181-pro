package com.genersoft.iot.vmp.conf.ftpServer;

import com.genersoft.iot.vmp.jt1078.event.FtpUploadEvent;
import org.apache.ftpserver.ftplet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class ftplet extends DefaultFtplet {

    private final Logger logger = LoggerFactory.getLogger(ftplet.class);

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
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        FtpUploadEvent event = new FtpUploadEvent(this);
        event.setFileName(session.getFileSystemView().getFile(request.getArgument()).getAbsolutePath());
        applicationEventPublisher.publishEvent(event);

        logger.info("[文件已上传]: {}", session.getFileSystemView().getFile(request.getArgument()).getAbsolutePath());
        return super.onUploadEnd(session, request);
    }

    @Override
    public FtpletResult onAppendEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        FtpUploadEvent event = new FtpUploadEvent(this);
        event.setFileName(session.getFileSystemView().getFile(request.getArgument()).getAbsolutePath());
        applicationEventPublisher.publishEvent(event);

        logger.info("[文件已上传]: {}", session.getFileSystemView().getFile(request.getArgument()).getAbsolutePath());
        return super.onUploadEnd(session, request);
    }

    @Override
    public FtpletResult onDownloadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return super.onDownloadStart(session, request);
    }
}
