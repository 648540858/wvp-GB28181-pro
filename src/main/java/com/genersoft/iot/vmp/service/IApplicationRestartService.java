package com.genersoft.iot.vmp.service;

/**
 * 服务重启业务
 */
public interface IApplicationRestartService {

    /**
     * 执行重启脚本
     * <p>
     * 脚本位于 jar 同级目录, 脚本内部负责停止当前进程并重新启动服务
     *
     * @return 是否已成功触发脚本
     */
    boolean restart();
}
