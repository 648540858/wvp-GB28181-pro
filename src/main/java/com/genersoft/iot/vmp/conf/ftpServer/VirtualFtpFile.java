package com.genersoft.iot.vmp.conf.ftpServer;

import lombok.Setter;
import org.apache.ftpserver.ftplet.FtpFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class VirtualFtpFile implements FtpFile {

    @Setter
    private String name;

    @Setter
    private boolean hidden = false;

    @Setter
    private boolean directory = false;

    @Setter
    private String ownerName;

    private Long lastModified = null;

    @Setter
    private long size = 0;

    @Setter
    private OutputStream outputStream;

    public static VirtualFtpFile getFile(String name) {
        VirtualFtpFile virtualFtpFile = new VirtualFtpFile();
        virtualFtpFile.setName(name);
        return virtualFtpFile;
    }

    public static VirtualFtpFile getDir(String name) {
        if (name.endsWith("/")) {
            name = name.replaceAll("/", "");
        }
        VirtualFtpFile virtualFtpFile = new VirtualFtpFile();
        virtualFtpFile.setName(name);
        virtualFtpFile.setDirectory(true);
        return virtualFtpFile;
    }

    @Override
    public String getAbsolutePath() {
        return FtpFileSystemView.HOME_PATH + "/" + name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public boolean isDirectory() {
        return directory;
    }

    @Override
    public boolean isFile() {
        return !directory;
    }

    @Override
    public boolean doesExist() {
        return false;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isRemovable() {
        return true;
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public String getGroupName() {
        return "root";
    }

    @Override
    public int getLinkCount() {
        return 0;
    }

    @Override
    public long getLastModified() {
        if (lastModified == null) {
            lastModified = System.currentTimeMillis();
        }
        return lastModified;
    }

    @Override
    public boolean setLastModified(long time) {
        lastModified = time;
        return true;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public Object getPhysicalFile() {
        System.err.println("getPhysicalFile");
        return null;
    }

    @Override
    public boolean mkdir() {
        return true;
    }

    @Override
    public boolean delete() {
        return true;
    }

    @Override
    public boolean move(FtpFile destination) {
        this.name = destination.getName();
        return true;
    }

    @Override
    public List<? extends FtpFile> listFiles() {
        return Collections.emptyList();
    }

    @Override
    public OutputStream createOutputStream(long offset) throws IOException {
        System.out.println("createOutputStream++++");
        return outputStream;
    }

    @Override
    public InputStream createInputStream(long offset) throws IOException {
        System.out.println("createInputStream----");
        return null;
    }
}
