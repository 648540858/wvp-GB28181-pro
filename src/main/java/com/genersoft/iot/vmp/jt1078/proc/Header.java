package com.genersoft.iot.vmp.jt1078.proc;

import com.genersoft.iot.vmp.jt1078.util.Bin;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:22
 * @email qingtaij@163.com
 */
public class Header {
    // 消息ID
    String msgId;

    // 消息体属性
    Integer msgPro;

    // 标识
    String devId;

    // 消息体流水号
    Integer sn;

    // 协议版本号
    Short version = -1;


    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public Integer getMsgPro() {
        return msgPro;
    }

    public void setMsgPro(Integer msgPro) {
        this.msgPro = msgPro;
    }

    public String getDevId() {
        return devId;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    public Integer getSn() {
        return sn;
    }

    public void setSn(Integer sn) {
        this.sn = sn;
    }

    public Short getVersion() {
        return version;
    }

    public void setVersion(Short version) {
        this.version = version;
    }

    /**
     * 判断是否是2019的版本
     *
     * @return true 2019后的版本。false 2013
     */
    public boolean is2019Version() {
        return Bin.get(msgPro, 14);
    }

}
