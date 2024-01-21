package com.genersoft.iot.vmp.service;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxy;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface IStreamProxyPlayService {


    void startProxy(StreamProxy streamProxy, MediaServerItem mediaInfo, GeneralCallback<StreamInfo> callback);


    void stopProxy(StreamProxy streamProxy, MediaServerItem mediaInfo, GeneralCallback<StreamInfo> callback);
}
