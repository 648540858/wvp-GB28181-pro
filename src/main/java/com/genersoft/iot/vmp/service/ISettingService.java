package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.vmanager.bean.ProfileInfo;

import java.util.List;

/**
 * 系统设置相关业务
 */
public interface ISettingService {

    /**
     * 查询配置文件列表
     * <p>
     * 1. 确定配置文件所在目录: 启动时通过 --spring.config.location 指定则使用该路径, 否则使用默认配置目录<br/>
     * 2. 扫描目录下 application-*.yml, 取中间部分作为配置文件ID<br/>
     * 3. 当前生效的配置文件被标记为 active
     */
    List<ProfileInfo> queryProfiles();

    /**
     * 切换配置文件: 将当前生效配置文件中的 spring.profiles.active 修改为指定的配置文件ID
     * <p>
     * 修改后需要重启服务才能生效
     *
     * @param profileId 目标配置文件ID
     * @return 被修改的配置文件路径
     */
    String switchProfile(String profileId);

    /**
     * 读取指定配置文件的内容
     *
     * @param profileId 配置文件ID
     * @return 配置文件文本内容
     */
    String readProfileContent(String profileId);
}
