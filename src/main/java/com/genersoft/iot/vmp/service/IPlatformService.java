package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 国标平台的业务类
 * @author lin
 */
public interface IPlatformService {

    ParentPlatform queryPlatformByServerGBId(String platformGbId);

    /**
     * 分页获取上级平台
     * @param page
     * @param count
     * @return
     */
    PageInfo<ParentPlatform> queryParentPlatformList(int page, int count, String query, Boolean online);

    /**
     * 添加级联平台
     * @param parentPlatform 级联平台
     */
    boolean add(ParentPlatform parentPlatform);

    /**
     * 添加级联平台
     * @param parentPlatform 级联平台
     */
    boolean update(ParentPlatform parentPlatform);

    /**
     * 平台上线
     * @param parentPlatform 平台信息
     */
    void online(ParentPlatform parentPlatform, SipTransactionInfo sipTransactionInfo);

    /**
     * 平台离线
     * @param parentPlatform 平台信息
     */
    void offline(ParentPlatform parentPlatform, boolean stopRegisterTask);

    /**
     * 向上级平台发起注册
     * @param parentPlatform
     */
    void login(ParentPlatform parentPlatform);

    /**
     * 向上级平台发送位置订阅
     * @param platformId 平台
     */
    void sendNotifyMobilePosition(Integer platformId);

    void addSimulatedSubscribeInfo(ParentPlatform parentPlatform);

    /**
     * 移除上级平台
     */
    boolean delete(String serverGBId);

    /**
     * 根据ID查询上级平台
     */
    ParentPlatform query(Integer platformId);

    /**
     * 开启所有开启了共享所有通道的上级
     */
    List<ParentPlatform> queryAllWithShareAll();

    /**
     * 获取指定范围内共享了指定通道的上级平台
     */
    List<ParentPlatform> querySharePlatform(List<CommonGbChannel> channel, List<Integer> platformIdList);
}
