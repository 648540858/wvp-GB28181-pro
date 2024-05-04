package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "路线拐点")
public class JTRoutePoint {

    @Schema(description = "拐点 ID")
    private int id;

    @Schema(description = "路段 ID")
    private int routeSectionId;

    @Schema(description = "拐点纬度")
    private Double latitude;

    @Schema(description = "拐点经度")
    private Double longitude;

    @Schema(description = "路段宽度")
    private int routeSectionAttributeWidth;

    @Schema(description = "路段属性")
    private JTRouteSectionAttribute routeSectionAttribute;

    @Schema(description = "路段行驶过长國值")
    private int routeSectionMaxLength;

    @Schema(description = "路段行驶不足國值")
    private int routeSectionMinLength;

    @Schema(description = "路段最高速度")
    private int routeSectionMaxSpeed;

    @Schema(description = "路段超速持续时间")
    private int routeSectionOverSpeedDuration;

    @Schema(description = "路段夜间最高速度")
    private int routeSectionNighttimeMaxSpeed;

    public ByteBuf encode(){
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt((int) (id & 0xffffffffL));
        byteBuf.writeInt((int) (routeSectionId & 0xffffffffL));
        byteBuf.writeInt((int) (Math.round((latitude * 1000000)) & 0xffffffffL));
        byteBuf.writeInt((int) (Math.round((longitude * 1000000)) & 0xffffffffL));
        byteBuf.writeByte(routeSectionAttributeWidth);
        byteBuf.writeByte(routeSectionAttribute.encode());
        byteBuf.writeShort((short)(routeSectionMaxLength & 0xffff));
        byteBuf.writeShort((short)(routeSectionMinLength & 0xffff));
        byteBuf.writeShort((short)(routeSectionMaxSpeed & 0xffff));
        byteBuf.writeByte(routeSectionOverSpeedDuration);
        byteBuf.writeShort((short)(routeSectionNighttimeMaxSpeed & 0xffff));
        return byteBuf;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRouteSectionId() {
        return routeSectionId;
    }

    public void setRouteSectionId(int routeSectionId) {
        this.routeSectionId = routeSectionId;
    }

    public int getRouteSectionAttributeWidth() {
        return routeSectionAttributeWidth;
    }

    public void setRouteSectionAttributeWidth(int routeSectionAttributeWidth) {
        this.routeSectionAttributeWidth = routeSectionAttributeWidth;
    }

    public JTRouteSectionAttribute getRouteSectionAttribute() {
        return routeSectionAttribute;
    }

    public void setRouteSectionAttribute(JTRouteSectionAttribute routeSectionAttribute) {
        this.routeSectionAttribute = routeSectionAttribute;
    }

    public int getRouteSectionMaxLength() {
        return routeSectionMaxLength;
    }

    public void setRouteSectionMaxLength(int routeSectionMaxLength) {
        this.routeSectionMaxLength = routeSectionMaxLength;
    }

    public int getRouteSectionMinLength() {
        return routeSectionMinLength;
    }

    public void setRouteSectionMinLength(int routeSectionMinLength) {
        this.routeSectionMinLength = routeSectionMinLength;
    }

    public int getRouteSectionMaxSpeed() {
        return routeSectionMaxSpeed;
    }

    public void setRouteSectionMaxSpeed(int routeSectionMaxSpeed) {
        this.routeSectionMaxSpeed = routeSectionMaxSpeed;
    }

    public int getRouteSectionOverSpeedDuration() {
        return routeSectionOverSpeedDuration;
    }

    public void setRouteSectionOverSpeedDuration(int routeSectionOverSpeedDuration) {
        this.routeSectionOverSpeedDuration = routeSectionOverSpeedDuration;
    }

    public int getRouteSectionNighttimeMaxSpeed() {
        return routeSectionNighttimeMaxSpeed;
    }

    public void setRouteSectionNighttimeMaxSpeed(int routeSectionNighttimeMaxSpeed) {
        this.routeSectionNighttimeMaxSpeed = routeSectionNighttimeMaxSpeed;
    }

    @Override
    public String toString() {
        return "JTRoutePoint{" +
                "id=" + id +
                ", routeSectionId=" + routeSectionId +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", routeSectionAttributeWidth=" + routeSectionAttributeWidth +
                ", routeSectionAttribute=" + routeSectionAttribute +
                ", routeSectionMaxLength=" + routeSectionMaxLength +
                ", routeSectionMinLength=" + routeSectionMinLength +
                ", routeSectionMaxSpeed=" + routeSectionMaxSpeed +
                ", routeSectionOverSpeedDuration=" + routeSectionOverSpeedDuration +
                ", routeSectionNighttimeMaxSpeed=" + routeSectionNighttimeMaxSpeed +
                '}';
    }
}
