package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.Charset;

@Setter
@Getter
@Schema(description = "矩形区域")
public class JTRectangleArea implements JTAreaOrRoute{

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
         byteBuf.writeInt((int) (Math.round((longitudeForUpperLeft * 1000000)) & 0xffffffffL));
         byteBuf.writeInt((int) (Math.round((latitudeForLowerRight * 1000000)) & 0xffffffffL));
         byteBuf.writeInt((int) (Math.round((longitudeForLowerRight * 1000000)) & 0xffffffffL));
         byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime)));
         byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime)));
         byteBuf.writeShort((short)(maxSpeed & 0xffff));
         byteBuf.writeByte(overSpeedDuration);
         byteBuf.writeShort((short)(nighttimeMaxSpeed & 0xffff));
         byteBuf.writeShort((short)(name.getBytes(Charset.forName("GBK")).length & 0xffff));
         byteBuf.writeCharSequence(name, Charset.forName("GBK"));
         return byteBuf;
     }

    public static JTRectangleArea decode(ByteBuf buf) {
        JTRectangleArea area = new JTRectangleArea();
        area.setId(buf.readUnsignedInt());
        int attributeInt = buf.readUnsignedShort();
        JTAreaAttribute areaAttribute = JTAreaAttribute.decode(attributeInt);
        area.setAttribute(areaAttribute);
        area.setLatitudeForUpperLeft(buf.readUnsignedInt()/1000000D);
        area.setLongitudeForUpperLeft(buf.readUnsignedInt()/1000000D);
        area.setLatitudeForLowerRight(buf.readUnsignedInt()/1000000D);
        area.setLongitudeForLowerRight(buf.readUnsignedInt()/1000000D);
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

}
