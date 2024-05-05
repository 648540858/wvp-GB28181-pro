package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;

import java.nio.charset.Charset;
import java.util.List;

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
         byteBuf.writeBytes(BCDUtil.transform(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime)));
         byteBuf.writeBytes(BCDUtil.transform(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime)));
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public JTRouteAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(JTRouteAttribute attribute) {
        this.attribute = attribute;
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

    public List<JTRoutePoint> getRoutePointList() {
        return routePointList;
    }

    public void setRoutePointList(List<JTRoutePoint> routePointList) {
        this.routePointList = routePointList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
