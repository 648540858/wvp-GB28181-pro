package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 查询区域或线路数据
 */
@Setter
@Getter
@MsgId(id = "8608")
public class J8608 extends Rs {


    /**
     * 查询类型, 1 = 查询圆形区域数据 ,2 = 查询矩形区域数据 ,3 = 查询多 边形区域数据 ,4 = 查询线路数据
     */
    private int type;


    /**
     * 要查询的区域或线路的 ID
     */
    private List<Long> idList;


    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(type);
        if (idList == null || idList.isEmpty()) {
            buffer.writeInt(0);
            return buffer;
        }else {
            buffer.writeInt(idList.size());
        }
        for (Long id : idList) {
            buffer.writeInt((int) (id & 0xffffffffL));
        }
        return buffer;
    }

}
