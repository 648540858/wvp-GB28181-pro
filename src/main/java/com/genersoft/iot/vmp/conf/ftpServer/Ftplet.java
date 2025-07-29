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
public class Ftplet extends DefaultFtplet {

    private final Logger logger = LoggerFactory.getLogger(Ftplet.class);

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        FtpFile file = session.getFileSystemView().getFile(request.getArgument());
        if (file == null) {
            return super.onUploadEnd(session, request);
        }
        sendEvent(file.getAbsolutePath());
        return super.onUploadUniqueEnd(session, request);
    }

    @Override
    public FtpletResult onAppendEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        FtpFile file = session.getFileSystemView().getFile(request.getArgument());
        if (file == null) {
            return super.onUploadEnd(session, request);
        }
        sendEvent(file.getAbsolutePath());
        return super.onUploadUniqueEnd(session, request);
    }

    @Override
    public FtpletResult onUploadUniqueEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        FtpFile file = session.getFileSystemView().getFile(request.getArgument());
        if (file == null) {
            return super.onUploadEnd(session, request);
        }
        sendEvent(file.getAbsolutePath());
        return super.onUploadUniqueEnd(session, request);
    }

    private void sendEvent(String filePath){
        FtpUploadEvent event = new FtpUploadEvent(this);
        logger.info("[文件已上传]: {}", filePath);
        event.setFileName(filePath);
        applicationEventPublisher.publishEvent(event);
    }
}
