package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;

import java.nio.charset.Charset;

@Schema(description = "矩形区域")
public class JTRectangleArea {

    @Schema(description = "区域 ID")
    private long id;

    @Schema(description = "")
    private JTAreaAttribute attribute;

    @Schema(description = "左上点纬度")
    private Double latitudeForUpperLeft;

    @Schema(description = "左上点经度")
    private Double longitudeForUpperLeft;

    @Schema(description = "右下点纬度")
    private Double latitudeForLowerRight;

    @Schema(description = "右下点经度")
    private Double longitudeForLowerRight;

    @Schema(description = "起始时间, yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "结束时间, yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @Schema(description = "最高速度, 单位为千米每小时(km/h)")
    private int maxSpeed;

    @Schema(description = "超速持续时间, 单位为秒(s)")
    private int overSpeedDuration;

    @Schema(description = "夜间最高速度, 单位为千米每小时(km/h)")
    private int nighttimeMaxSpeed;

    @Schema(description = "区域的名称")
    private String name;

     public ByteBuf encode(){
         ByteBuf byteBuf = Unpooled.buffer();
         byteBuf.writeInt((int) (id & 0xffffffffL));
         byteBuf.writeBytes(attribute.encode());
         byteBuf.writeInt((int) (Math.round((latitudeForUpperLeft * 1000000)) & 0xffffffffL));
         byteBuf.writeInt((int) (Math.round((longitudeForLowerRight * 1000000)) & 0xffffffffL));
         byteBuf.writeInt((int) (Math.round((latitudeForLowerRight * 1000000)) & 0xffffffffL));
         byteBuf.writeInt((int) (Math.round((longitudeForLowerRight * 1000000)) & 0xffffffffL));
         byteBuf.writeBytes(BCDUtil.transform(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime)));
         byteBuf.writeBytes(BCDUtil.transform(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime)));
         byteBuf.writeShort((short)(maxSpeed & 0xffff));
         byteBuf.writeByte(overSpeedDuration);
         byteBuf.writeShort((short)(nighttimeMaxSpeed & 0xffff));
         byteBuf.writeShort((short)(name.getBytes(Charset.forName("GBK")).length & 0xffff));
         byteBuf.writeCharSequence(name, Charset.forName("GBK"));
         return byteBuf;
     }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public JTAreaAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(JTAreaAttribute attribute) {
        this.attribute = attribute;
    }

    public Double getLatitudeForUpperLeft() {
        return latitudeForUpperLeft;
    }

    public void setLatitudeForUpperLeft(Double latitudeForUpperLeft) {
        this.latitudeForUpperLeft = latitudeForUpperLeft;
    }

    public Double getLongitudeForUpperLeft() {
        return longitudeForUpperLeft;
    }

    public void setLongitudeForUpperLeft(Double longitudeForUpperLeft) {
        this.longitudeForUpperLeft = longitudeForUpperLeft;
    }

    public Double getLatitudeForLowerRight() {
        return latitudeForLowerRight;
    }

    public void setLatitudeForLowerRight(Double latitudeForLowerRight) {
        this.latitudeForLowerRight = latitudeForLowerRight;
    }

    public Double getLongitudeForLowerRight() {
        return longitudeForLowerRight;
    }

    public void setLongitudeForLowerRight(Double longitudeForLowerRight) {
        this.longitudeForLowerRight = longitudeForLowerRight;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public int getOverSpeedDuration() {
        return overSpeedDuration;
    }

    public void setOverSpeedDuration(int overSpeedDuration) {
        this.overSpeedDuration = overSpeedDuration;
    }

    public int getNighttimeMaxSpeed() {
        return nighttimeMaxSpeed;
    }

    public void setNighttimeMaxSpeed(int nighttimeMaxSpeed) {
        this.nighttimeMaxSpeed = nighttimeMaxSpeed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
