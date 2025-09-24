package com.genersoft.iot.vmp.jt1078.codec.encode;


import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:10
 * @email qingtaij@163.com
 */
@Slf4j
public class Jt808Encoder extends MessageToByteEncoder<Rs> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Rs msg, ByteBuf out) throws Exception {
        Session session = ctx.channel().attr(Session.KEY).get();

        List<ByteBuf> encodeList = Jt808EncoderCmd.encode(msg, session, session.nextSerialNo());
        if(encodeList!=null && !encodeList.isEmpty()){
            for (ByteBuf byteBuf : encodeList) {
                log.debug("< {} hex:{}", session, ByteBufUtil.hexDump(byteBuf));
                out.writeBytes(byteBuf);
            }
        }
    }
}
