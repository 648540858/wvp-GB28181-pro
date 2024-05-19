package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTQueryMediaDataCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单条存储多媒体数据检索上传命令
 */
@MsgId(id = "8805")
public class J8805 extends Rs {

    /**
     * 多媒体 ID
     */
    private Long mediaId;

    /**
     * 删除标志, 0:保留；1:删除, 存储多媒体数据上传命令中使用
     */
    private Integer delete;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt((int) (mediaId & 0xffffffffL));
        byteBuf.writeByte(delete);
        return byteBuf;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public Integer getDelete() {
        return delete;
    }

    public void setDelete(Integer delete) {
        this.delete = delete;
    }
}
