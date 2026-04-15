package com.genersoft.iot.vmp.gb28181.service;

/**
 * 资源能力接入-其他
 */
public interface ISourceOtherService {


    Boolean closeStreamOnNoneReader(String mediaServerId, String app, String stream, String schema);
}
