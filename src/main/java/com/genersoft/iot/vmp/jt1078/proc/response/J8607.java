package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;

/**
 * 删除路线
 */
@MsgId(id = "8605")
public class J8607 extends Rs {


    /**
     * 待删除的路线ID
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
