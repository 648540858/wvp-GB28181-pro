package com.genersoft.iot.vmp.sip.service;

import com.genersoft.iot.vmp.sip.bean.SipServer;
import com.genersoft.iot.vmp.sip.bean.SipServerAccount;
import com.genersoft.iot.vmp.sip.bean.SipVideo;
import com.github.pagehelper.PageInfo;

/**
 * 接入SIP系统的接口
 */
public interface ISipService {

    /**
     * 添加服务器信息
     */
    SipServer getSipServer(int sipServerId);

    /**
     * 添加服务器信息
     */
    void addSipServer(SipServer sipServer);

    /**
     * 删除服务器信息
     */
    void removeSipServer(int sipServerId);

    /**
     * 更新服务器信息
     */
    void updateSipServer(SipServer sipServer);

    /**
     * 开始连接服务器
     */
    void startSipServer(int sipServerId, SipServerAccount account, SipVideo video);

    /**
     * 与服务器断开连接
     */
    void stopSipServer(int sipServerId);

    /**
     * 服务器上线
     */
    void sipServerOnline(int sipServerId);

    /**
     * 服务器下线
     */
    void sipServerOffline(int sipServerId);


    /**
     * 分页获取服务器列表
     */
    PageInfo<SipServer> getServerList(int page, int count);


    /**
     * 添加一个sip视频
     */
    void addSipVideo(SipVideo sipVideo);

    /**
     * 获取帐号列表
     */
    PageInfo<SipServerAccount> getAccountList(int serverId, Integer page, Integer count);

    /**
     * 获取视频列表
     */
    PageInfo<SipVideo> getVideoList(int serverId, int accountId, Integer page, Integer count);

    /**
     * 增加帐号
     */
    void addSipServerAccount(SipServerAccount account);

    /**
     * 更新帐号
     */
    void updateSipServerAccount(SipServerAccount account);

    /**
     * 移除帐号
     */
    void removeSipServerAccount(Integer accountId);

    /**
     * 更新SIP推送视频
     */
    void updateSipVideo(SipVideo video);

    /**
     * 移除
     */
    void removeSipVideo(Integer videoId);
}
