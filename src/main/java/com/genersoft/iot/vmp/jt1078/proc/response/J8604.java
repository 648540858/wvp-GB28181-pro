package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTPolygonArea;
import com.genersoft.iot.vmp.jt1078.bean.JTRectangleArea;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 设置多边形区域
 */
@Setter
@Getter
@MsgId(id = "8604")
public class J8604 extends Rs {

    /**
     * 多边形区域
     */
    private JTPolygonArea polygonArea;


    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(polygonArea.encode());
        return buffer;
    }

}
