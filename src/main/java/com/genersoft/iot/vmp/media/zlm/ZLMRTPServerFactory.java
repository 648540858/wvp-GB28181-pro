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

import java.util.HashMap;
import java.util.Map;

@Component
public class ZLMRTPServerFactory {

    private Logger logger = LoggerFactory.getLogger("ZLMRTPServerFactory");

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    private int[] portRangeArray = new int[2];

    public int createRTPServer(MediaServerItem mediaServerItem, String streamId) {
        Map<String, Integer> currentStreams = new HashMap<>();
        JSONObject listRtpServerJsonResult = zlmresTfulUtils.listRtpServer(mediaServerItem);
        if (listRtpServerJsonResult != null) {
            JSONArray data = listRtpServerJsonResult.getJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JSONObject dataItem = data.getJSONObject(i);
                    currentStreams.put(dataItem.getString("stream_id"), dataItem.getInteger("port"));
                }
            }
        }
        // 已经在推流
        if (currentStreams.get(streamId) != null) {
            Map<String, Object> closeRtpServerParam = new HashMap<>();
            closeRtpServerParam.put("stream_id", streamId);
            zlmresTfulUtils.closeRtpServer(mediaServerItem, closeRtpServerParam);
            currentStreams.remove(streamId);
        }

        Map<String, Object> param = new HashMap<>();
        int result = -1;
        /**
         * 不设置推流端口端则使用随机端口
         */
        if (StringUtils.isEmpty(mediaServerItem.getSendRtpPortRange())){
            param.put("port", 0);
        }else {
            int newPort = getPortFromportRange(mediaServerItem);
            param.put("port", newPort);
        }
        param.put("enable_tcp", 1);
        param.put("stream_id", streamId);
        JSONObject openRtpServerResultJson = zlmresTfulUtils.openRtpServer(mediaServerItem, param);

        if (openRtpServerResultJson != null) {
            switch (openRtpServerResultJson.getInteger("code")){
                case 0:
                    result= openRtpServerResultJson.getInteger("port");
                    break;
                case -300: // id已经存在, 可能已经在其他端口推流
                    Map<String, Object> closeRtpServerParam = new HashMap<>();
                    closeRtpServerParam.put("stream_id", streamId);
                    zlmresTfulUtils.closeRtpServer(mediaServerItem, closeRtpServerParam);
                    result = createRTPServer(mediaServerItem, streamId);;
                    break;
                case -400: // 端口占用
                    result= createRTPServer(mediaServerItem, streamId);
                    break;
                default:
                    logger.error("创建RTP Server 失败 {}: " + openRtpServerResultJson.getString("msg"),  param.get("port"));
                    break;
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

    private int getPortFromportRange(MediaServerItem mediaServerItem) {
        int currentPort = mediaServerItem.getCurrentPort();
        if (currentPort == 0) {
            String[] portRangeStrArray = mediaServerItem.getSendRtpPortRange().split(",");
            if (portRangeStrArray.length != 2) {
                portRangeArray[0] = 30000;
                portRangeArray[1] = 30500;
            }else {
                portRangeArray[0] = Integer.parseInt(portRangeStrArray[0]);
                portRangeArray[1] = Integer.parseInt(portRangeStrArray[1]);
            }
        }

        if (currentPort == 0 || currentPort++ > portRangeArray[1]) {
            currentPort = portRangeArray[0];
            mediaServerItem.setCurrentPort(currentPort);
            return portRangeArray[0];
        } else {
            if (currentPort % 2 == 1) {
                currentPort++;
            }
            currentPort++;
            mediaServerItem.setCurrentPort(currentPort);
            return currentPort;
        }
    }

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
        String playSsrc = serverItem.getSsrcConfig().getPlaySsrc();
        int localPort = createRTPServer(serverItem, playSsrc);
        if (localPort != -1) {
            // TODO 高并发时可能因为未放入缓存而ssrc冲突
            serverItem.getSsrcConfig().releaseSsrc(playSsrc);
            closeRTPServer(serverItem, playSsrc);
        }else {
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
        String playSsrc = serverItem.getSsrcConfig().getPlaySsrc();
        int localPort = createRTPServer(serverItem, playSsrc);
        if (localPort != -1) {
            // TODO 高并发时可能因为未放入缓存而ssrc冲突
            serverItem.getSsrcConfig().releaseSsrc(ssrc);
            closeRTPServer(serverItem, playSsrc);
        }else {
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
     * 调用zlm RESTful API —— startSendRtp
     */
    public JSONObject startSendRtpStream(MediaServerItem mediaServerItem, Map<String, Object>param) {
        Boolean result = false;
        JSONObject jsonObject = zlmresTfulUtils.startSendRtp(mediaServerItem, param);
        if (jsonObject == null) {
            logger.error("RTP推流失败: 请检查ZLM服务");
        } else if (jsonObject.getInteger("code") == 0) {
            result= true;
            logger.info("RTP推流[ {}/{} ]请求成功，本地推流端口：{}" ,param.get("app"), param.get("stream"), jsonObject.getString("local_port"));
        } else {
            logger.error("RTP推流失败: " + jsonObject.getString("msg"));
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
            logger.error("停止RTP推流失败: " + jsonObject.getString("msg"));
        }
        return result;
    }

    public void closeAllSendRtpStream() {

    }
}
