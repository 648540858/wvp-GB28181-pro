package com.genersoft.iot.vmp.onvif;

import be.teletask.onvif.models.OnvifDevice;
import com.genersoft.iot.vmp.onvif.dto.ONVIFCallBack;

import java.util.List;

public interface IONVIFServer {

    void search(int timeout, ONVIFCallBack<List<String>> callBack);

    void getRTSPUrl(int timeout, OnvifDevice device, ONVIFCallBack<String> callBack);
}
