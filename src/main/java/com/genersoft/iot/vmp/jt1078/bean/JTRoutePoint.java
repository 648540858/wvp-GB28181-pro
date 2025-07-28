package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "路线拐点")
public class JTRoutePoint {

    @Schema(description = "拐点 ID")
    private long id;

    @Schema(description = "路段 ID")
    private long routeSectionId;

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

    public static JTRoutePoint decode(ByteBuf buf) {
        JTRoutePoint point = new JTRoutePoint();
        point.setId(buf.readUnsignedInt());
        point.setRouteSectionId(buf.readUnsignedInt());
        point.setLatitude(buf.readUnsignedInt()/1000000D);
        point.setLongitude(buf.readUnsignedInt()/1000000D);
        point.setRouteSectionAttributeWidth(buf.readUnsignedByte());

        JTRouteSectionAttribute areaAttribute = JTRouteSectionAttribute.decode(buf.readUnsignedByte());
        point.setRouteSectionAttribute(areaAttribute);

        point.setRouteSectionMaxLength(buf.readUnsignedShort());
        point.setRouteSectionMinLength(buf.readUnsignedShort());
        point.setRouteSectionMaxSpeed(buf.readUnsignedShort());
        point.setRouteSectionOverSpeedDuration(buf.readUnsignedByte());
        point.setRouteSectionNighttimeMaxSpeed(buf.readUnsignedShort());
        return point;
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
