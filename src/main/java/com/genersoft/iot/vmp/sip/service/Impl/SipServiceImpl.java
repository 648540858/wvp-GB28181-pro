package com.genersoft.iot.vmp.sip.service.Impl;

import com.genersoft.iot.vmp.sip.bean.SipServer;
import com.genersoft.iot.vmp.sip.bean.SipServerAccount;
import com.genersoft.iot.vmp.sip.bean.SipVideo;
import com.genersoft.iot.vmp.sip.dao.SipServerAccountMapper;
import com.genersoft.iot.vmp.sip.dao.SipServerMapper;
import com.genersoft.iot.vmp.sip.dao.SipVideoMapper;
import com.genersoft.iot.vmp.sip.service.ISipService;
import com.genersoft.iot.vmp.sip.service.SipSdk;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.TransportNotSupportedException;
import java.util.List;
import java.util.TooManyListenersException;

@Service
public class SipServiceImpl implements ISipService {

    private final static Logger logger = LoggerFactory.getLogger(SipServiceImpl.class);


    @Autowired
    private SipServerMapper serverMapper;

    @Autowired
    private SipServerAccountMapper accountMapper;

    @Autowired
    private SipVideoMapper videoMapper;

    @Autowired
    private SipSdk sipSdk;


    @Override
    public SipServer getSipServer(int sipServerId) {
        return serverMapper.query(sipServerId);
    }

    @Override
    public void addSipServer(SipServer sipServer) {
        serverMapper.add(sipServer);
    }

    @Override
    public void removeSipServer(int sipServerId) {
        serverMapper.remove(sipServerId);
    }

    @Override
    public void updateSipServer(SipServer sipServer) {
        serverMapper.update(sipServer);
    }

    @Override
    public void startSipServer(int sipServerId, SipServerAccount account, SipVideo video) {
        SipServer sipServer = getSipServer(sipServerId);
        if (sipServer == null){
            return;
        }
        if (account.getId() == null) {

        }

    }

    @Override
    public void stopSipServer(int sipAccountId) {

    }

    @Override
    public void sipServerOnline(int sipAccountId) {

    }

    @Override
    public void sipServerOffline(int sipAccountId) {

    }

    @Override
    public PageInfo<SipServer> getServerList(int page, int count) {
        PageHelper.startPage(page, count);
        List<SipServer> all = serverMapper.all();
        return new PageInfo<>(all);
    }

    @Override
    public void addSipVideo(SipVideo sipVideo) {
        videoMapper.add(sipVideo);
    }

    @Override
    public PageInfo<SipServerAccount> getAccountList(int serverId, Integer page, Integer count) {
        PageHelper.startPage(page, count);
        List<SipServerAccount> all = accountMapper.all(serverId);
        return new PageInfo<>(all);
    }

    @Override
    public PageInfo<SipVideo> getVideoList(int serverId, int accountId, Integer page, Integer count) {
        PageHelper.startPage(page, count);
        List<SipVideo> all = videoMapper.all(serverId, accountId);
        return new PageInfo<>(all);
    }

    @Override
    public void addSipServerAccount(SipServerAccount account) {
        if (account.getSipServerId() == null) {
            return;
        }
        SipServer server = serverMapper.query(account.getSipServerId());
        if (server == null) {
            return;
        }
        accountMapper.add(account);
        try {
            sipSdk.register(server, account, (code, msg, data) -> {
                if (code == 200) {

                }else {

                }
            });
        } catch (PeerUnavailableException | TransportNotSupportedException | InvalidArgumentException |
                 ObjectInUseException | TooManyListenersException e) {

        }


    }

    @Override
    public void updateSipServerAccount(SipServerAccount account) {
        accountMapper.update(account);
    }

    @Override
    public void removeSipServerAccount(Integer accountId) {
        accountMapper.remove(accountId);
    }

    @Override
    public void updateSipVideo(SipVideo video) {
        videoMapper.update(video);
    }

    @Override
    public void removeSipVideo(Integer videoId) {
        videoMapper.remove(videoId);
    }

    @Override
    public SipServer getSipServerByServerAddress(String host, int port) {
        return serverMapper.getOneByServerAddress(host, port);
    }

    @Override
    public SipServerAccount getAccountByUsername(int serverId, String username) {
        return accountMapper.getOneByUsername(serverId, username);
    }
}
