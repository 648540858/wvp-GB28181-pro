package com.genersoft.iot.vmp.conf.ftpServer;

import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FtpFileSystemFactory implements FileSystemFactory {

    private final Map<String, OutputStream> outputStreamMap = new ConcurrentHashMap<>();

    @Override
    public FileSystemView createFileSystemView(User user) throws FtpException {
        return new FtpFileSystemView(user, path -> {
            return outputStreamMap.get(path);
        });
    }

    public void addOutputStream(String filePath, OutputStream outputStream) {
        outputStreamMap.put(filePath, outputStream);
    }

    public void removeOutputStream(String filePath) {
        outputStreamMap.remove(filePath);
    }
}
