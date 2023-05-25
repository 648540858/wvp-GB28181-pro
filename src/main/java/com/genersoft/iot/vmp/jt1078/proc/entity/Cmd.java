package com.genersoft.iot.vmp.jt1078.proc.entity;

import com.genersoft.iot.vmp.jt1078.proc.response.Rs;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:23
 * @email qingtaij@163.com
 */
public class Cmd {
    String devId;
    Long packageNo;
    String msgId;
    String respId;
    Rs rs;

    public Cmd() {
    }

    public Cmd(Builder builder) {
        this.devId = builder.devId;
        this.packageNo = builder.packageNo;
        this.msgId = builder.msgId;
        this.respId = builder.respId;
        this.rs = builder.rs;
    }

    public String getDevId() {
        return devId;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    public Long getPackageNo() {
        return packageNo;
    }

    public void setPackageNo(Long packageNo) {
        this.packageNo = packageNo;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getRespId() {
        return respId;
    }

    public void setRespId(String respId) {
        this.respId = respId;
    }

    public Rs getRs() {
        return rs;
    }

    public void setRs(Rs rs) {
        this.rs = rs;
    }

    public static class Builder {
        String devId;
        Long packageNo;
        String msgId;
        String respId;
        Rs rs;

        public Builder setDevId(String devId) {
            this.devId = devId.replaceFirst("^0*", "");
            return this;
        }

        public Builder setPackageNo(Long packageNo) {
            this.packageNo = packageNo;
            return this;
        }

        public Builder setMsgId(String msgId) {
            this.msgId = msgId;
            return this;
        }

        public Builder setRespId(String respId) {
            this.respId = respId;
            return this;
        }

        public Builder setRs(Rs re) {
            this.rs = re;
            return this;
        }

        public Cmd build() {
            return new Cmd(this);
        }
    }


    @Override
    public String toString() {
        return "Cmd{" +
                "devId='" + devId + '\'' +
                ", packageNo=" + packageNo +
                ", msgId='" + msgId + '\'' +
                ", respId='" + respId + '\'' +
                ", rs=" + rs +
                '}';
    }
}
