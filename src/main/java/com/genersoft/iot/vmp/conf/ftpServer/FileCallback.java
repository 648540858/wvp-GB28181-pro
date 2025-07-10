package com.genersoft.iot.vmp.conf.ftpServer;

import java.io.OutputStream;

public interface FileCallback {

    OutputStream run(String path);
}
