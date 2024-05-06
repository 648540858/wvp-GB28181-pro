package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;

import java.nio.charset.Charset;
import java.util.Date;

@Schema(description = "圆形区域")
public class JTCircleArea implements JTAreaOrRoute{

    @Schema(description = "区域 ID")
    private long id;

    @Schema(description = "")
    private JTAreaAttribute attribute;

    @Schema(description = "中心点纬度")
    private Double latitude;

    @Schema(description = "中心点经度")
    private Double longitude;

    @Schema(description = "半径,单位为米(m)")
    private long radius;

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
         byteBuf.writeInt((int) (Math.round((latitude * 1000000)) & 0xffffffffL));
         byteBuf.writeInt((int) (Math.round((longitude * 1000000)) & 0xffffffffL));
         byteBuf.writeInt((int) (radius & 0xffffffffL));
         byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime)));
         byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime)));
         byteBuf.writeShort((short)(maxSpeed & 0xffff));
         byteBuf.writeByte(overSpeedDuration);
         byteBuf.writeShort((short)(nighttimeMaxSpeed & 0xffff));
         byteBuf.writeShort((short)(name.getBytes(Charset.forName("GBK")).length & 0xffff));
         byteBuf.writeCharSequence(name, Charset.forName("GBK"));
         return byteBuf;
     }

    public static JTCircleArea decode(ByteBuf buf) {

        JTCircleArea area = new JTCircleArea();
        area.setId(buf.readUnsignedInt());
        int attributeInt = buf.readUnsignedShort();
        JTAreaAttribute areaAttribute = JTAreaAttribute.decode(attributeInt);
        area.setAttribute(areaAttribute);

        area.setLatitude(buf.readUnsignedInt()/1000000D);
        area.setLongitude(buf.readUnsignedInt()/1000000D);
        area.setRadius(buf.readUnsignedInt());
        byte[] startTimeBytes = new byte[6];
        buf.readBytes(startTimeBytes);
        area.setStartTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(startTimeBytes)));
        byte[] endTimeBytes = new byte[6];
        buf.readBytes(endTimeBytes);
        area.setEndTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(endTimeBytes)));
        area.setMaxSpeed(buf.readUnsignedShort());
        area.setOverSpeedDuration(buf.readUnsignedByte());
        area.setNighttimeMaxSpeed(buf.readUnsignedShort());
        int nameLength = buf.readUnsignedShort();
        area.setName(buf.readCharSequence(nameLength, Charset.forName("GBK")).toString().trim());
        return area;
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

    public long getRadius() {
        return radius;
    }

    public void setRadius(long radius) {
        this.radius = radius;
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
