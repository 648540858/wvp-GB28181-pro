package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
public class ZLMRTPServerFactory {

    private Logger logger = LoggerFactory.getLogger("ZLMRTPServerFactory");

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    private int[] portRangeArray = new int[2];

    public int getFreePort(MediaServerItem mediaServerItem, int startPort, int endPort, List<Integer> usedFreelist) {
        if (endPort <= startPort) {
            return -1;
        }
        if (usedFreelist == null) {
            usedFreelist = new ArrayList<>();
        }
        JSONObject listRtpServerJsonResult = zlmresTfulUtils.listRtpServer(mediaServerItem);
        if (listRtpServerJsonResult != null) {
            JSONArray data = listRtpServerJsonResult.getJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JSONObject dataItem = data.getJSONObject(i);
                    usedFreelist.add(dataItem.getInteger("port"));
                }
            }
        }

        Map<String, Object> param = new HashMap<>();
        int result = -1;
        // 设置推流端口
        if (startPort%2 == 1) {
            startPort ++;
        }
        boolean checkPort = false;
        for (int i = startPort; i < endPort  + 1; i+=2) {
            if (!usedFreelist.contains(i)){
                checkPort = true;
                startPort = i;
                break;
            }
        }
        if (!checkPort) {
            logger.warn("未找到节点{}上范围[{}-{}]的空闲端口", mediaServerItem.getId(), startPort, endPort);
            return -1;
        }
        param.put("port", startPort);
        String stream = UUID.randomUUID().toString();
        param.put("enable_tcp", 1);
        param.put("stream_id", stream);
        param.put("port", 0);
        JSONObject openRtpServerResultJson = zlmresTfulUtils.openRtpServer(mediaServerItem, param);

        if (openRtpServerResultJson != null) {
            if (openRtpServerResultJson.getInteger("code") == 0) {
                result= openRtpServerResultJson.getInteger("port");
                Map<String, Object> closeRtpServerParam = new HashMap<>();
                closeRtpServerParam.put("stream_id", stream);
                zlmresTfulUtils.closeRtpServer(mediaServerItem, closeRtpServerParam);
            }else {
                usedFreelist.add(startPort);
                startPort +=2;
                result = getFreePort(mediaServerItem, startPort, endPort,usedFreelist);
            }
        }else {
            //  检查ZLM状态
            logger.error("创建RTP Server 失败 {}: 请检查ZLM服务", param.get("port"));
        }
        return result;
    }

    public int createRTPServer(MediaServerItem mediaServerItem, String streamId, int ssrc) {
        int result = -1;
        // 查询此rtp server 是否已经存在
        JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(mediaServerItem, streamId);
        if (rtpInfo != null && rtpInfo.getInteger("code") == 0 && rtpInfo.getBoolean("exist")) {
            result = rtpInfo.getInteger("local_port");
            return result;
        }
        Map<String, Object> param = new HashMap<>();
        // 推流端口设置0则使用随机端口
        param.put("enable_tcp", 1);
        param.put("stream_id", streamId);
        param.put("port", 0);
        param.put("ssrc", ssrc);
        JSONObject openRtpServerResultJson = zlmresTfulUtils.openRtpServer(mediaServerItem, param);

        if (openRtpServerResultJson != null) {
            if (openRtpServerResultJson.getInteger("code") == 0) {
                result= openRtpServerResultJson.getInteger("port");
            }else {
                logger.error("创建RTP Server 失败 {}: ", openRtpServerResultJson.getString("msg"));
            }
        }else {
            //  检查ZLM状态
            logger.error("创建RTP Server 失败 {}: 请检查ZLM服务", param.get("port"));
        }
        return result;
    }

    public boolean closeRTPServer(MediaServerItem serverItem, String streamId) {
        boolean result = false;
        if (serverItem !=null){
            Map<String, Object> param = new HashMap<>();
            param.put("stream_id", streamId);
            JSONObject jsonObject = zlmresTfulUtils.closeRtpServer(serverItem, param);
            if (jsonObject != null ) {
                if (jsonObject.getInteger("code") == 0) {
                    result = jsonObject.getInteger("hit") == 1;
                }else {
                    logger.error("关闭RTP Server 失败: " + jsonObject.getString("msg"));
                }
            }else {
                //  检查ZLM状态
                logger.error("关闭RTP Server 失败: 请检查ZLM服务");
            }
        }
        return result;
    }

