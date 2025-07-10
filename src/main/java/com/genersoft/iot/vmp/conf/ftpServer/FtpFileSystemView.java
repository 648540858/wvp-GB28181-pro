package com.genersoft.iot.vmp.conf.ftpServer;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import java.io.OutputStream;

public class FtpFileSystemView implements FileSystemView {

    private User user;

    private FileCallback fileCallback;

    public FtpFileSystemView(User user, FileCallback fileCallback) {
        this.user = user;
        this.fileCallback = fileCallback;
    }

    public static String HOME_PATH = "root";

    public FtpFile workDir = VirtualFtpFile.getDir(HOME_PATH);

    @Override
    public FtpFile getHomeDirectory() throws FtpException {
        return VirtualFtpFile.getDir(HOME_PATH);
    }

    @Override
    public FtpFile getWorkingDirectory() throws FtpException {
        return workDir;
    }

    @Override
    public boolean changeWorkingDirectory(String dir) throws FtpException {
        workDir = VirtualFtpFile.getDir(dir);
        return true;
    }

    @Override
    public FtpFile getFile(String file) throws FtpException {
        VirtualFtpFile ftpFile = VirtualFtpFile.getFile(file);
        if (fileCallback != null) {
            OutputStream outputStream = fileCallback.run(workDir.getName());
            if (outputStream != null) {
                ftpFile.setOutputStream(outputStream);
            }
        }
        return ftpFile;
    }

    @Override
    public boolean isRandomAccessible() throws FtpException {
        return true;
    }

    @Override
    public void dispose() {
    }


}
