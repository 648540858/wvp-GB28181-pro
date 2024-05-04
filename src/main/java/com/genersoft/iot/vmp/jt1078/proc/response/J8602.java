package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTCircleArea;
import com.genersoft.iot.vmp.jt1078.bean.JTRectangleArea;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;

/**
 * 设置矩形区域
 */
@MsgId(id = "8602")
public class J8602 extends Rs {

    /**
     * 设置属性, 0：更新区域； 1：追加区域； 2：修改区域
     */
    private int attribute;

    /**
     * 区域项
     */
    private List<JTRectangleArea> rectangleAreas;


    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(attribute);
        buffer.writeByte(rectangleAreas.size());
        if (rectangleAreas.isEmpty()) {
            return buffer;
        }
        for (JTRectangleArea area : rectangleAreas) {
            buffer.writeBytes(area.encode());
        }
        return buffer;
    }

    public int getAttribute() {
        return attribute;
    }

    public void setAttribute(int attribute) {
        this.attribute = attribute;
    }

    public List<JTRectangleArea> getRectangleAreas() {
        return rectangleAreas;
    }

    public void setRectangleAreas(List<JTRectangleArea> rectangleAreas) {
        this.rectangleAreas = rectangleAreas;
    }
}
