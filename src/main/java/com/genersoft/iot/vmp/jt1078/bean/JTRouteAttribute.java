package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "路线属性")
public class JTRouteAttribute {

    @Schema(description = "是否启用起始时间与结束时间的判断规则 ,false：否；true：是")
    private boolean ruleForTimeLimit;

    @Schema(description = "进区域是否报警给驾驶员,false：否；true：是")
    private boolean ruleForAlarmToDriverWhenEnter;

    @Schema(description = "进区域是否报警给平台 ,false：否；true：是")
    private boolean ruleForAlarmToPlatformWhenEnter;

    @Schema(description = "出区域是否报警给驾驶员,false：否；true：是")
    private boolean ruleForAlarmToDriverWhenExit;

    @Schema(description = "出区域是否报警给平台 ,false：否；true：是")
    private boolean ruleForAlarmToPlatformWhenExit;

    public ByteBuf encode(){
        ByteBuf byteBuf = Unpooled.buffer();
        byte[] bytes = new byte[2];
        if (ruleForTimeLimit) {
            bytes[0] |= 1;
        }
        if (ruleForAlarmToDriverWhenEnter) {
            bytes[0] |= (1 << 2);
        }
        if (ruleForAlarmToPlatformWhenEnter) {
            bytes[0] |= (1 << 3);
        }
        if (ruleForAlarmToDriverWhenExit) {
            bytes[0] |= (1 << 4);
        }
        if (ruleForAlarmToPlatformWhenExit) {
            bytes[0] |= (1 << 5);
        }
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }
    public boolean isRuleForTimeLimit() {
        return ruleForTimeLimit;
    }

    public void setRuleForTimeLimit(boolean ruleForTimeLimit) {
        this.ruleForTimeLimit = ruleForTimeLimit;
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

    @Override
    public String toString() {
        return "JTRouteAttribute{" +
                "ruleForTimeLimit=" + ruleForTimeLimit +
                ", ruleForAlarmToDriverWhenEnter=" + ruleForAlarmToDriverWhenEnter +
                ", ruleForAlarmToPlatformWhenEnter=" + ruleForAlarmToPlatformWhenEnter +
                ", ruleForAlarmToDriverWhenExit=" + ruleForAlarmToDriverWhenExit +
                ", ruleForAlarmToPlatformWhenExit=" + ruleForAlarmToPlatformWhenExit +
                '}';
    }
}
