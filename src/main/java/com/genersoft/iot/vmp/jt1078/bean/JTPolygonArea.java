package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Schema(description = "多边形区域")
public class JTPolygonArea implements JTAreaOrRoute{

    @Schema(description = "区域 ID")
    private long id;

    @Schema(description = "")
    private JTAreaAttribute attribute;

    @Schema(description = "起始时间, yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "结束时间, yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @Schema(description = "最高速度, 单位为千米每小时(km/h)")
    private int maxSpeed;

    @Schema(description = "超速持续时间, 单位为秒(s)")
    private int overSpeedDuration;

    @Schema(description = "区域顶点")
    private List<JTPolygonPoint> polygonPoints;

    @Schema(description = "夜间最高速度, 单位为千米每小时(km/h)")
    private int nighttimeMaxSpeed;

    @Schema(description = "区域的名称")
    private String name;

    public ByteBuf encode(){
         ByteBuf byteBuf = Unpooled.buffer();
         byteBuf.writeInt((int) (id & 0xffffffffL));
         byteBuf.writeBytes(attribute.encode());
         byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime)));
         byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime)));
         byteBuf.writeShort((short)(maxSpeed & 0xffff));
         byteBuf.writeByte(overSpeedDuration);

         byteBuf.writeShort((short)(polygonPoints.size() & 0xffff));
         if (!polygonPoints.isEmpty()) {
             for (JTPolygonPoint polygonPoint : polygonPoints) {
                 byteBuf.writeBytes(polygonPoint.encode());
             }
         }
         byteBuf.writeShort((short)(nighttimeMaxSpeed & 0xffff));
         byteBuf.writeShort((short)(name.getBytes(Charset.forName("GBK")).length & 0xffff));
         byteBuf.writeCharSequence(name, Charset.forName("GBK"));
         return byteBuf;
     }

    public static JTPolygonArea decode(ByteBuf buf) {
        JTPolygonArea area = new JTPolygonArea();
        area.setId(buf.readUnsignedInt());
        int attributeInt = buf.readUnsignedShort();
        JTAreaAttribute areaAttribute = JTAreaAttribute.decode(attributeInt);
        area.setAttribute(areaAttribute);
        byte[] startTimeBytes = new byte[6];
        buf.readBytes(startTimeBytes);
        area.setStartTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(startTimeBytes)));
        byte[] endTimeBytes = new byte[6];
        buf.readBytes(endTimeBytes);
        area.setEndTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(endTimeBytes)));
        area.setMaxSpeed(buf.readUnsignedShort());
        area.setOverSpeedDuration(buf.readUnsignedByte());
        int polygonPointsSize = buf.readUnsignedShort();
        List<JTPolygonPoint> polygonPointList = new ArrayList<>(polygonPointsSize);
        for (int i = 0; i < polygonPointsSize; i++) {
            JTPolygonPoint polygonPoint = new JTPolygonPoint();
            polygonPoint.setLatitude(buf.readUnsignedInt()/1000000D);
            polygonPoint.setLongitude(buf.readUnsignedInt()/1000000D);
            polygonPointList.add(polygonPoint);
        }
        area.setPolygonPoints(polygonPointList);
        area.setNighttimeMaxSpeed(buf.readUnsignedShort());
        int nameLength = buf.readUnsignedShort();
        area.setName(buf.readCharSequence(nameLength, Charset.forName("GBK")).toString().trim());
        return area;
    }

}
