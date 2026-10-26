package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.vmanager.cloudRecord.bean.CloudRecordSessionContent;

/** 已有云录像的独立观看会话，不创建或控制设备侧回放。 */
public interface ICloudRecordSessionService {
    /** 在录像所属媒体节点创建独立文件点播。 */
    CloudRecordSessionContent create(int userId, int cloudRecordId, long positionMs, String streamHost);

    /** 查询当前用户持有的会话状态。 */
    CloudRecordSessionContent get(int userId, String sessionId);

    /** 调整自己的位置，正在播放时返回替代媒体地址。 */
    CloudRecordSessionContent seek(int userId, String sessionId, long positionMs);

    /** 保存播放器实际位置并释放自己的媒体源。 */
    CloudRecordSessionContent pause(int userId, String sessionId, long positionMs);

    /** 从已保存位置恢复，返回新的独立媒体地址。 */
    CloudRecordSessionContent resume(int userId, String sessionId);

    /** 幂等关闭自己的会话，保留共享录像文件。 */
    void close(int userId, String sessionId);
}
