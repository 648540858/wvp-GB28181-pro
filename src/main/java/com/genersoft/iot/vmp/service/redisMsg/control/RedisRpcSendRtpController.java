package com.genersoft.iot.vmp.service.redisMsg.control;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcController;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcMapping;
import com.genersoft.iot.vmp.service.redisMsg.dto.RpcController;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.message.Response;

@Component
@Slf4j
@RedisRpcController("sendRtp")
public class RedisRpcSendRtpController extends RpcController {

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private UserSetting userSetting;


    /**
     * 获取发流的信息
     */
    @RedisRpcMapping("getSendRtpItem")
    public RedisRpcResponse getSendRtpItem(RedisRpcRequest request) {
        String callId = request.getParam().toString();
        SendRtpInfo sendRtpItem = sendRtpServerService.queryByCallId(callId);
        if (sendRtpItem == null) {
            log.info("[redis-rpc] 获取发流的信息, 未找到redis中的发流信息， callId：{}", callId);
            RedisRpcResponse response = request.getResponse();
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            return response;
        }
        log.info("[redis-rpc] 获取发流的信息： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
        // 查询本级是否有这个流
        MediaServer mediaServerItem = mediaServerService.getMediaServerByAppAndStream(sendRtpItem.getApp(), sendRtpItem.getStream());
        if (mediaServerItem == null) {
            RedisRpcResponse response = request.getResponse();
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
        }
        // 自平台内容
        int localPort = sendRtpServerService.getNextPort(mediaServerItem);
        if (localPort == 0) {
            log.info("[redis-rpc] getSendRtpItem->服务器端口资源不足" );
            RedisRpcResponse response = request.getResponse();
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
        }
        // 写入redis， 超时时回复
        sendRtpItem.setStatus(1);
        sendRtpItem.setServerId(userSetting.getServerId());
        sendRtpItem.setLocalIp(mediaServerItem.getSdpIp());
        if (sendRtpItem.getSsrc() == null) {
            // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
            String ssrc = "Play".equalsIgnoreCase(sendRtpItem.getSessionName()) ? ssrcFactory.getPlaySsrc(mediaServerItem.getId()) : ssrcFactory.getPlayBackSsrc(mediaServerItem.getId());
            sendRtpItem.setSsrc(ssrc);
        }
        sendRtpServerService.update(sendRtpItem);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        response.setBody(callId);
        return response;
    }

    /**
     * 开始发流
     */
    @RedisRpcMapping("startSendRtp")
    public RedisRpcResponse startSendRtp(RedisRpcRequest request) {
        String callId = request.getParam().toString();
        SendRtpInfo sendRtpItem = sendRtpServerService.queryByCallId(callId);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        if (sendRtpItem == null) {
            log.info("[redis-rpc] 开始发流, 未找到redis中的发流信息， callId：{}", callId);
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "未找到redis中的发流信息");
            response.setBody(wvpResult);
            return response;
        }
        log.info("[redis-rpc] 开始发流： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
        MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
        if (mediaServer == null) {
            log.info("[redis-rpc] startSendRtp->未找到MediaServer： {}", sendRtpItem.getMediaServerId() );
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "未找到MediaServer");
            response.setBody(wvpResult);
            return response;
        }
        MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream());
        if (mediaInfo == null) {
            log.info("[redis-rpc] startSendRtp->流不在线： {}/{}", sendRtpItem.getApp(), sendRtpItem.getStream() );
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "流不在线");
            response.setBody(wvpResult);
            return response;
        }
        try {
            mediaServerService.startSendRtp(mediaServer, sendRtpItem);
        }catch (ControllerException exception) {
            log.info("[redis-rpc] 发流失败： {}/{}, 目标地址： {}：{}， {}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort(), exception.getMsg());
            WVPResult wvpResult = WVPResult.fail(exception.getCode(), exception.getMsg());
            response.setBody(wvpResult);
            return response;
        }
        log.info("[redis-rpc] 发流成功： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
        WVPResult wvpResult = WVPResult.success();
        response.setBody(wvpResult);
        return response;
    }

    /**
     * 停止发流
     */
    @RedisRpcMapping("stopSendRtp")
    public RedisRpcResponse stopSendRtp(RedisRpcRequest request) {
        String callId = request.getParam().toString();
        SendRtpInfo sendRtpItem = sendRtpServerService.queryByCallId(callId);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        if (sendRtpItem == null) {
            log.info("[redis-rpc] 停止推流, 未找到redis中的发流信息， key：{}", callId);
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "未找到redis中的发流信息");
            response.setBody(wvpResult);
            return response;
        }
        log.info("[redis-rpc] 停止推流： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
        MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
        if (mediaServer == null) {
            log.info("[redis-rpc] stopSendRtp->未找到MediaServer： {}", sendRtpItem.getMediaServerId() );
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "未找到MediaServer");
            response.setBody(wvpResult);
            return response;
        }
        try {
            mediaServerService.stopSendRtp(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getSsrc());
        }catch (ControllerException exception) {
            log.info("[redis-rpc] 停止推流失败： {}/{}, 目标地址： {}：{}， code： {}, msg: {}", sendRtpItem.getApp(),
                    sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort(), exception.getCode(), exception.getMsg() );
            response.setBody(WVPResult.fail(exception.getCode(), exception.getMsg()));
            return response;
        }
        log.info("[redis-rpc] 停止推流成功： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
        response.setBody(WVPResult.success());
        return response;
    }

}
