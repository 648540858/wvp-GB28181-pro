package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "路段属性")
public class JTRouteSectionAttribute {

    @Schema(description = "行驶时间 ,false：否；true：是")
    private boolean ruleForTimeLimit;

    @Schema(description = "限速 ,false：否；true：是")
    private boolean ruleForSpeedLimit;

    @Schema(description = "false：北纬；true：南纬")
    private boolean southLatitude;

    @Schema(description = "false：东经；true：西经")
    private boolean westLongitude;

    public byte encode(){
        byte attributeByte = 0;
        if (ruleForTimeLimit) {
            attributeByte |= 1;
        }
        if (ruleForSpeedLimit) {
            attributeByte |= (1 << 1);
        }
        if (southLatitude) {
            attributeByte |= (1 << 2);
        }
        if (westLongitude) {
            attributeByte |= (1 << 3);
        }
        return attributeByte;
    }

    public static JTRouteSectionAttribute decode(short attributeShort) {
        JTRouteSectionAttribute attribute = new JTRouteSectionAttribute();
        attribute.setRuleForTimeLimit((attributeShort & 1) == 1);
        attribute.setRuleForSpeedLimit((attributeShort >> 1 & 1) == 1);
        attribute.setSouthLatitude((attributeShort >> 2 & 1) == 1);
        attribute.setWestLongitude((attributeShort >> 3 & 1) == 1);
        return attribute;
    }

    public boolean isRuleForTimeLimit() {
        return ruleForTimeLimit;
    }

    public void setRuleForTimeLimit(boolean ruleForTimeLimit) {
        this.ruleForTimeLimit = ruleForTimeLimit;
    }

    public boolean isRuleForSpeedLimit() {
        return ruleForSpeedLimit;
    }

    public void setRuleForSpeedLimit(boolean ruleForSpeedLimit) {
        this.ruleForSpeedLimit = ruleForSpeedLimit;
    }

    public boolean isSouthLatitude() {
        return southLatitude;
    }

    public void setSouthLatitude(boolean southLatitude) {
        this.southLatitude = southLatitude;
    }

    public boolean isWestLongitude() {
        return westLongitude;
    }

    public void setWestLongitude(boolean westLongitude) {
        this.westLongitude = westLongitude;
    }
}
