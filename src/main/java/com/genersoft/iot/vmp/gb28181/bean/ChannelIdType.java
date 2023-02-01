package com.genersoft.iot.vmp.gb28181.bean;

/**
 * 国标类型编码,国标编码中11-13位为类型编码
 * 详见 附 录 D  编码规则 A
 * @author lin
 */
public class ChannelIdType {
    /**
     * 中心信令控制服务器编码
     */
    public final static String CENTRAL_SIGNALING_CONTROL_SERVER = "200";

    /**
     * 业务分组编码
     */
    public final static String BUSINESS_GROUP = "215";

    /**
     * 虚拟组织编码
     */
    public final static String VIRTUAL_ORGANIZATION = "216";
}
