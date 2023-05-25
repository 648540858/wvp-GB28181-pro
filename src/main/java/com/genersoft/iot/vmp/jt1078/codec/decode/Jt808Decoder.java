package com.genersoft.iot.vmp.jt1078.codec.decode;

import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.factory.CodecFactory;
import com.genersoft.iot.vmp.jt1078.proc.request.Re;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:10
 * @email qingtaij@163.com
 */
public class Jt808Decoder extends ByteToMessageDecoder {
    private final static Logger log = LoggerFactory.getLogger(Jt808Decoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Session session = ctx.channel().attr(Session.KEY).get();
        log.info("> {} hex:{}", session, ByteBufUtil.hexDump(in));

        try {
            ByteBuf buf = unEscapeAndCheck(in);

            Header header = new Header();
            header.setMsgId(ByteBufUtil.hexDump(buf.readSlice(2)));
            header.setMsgPro(buf.readUnsignedShort());
            if (header.is2019Version()) {
                header.setVersion(buf.readUnsignedByte());
                String devId = ByteBufUtil.hexDump(buf.readSlice(10));
                header.setDevId(devId.replaceFirst("^0*", ""));
            } else {
                header.setDevId(ByteBufUtil.hexDump(buf.readSlice(6)).replaceFirst("^0*", ""));
            }
            header.setSn(buf.readUnsignedShort());

            Re handler = CodecFactory.getHandler(header.getMsgId());
            if (handler == null) {
                log.error("get msgId is null {}", header.getMsgId());
                return;
            }
            Rs decode = handler.decode(buf, header, session);
            if (decode != null) {
                out.add(decode);
            }
        } finally {
            in.skipBytes(in.readableBytes());
        }


    }


    /**
     * 转义与验证校验码
     *
     * @param byteBuf 转义Buf
     * @return 转义好的数据
     */
    public ByteBuf unEscapeAndCheck(ByteBuf byteBuf) throws Exception {
        int low = byteBuf.readerIndex();
        int high = byteBuf.writerIndex();
        byte checkSum = 0;
        int calculationCheckSum = 0;

        byte aByte = byteBuf.getByte(high - 2);
        byte protocolEscapeFlag7d = 0x7d;
        //0x7d转义
        byte protocolEscapeFlag01 = 0x01;
        //0x7e转义
        byte protocolEscapeFlag02 = 0x02;
        if (aByte == protocolEscapeFlag7d) {
            byte b2 = byteBuf.getByte(high - 1);
            if (b2 == protocolEscapeFlag01) {
                checkSum = protocolEscapeFlag7d;
            } else if (b2 == protocolEscapeFlag02) {
                checkSum = 0x7e;
            } else {
                log.error("转义1异常:{}", ByteBufUtil.hexDump(byteBuf));
                throw new Exception("转义错误");
            }
            high = high - 2;
        } else {
            high = high - 1;
            checkSum = byteBuf.getByte(high);
        }
        List<ByteBuf> bufList = new ArrayList<>();
        int index = low;
        while (index < high) {
            byte b = byteBuf.getByte(index);
            if (b == protocolEscapeFlag7d) {
                byte c = byteBuf.getByte(index + 1);
                if (c == protocolEscapeFlag01) {
                    ByteBuf slice = slice0x01(byteBuf, low, index);
                    bufList.add(slice);
                    b = protocolEscapeFlag7d;
                } else if (c == protocolEscapeFlag02) {
                    ByteBuf slice = slice0x02(byteBuf, low, index);
                    bufList.add(slice);
                    b = 0x7e;
                } else {
                    log.error("转义2异常:{}", ByteBufUtil.hexDump(byteBuf));
                    throw new Exception("转义错误");
                }
                index += 2;
                low = index;
            } else {
                index += 1;
            }
            calculationCheckSum = calculationCheckSum ^ b;
        }

        if (calculationCheckSum == checkSum) {
            if (bufList.size() == 0) {
                return byteBuf.slice(low, high);
            } else {
                bufList.add(byteBuf.slice(low, high - low));
                return new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, false, bufList.size(), bufList);
            }
        } else {
            log.info("{} 解析校验码:{}--计算校验码:{}", ByteBufUtil.hexDump(byteBuf), checkSum, calculationCheckSum);
            throw new Exception("校验码错误!");
        }
    }


    private ByteBuf slice0x01(ByteBuf buf, int low, int sign) {
        return buf.slice(low, sign - low + 1);
    }

    private ByteBuf slice0x02(ByteBuf buf, int low, int sign) {
        buf.setByte(sign, 0x7e);
        return buf.slice(low, sign - low + 1);
    }
}
