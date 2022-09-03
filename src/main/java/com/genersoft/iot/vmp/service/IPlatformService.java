package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.github.pagehelper.PageInfo;

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
    PageInfo<ParentPlatform> queryParentPlatformList(int page, int count);

    /**
     * 添加级联平台
     * @param parentPlatform 级联平台
     */
    boolean add(ParentPlatform parentPlatform);

    /**
     * 平台上线
     * @param parentPlatform 平台信息
     */
    void online(ParentPlatform parentPlatform);

    /**
     * 平台离线
     * @param parentPlatform 平台信息
     */
    void offline(ParentPlatform parentPlatform);

    /**
     * 向上级平台发起注册
     * @param parentPlatform
     */
    void login(ParentPlatform parentPlatform);
}
