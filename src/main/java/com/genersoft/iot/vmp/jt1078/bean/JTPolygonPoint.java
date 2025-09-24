package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "多边形区域的顶点")
public class JTPolygonPoint {

    @Schema(description = "顶点纬度")
    private Double latitude;

    @Schema(description = "顶点经度")
    private Double longitude;

    public ByteBuf encode(){
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt((int) (Math.round((latitude * 1000000)) & 0xffffffffL));
        byteBuf.writeInt((int) (Math.round((longitude * 1000000)) & 0xffffffffL));
        return byteBuf;
    }

}
