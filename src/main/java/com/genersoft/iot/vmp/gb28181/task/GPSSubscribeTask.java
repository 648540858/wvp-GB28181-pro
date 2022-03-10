package com.genersoft.iot.vmp.gb28181.task;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeHolder;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeInfo;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

import java.text.SimpleDateFormat;
import java.util.List;

public class GPSSubscribeTask implements Runnable{

    private IRedisCatchStorage redisCatchStorage;
    private IVideoManagerStorager storager;
    private ISIPCommanderForPlatform sipCommanderForPlatform;
    private SubscribeHolder subscribeHolder;
    private String platformId;
    private String sn;
    private String key;

    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public GPSSubscribeTask(IRedisCatchStorage redisCatchStorage, ISIPCommanderForPlatform sipCommanderForPlatform, IVideoManagerStorager storager, String platformId, String sn, String key, SubscribeHolder subscribeInfo) {
        this.redisCatchStorage = redisCatchStorage;
        this.storager = storager;
        this.platformId = platformId;
        this.sn = sn;
        this.key = key;
        this.sipCommanderForPlatform = sipCommanderForPlatform;
        this.subscribeHolder = subscribeInfo;
    }

    @Override
    public void run() {

        SubscribeInfo subscribe = subscribeHolder.getMobilePositionSubscribe(platformId);

        if (subscribe != null) {
            ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformId);
            if (parentPlatform == null || parentPlatform.isStatus()) {
                // TODO 暂时只处理视频流的回复,后续增加对国标设备的支持
                List<GbStream> gbStreams = storager.queryGbStreamListInPlatform(platformId);
                if (gbStreams.size() > 0) {
                    for (GbStream gbStream : gbStreams) {
                        String gbId = gbStream.getGbId();
                        GPSMsgInfo gpsMsgInfo = redisCatchStorage.getGpsMsgInfo(gbId);
                        if (gbStream.isStatus()) {
                            if (gpsMsgInfo != null) {
                                // 发送GPS消息
                                sipCommanderForPlatform.sendNotifyMobilePosition(parentPlatform, gpsMsgInfo, subscribe);
                            }else {
                                // 没有在redis找到新的消息就使用数据库的消息
                                gpsMsgInfo = new GPSMsgInfo();
                                gpsMsgInfo.setId(gbId);
                                gpsMsgInfo.setLat(gbStream.getLongitude());
                                gpsMsgInfo.setLng(gbStream.getLongitude());
                                // 发送GPS消息
                                sipCommanderForPlatform.sendNotifyMobilePosition(parentPlatform, gpsMsgInfo, subscribe);
                            }
                        }

                    }
                }
            }
        }
    }
}
