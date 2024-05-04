package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTPolygonArea;
import com.genersoft.iot.vmp.jt1078.bean.JTRoute;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 设置路线
 */
@MsgId(id = "8606")
public class J8606 extends Rs {

    /**
     * 路线
     */
    private JTRoute route;


    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(route.encode());
        return buffer;
    }

    public JTRoute getRoute() {
        return route;
    }

    public void setRoute(JTRoute route) {
        this.route = route;
    }
}
