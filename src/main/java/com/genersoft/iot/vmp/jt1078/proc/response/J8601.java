package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTCircleArea;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;

/**
 * 删除圆形区域
 */
@MsgId(id = "8601")
public class J8601 extends Rs {


    /**
     * 待删除的区域ID
     */
    private List<Long> idList;


    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        if (idList == null || idList.isEmpty()) {
            buffer.writeByte(0);
            return buffer;
        }else {
            buffer.writeByte(idList.size());
        }
        for (Long id : idList) {
            buffer.writeInt((int) (id & 0xffffffffL));
        }
        return buffer;
    }

    public List<Long> getIdList() {
        return idList;
    }

    public void setIdList(List<Long> idList) {
        this.idList = idList;
    }
}
