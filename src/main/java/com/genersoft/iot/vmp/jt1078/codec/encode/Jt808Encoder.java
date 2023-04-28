package com.genersoft.iot.vmp.jt1078.codec.encode;


import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:10
 * @email qingtaij@163.com
 */
public class Jt808Encoder extends MessageToByteEncoder<Rs> {
    private final static Logger log = LoggerFactory.getLogger(Jt808Encoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Rs msg, ByteBuf out) throws Exception {
        Session session = ctx.channel().attr(Session.KEY).get();

        ByteBuf encode = Jt808EncoderCmd.encode(msg, session, session.nextSerialNo());
        if(encode!=null){
            log.info("< {} hex:{}", session, ByteBufUtil.hexDump(encode));
            out.writeBytes(encode);
        }
    }


}
