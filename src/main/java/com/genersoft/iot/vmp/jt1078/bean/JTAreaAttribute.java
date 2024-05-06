package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "区域属性")
public class JTAreaAttribute {

    @Schema(description = "是否启用起始时间与结束时间的判断规则 ,false：否；true：是")
    private boolean ruleForTimeLimit;

    @Schema(description = "是否启用最高速度、超速持续时间和夜间最高速度的判断规则 ,false：否；true：是")
    private boolean ruleForSpeedLimit;

    @Schema(description = "进区域是否报警给驾驶员,false：否；true：是")
    private boolean ruleForAlarmToDriverWhenEnter;

    @Schema(description = "进区域是否报警给平台 ,false：否；true：是")
    private boolean ruleForAlarmToPlatformWhenEnter;

    @Schema(description = "出区域是否报警给驾驶员,false：否；true：是")
    private boolean ruleForAlarmToDriverWhenExit;

    @Schema(description = "出区域是否报警给平台 ,false：否；true：是")
    private boolean ruleForAlarmToPlatformWhenExit;

    @Schema(description = "false：北纬；true：南纬")
    private boolean southLatitude;

    @Schema(description = "false：东经；true：西经")
    private boolean westLongitude;

    @Schema(description = "false：允许开门；true：禁止开门")
    private boolean prohibitOpeningDoors;

    @Schema(description = "false：进区域开启通信模块；true：进区域关闭通信模块")
    private boolean ruleForTurnOffCommunicationWhenEnter;

    @Schema(description = "false：进区域不采集 GNSS 详细定位数据；true：进区域采集 GNSS 详细定位数据")
    private boolean ruleForGnssWhenEnter;

    public ByteBuf encode(){
        ByteBuf byteBuf = Unpooled.buffer();
        short content = 0 ;
        if (ruleForTimeLimit) {
            content |= 1;
        }
        if (ruleForSpeedLimit) {
            content |= (1 << 1);
        }
        if (ruleForAlarmToDriverWhenEnter) {
            content |= (1 << 2);
        }
        if (ruleForAlarmToPlatformWhenEnter) {
            content |= (1 << 3);
        }
        if (ruleForAlarmToDriverWhenExit) {
            content |= (1 << 4);
        }
        if (ruleForAlarmToPlatformWhenExit) {
            content |= (1 << 5);
        }
        if (southLatitude) {
            content |= (1 << 6);
        }
        if (westLongitude) {
            content |= (byte) (1 << 7);
        }
        if (prohibitOpeningDoors) {
            content |= (1 << (0 + 8));
        }
        if (ruleForTurnOffCommunicationWhenEnter) {
            content |= (1 << (1 + 8));
        }
        if (ruleForGnssWhenEnter) {
            content |= (1 << (2 + 8));
        }
        byteBuf.writeShort((short)(content & 0xffff));
        return byteBuf;
    }

    public static JTAreaAttribute decode(int attributeInt) {
        JTAreaAttribute attribute = new JTAreaAttribute();
        attribute.setRuleForTimeLimit((attributeInt & 1) == 1);
        attribute.setRuleForSpeedLimit((attributeInt >> 1 & 1) == 1);
        attribute.setRuleForAlarmToDriverWhenEnter((attributeInt >> 2 & 1) == 1);
        attribute.setRuleForAlarmToPlatformWhenEnter((attributeInt >> 3 & 1) == 1);
        attribute.setRuleForAlarmToDriverWhenExit((attributeInt >> 4 & 1) == 1);
        attribute.setRuleForAlarmToPlatformWhenExit((attributeInt >> 5 & 1) == 1);
        attribute.setSouthLatitude((attributeInt >> 6 & 1) == 1);
        attribute.setWestLongitude((attributeInt >> 7 & 1) == 1);
        attribute.setProhibitOpeningDoors((attributeInt >> 8 & 1) == 1);
        attribute.setRuleForTurnOffCommunicationWhenEnter((attributeInt >> 9 & 1) == 1);
        attribute.setRuleForGnssWhenEnter((attributeInt >> 10 & 1) == 1);
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

    public boolean isRuleForAlarmToDriverWhenEnter() {
        return ruleForAlarmToDriverWhenEnter;
    }

    public void setRuleForAlarmToDriverWhenEnter(boolean ruleForAlarmToDriverWhenEnter) {
        this.ruleForAlarmToDriverWhenEnter = ruleForAlarmToDriverWhenEnter;
    }

    public boolean isRuleForAlarmToPlatformWhenEnter() {
        return ruleForAlarmToPlatformWhenEnter;
    }

    public void setRuleForAlarmToPlatformWhenEnter(boolean ruleForAlarmToPlatformWhenEnter) {
        this.ruleForAlarmToPlatformWhenEnter = ruleForAlarmToPlatformWhenEnter;
    }

    public boolean isRuleForAlarmToDriverWhenExit() {
        return ruleForAlarmToDriverWhenExit;
    }

    public void setRuleForAlarmToDriverWhenExit(boolean ruleForAlarmToDriverWhenExit) {
        this.ruleForAlarmToDriverWhenExit = ruleForAlarmToDriverWhenExit;
    }

    public boolean isRuleForAlarmToPlatformWhenExit() {
        return ruleForAlarmToPlatformWhenExit;
    }

    public void setRuleForAlarmToPlatformWhenExit(boolean ruleForAlarmToPlatformWhenExit) {
        this.ruleForAlarmToPlatformWhenExit = ruleForAlarmToPlatformWhenExit;
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

    public boolean isProhibitOpeningDoors() {
        return prohibitOpeningDoors;
    }

    public void setProhibitOpeningDoors(boolean prohibitOpeningDoors) {
        this.prohibitOpeningDoors = prohibitOpeningDoors;
    }

    public boolean isRuleForTurnOffCommunicationWhenEnter() {
        return ruleForTurnOffCommunicationWhenEnter;
    }

    public void setRuleForTurnOffCommunicationWhenEnter(boolean ruleForTurnOffCommunicationWhenEnter) {
        this.ruleForTurnOffCommunicationWhenEnter = ruleForTurnOffCommunicationWhenEnter;
    }

    public boolean isRuleForGnssWhenEnter() {
        return ruleForGnssWhenEnter;
    }

    public void setRuleForGnssWhenEnter(boolean ruleForGnssWhenEnter) {
        this.ruleForGnssWhenEnter = ruleForGnssWhenEnter;
    }
}