//    private int getPortFromportRange(MediaServerItem mediaServerItem) {
//        int currentPort = mediaServerItem.getCurrentPort();
//        if (currentPort == 0) {
//            String[] portRangeStrArray = mediaServerItem.getSendRtpPortRange().split(",");
//            if (portRangeStrArray.length != 2) {
//                portRangeArray[0] = 30000;
//                portRangeArray[1] = 30500;
//            }else {
//                portRangeArray[0] = Integer.parseInt(portRangeStrArray[0]);
//                portRangeArray[1] = Integer.parseInt(portRangeStrArray[1]);
//            }
//        }
//
//        if (currentPort == 0 || currentPort++ > portRangeArray[1]) {
//            currentPort = portRangeArray[0];
//            mediaServerItem.setCurrentPort(currentPort);
//            return portRangeArray[0];
//        } else {
//            if (currentPort % 2 == 1) {
//                currentPort++;
//            }
//            currentPort++;
//            mediaServerItem.setCurrentPort(currentPort);
//            return currentPort;
//        }
//    }

    /**
     * 创建一个国标推流
     * @param ip 推流ip
     * @param port 推流端口
     * @param ssrc 推流唯一标识
     * @param platformId 平台id
     * @param channelId 通道id
     * @param tcp 是否为tcp
     * @return SendRtpItem
     */
    public SendRtpItem createSendRtpItem(MediaServerItem serverItem, String ip, int port, String ssrc, String platformId, String deviceId, String channelId, boolean tcp){

        // 使用RTPServer 功能找一个可用的端口
        String sendRtpPortRange = serverItem.getSendRtpPortRange();
        if (StringUtils.isEmpty(sendRtpPortRange)) {
            return null;
        }
        String[] portRangeStrArray = serverItem.getSendRtpPortRange().split(",");
        int localPort = -1;
        if (portRangeStrArray.length != 2) {
            localPort = getFreePort(serverItem, 30000, 30500, null);
        }else {
            localPort = getFreePort(serverItem, Integer.parseInt(portRangeStrArray[0]),  Integer.parseInt(portRangeStrArray[1]), null);
        }
        if (localPort == -1) {
            logger.error("没有可用的端口");
            return null;
        }
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setIp(ip);
        sendRtpItem.setPort(port);
        sendRtpItem.setSsrc(ssrc);
        sendRtpItem.setPlatformId(platformId);
        sendRtpItem.setDeviceId(deviceId);
        sendRtpItem.setChannelId(channelId);
        sendRtpItem.setTcp(tcp);
        sendRtpItem.setApp("rtp");
        sendRtpItem.setLocalPort(localPort);
        sendRtpItem.setMediaServerId(serverItem.getId());
        return sendRtpItem;
    }

    /**
     * 创建一个直播推流
     * @param ip 推流ip
     * @param port 推流端口
     * @param ssrc 推流唯一标识
     * @param platformId 平台id
     * @param channelId 通道id
     * @param tcp 是否为tcp
     * @return SendRtpItem
     */
    public SendRtpItem createSendRtpItem(MediaServerItem serverItem, String ip, int port, String ssrc, String platformId, String app, String stream, String channelId, boolean tcp){
        // 使用RTPServer 功能找一个可用的端口
        String sendRtpPortRange = serverItem.getSendRtpPortRange();
        if (StringUtils.isEmpty(sendRtpPortRange)) {
            return null;
        }
        String[] portRangeStrArray = serverItem.getSendRtpPortRange().split(",");
        int localPort = -1;
        if (portRangeStrArray.length != 2) {
            localPort = getFreePort(serverItem, 30000, 30500, null);
        }else {
            localPort = getFreePort(serverItem, Integer.parseInt(portRangeStrArray[0]),  Integer.parseInt(portRangeStrArray[1]), null);
        }
        if (localPort == -1) {
            logger.error("没有可用的端口");
            return null;
        }
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setIp(ip);
        sendRtpItem.setPort(port);
        sendRtpItem.setSsrc(ssrc);
        sendRtpItem.setApp(app);
        sendRtpItem.setStreamId(stream);
        sendRtpItem.setPlatformId(platformId);
        sendRtpItem.setChannelId(channelId);
        sendRtpItem.setTcp(tcp);
        sendRtpItem.setLocalPort(localPort);
        sendRtpItem.setMediaServerId(serverItem.getId());
        return sendRtpItem;
    }

    /**
     * 调用zlm RESTFUL API —— startSendRtp
     */
    public JSONObject startSendRtpStream(MediaServerItem mediaServerItem, Map<String, Object>param) {
        Boolean result = false;
        JSONObject jsonObject = zlmresTfulUtils.startSendRtp(mediaServerItem, param);
        if (jsonObject == null) {
            logger.error("RTP推流失败: 请检查ZLM服务");
        } else if (jsonObject.getInteger("code") == 0) {
            result= true;
            logger.info("RTP推流成功[ {}/{} ]，{}->{}:{}, " ,param.get("app"), param.get("stream"), jsonObject.getString("local_port"), param.get("dst_url"), param.get("dst_port"));
        } else {
            logger.error("RTP推流失败: {}, 参数：{}",jsonObject.getString("msg"),JSONObject.toJSON(param));
        }
        return jsonObject;
    }

    /**
     * 查询待转推的流是否就绪
     */
    public Boolean isRtpReady(MediaServerItem mediaServerItem, String streamId) {
        JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo(mediaServerItem,"rtp", "rtmp", streamId);
        return (mediaInfo.getInteger("code") == 0 && mediaInfo.getBoolean("online"));
    }

    /**
     * 查询待转推的流是否就绪
     */
    public Boolean isStreamReady(MediaServerItem mediaServerItem, String app, String streamId) {
        JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo(mediaServerItem, app, "rtmp", streamId);
        return (mediaInfo.getInteger("code") == 0 && mediaInfo.getBoolean("online"));
    }

    /**
     * 查询转推的流是否有其它观看者
     * @param streamId
     * @return
     */
    public int totalReaderCount(MediaServerItem mediaServerItem, String app, String streamId) {
        JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo(mediaServerItem, app, "rtmp", streamId);
        Integer code = mediaInfo.getInteger("code");
        if (mediaInfo == null) {
            return 0;
        }
        if ( code < 0) {
            logger.warn("查询流({}/{})是否有其它观看者时得到： {}", app, streamId, mediaInfo.getString("msg"));
            return -1;
        }
        if ( code == 0 && ! mediaInfo.getBoolean("online")) {
            logger.warn("查询流({}/{})是否有其它观看者时得到： {}", app, streamId, mediaInfo.getString("msg"));
            return -1;
        }
        return mediaInfo.getInteger("totalReaderCount");
    }

    /**
     * 调用zlm RESTful API —— stopSendRtp
     */
    public Boolean stopSendRtpStream(MediaServerItem mediaServerItem, Map<String, Object>param) {
        Boolean result = false;
        JSONObject jsonObject = zlmresTfulUtils.stopSendRtp(mediaServerItem, param);
        if (jsonObject == null) {
            logger.error("停止RTP推流失败: 请检查ZLM服务");
        } else if (jsonObject.getInteger("code") == 0) {
            result= true;
            logger.info("停止RTP推流成功");
        } else {
            logger.error("停止RTP推流失败: {}, 参数：{}",jsonObject.getString("msg"),JSONObject.toJSON(param));
        }
        return result;
    }

    public void closeAllSendRtpStream() {

    }
}
