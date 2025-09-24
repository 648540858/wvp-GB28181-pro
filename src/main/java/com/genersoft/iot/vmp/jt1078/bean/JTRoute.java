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
@Schema(description = "路线")
public class JTRoute implements JTAreaOrRoute{

    @Schema(description = "路线 ID")
    private long id;

    @Schema(description = "路线属性")
    private JTRouteAttribute attribute;

    @Schema(description = "起始时间, yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "结束时间, yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @Schema(description = "路线拐点")
    private List<JTRoutePoint> routePointList;

    @Schema(description = "区域的名称")
    private String name;

    public ByteBuf encode(){
         ByteBuf byteBuf = Unpooled.buffer();
         byteBuf.writeInt((int) (id & 0xffffffffL));
         byteBuf.writeBytes(attribute.encode());
         byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime)));
         byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime)));
         byteBuf.writeShort((short)(routePointList.size() & 0xffff));
         if (!routePointList.isEmpty()){
             for (JTRoutePoint jtRoutePoint : routePointList) {
                 byteBuf.writeBytes(jtRoutePoint.encode());
             }
         }
         byteBuf.writeShort((short)(name.getBytes(Charset.forName("GBK")).length & 0xffff));
         byteBuf.writeCharSequence(name, Charset.forName("GBK"));
         return byteBuf;
     }

    public static JTRoute decode(ByteBuf buf) {
        JTRoute route = new JTRoute();
        route.setId(buf.readUnsignedInt());
        int attributeInt = buf.readUnsignedShort();
        JTRouteAttribute routeAttribute = JTRouteAttribute.decode(attributeInt);
        route.setAttribute(routeAttribute);
        byte[] startTimeBytes = new byte[6];
        buf.readBytes(startTimeBytes);
        route.setStartTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(startTimeBytes)));
        byte[] endTimeBytes = new byte[6];
        buf.readBytes(endTimeBytes);
        route.setEndTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(endTimeBytes)));

        int routePointsSize = buf.readUnsignedShort();
        List<JTRoutePoint> jtRoutePoints = new ArrayList<>(routePointsSize);
        for (int i = 0; i < routePointsSize; i++) {
            jtRoutePoints.add(JTRoutePoint.decode(buf));
        }
        route.setRoutePointList(jtRoutePoints);
        int nameLength = buf.readUnsignedShort();
        route.setName(buf.readCharSequence(nameLength, Charset.forName("GBK")).toString().trim());
        return route;
    }

    @Override
    public String toString() {
        return "JTRoute{" +
                "id=" + id +
                ", attribute=" + attribute +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", routePointList=" + routePointList +
                ", name='" + name + '\'' +
                '}';
    }
}
