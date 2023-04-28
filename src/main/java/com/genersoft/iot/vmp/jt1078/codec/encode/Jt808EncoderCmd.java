package com.genersoft.iot.vmp.jt1078.codec.encode;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.entity.Cmd;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.util.Bin;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ByteProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.LinkedList;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:25
 * @email qingtaij@163.com
 */
public class Jt808EncoderCmd extends MessageToByteEncoder<Cmd> {
    private final static Logger log = LoggerFactory.getLogger(Jt808EncoderCmd.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Cmd cmd, ByteBuf out) throws Exception {
        Session session = ctx.channel().attr(Session.KEY).get();
        Rs msg = cmd.getRs();
        ByteBuf encode = encode(msg, session, cmd.getPackageNo().intValue());
        if (encode != null) {
            log.info("< {} hex:{}", session, ByteBufUtil.hexDump(encode));
            out.writeBytes(encode);
        }
    }


    public static ByteBuf encode(Rs msg, Session session, Integer packageNo) {
        String id = msg.getClass().getAnnotation(MsgId.class).id();
        if (!StringUtils.hasLength(id)) {
            log.error("Not find msgId");
            return null;
        }

        ByteBuf byteBuf = Unpooled.buffer();

        byteBuf.writeBytes(ByteBufUtil.decodeHexDump(id));

        ByteBuf encode = msg.encode();

        Header header = msg.getHeader();
        if (header == null) {
            header = session.getHeader();
        }

        if (header.is2019Version()) {
            // 消息体属性
            byteBuf.writeShort(encode.readableBytes() | 1 << 14);

            // 版本号
            byteBuf.writeByte(header.getVersion());

            // 终端手机号
            byteBuf.writeBytes(ByteBufUtil.decodeHexDump(Bin.strHexPaddingLeft(header.getDevId(), 20)));
        } else {
            // 消息体属性
            byteBuf.writeShort(encode.readableBytes());

            byteBuf.writeBytes(ByteBufUtil.decodeHexDump(Bin.strHexPaddingLeft(header.getDevId(), 12)));
        }

        // 消息体流水号
        byteBuf.writeShort(packageNo);

        // 写入消息体
        byteBuf.writeBytes(encode);

        // 计算校验码，并反转义
        byteBuf = escapeAndCheck0(byteBuf);
        return byteBuf;
    }


    private static final ByteProcessor searcher = value -> !(value == 0x7d || value == 0x7e);

    //转义与校验
    public static ByteBuf escapeAndCheck0(ByteBuf source) {

        sign(source);

        int low = source.readerIndex();
        int high = source.writerIndex();

        LinkedList<ByteBuf> bufList = new LinkedList<>();
        int mark, len;
        while ((mark = source.forEachByte(low, high - low, searcher)) > 0) {

            len = mark + 1 - low;
            ByteBuf[] slice = slice(source, low, len);
            bufList.add(slice[0]);
            bufList.add(slice[1]);
            low += len;
        }

        if (bufList.size() > 0) {
            bufList.add(source.slice(low, high - low));
        } else {
            bufList.add(source);
        }

        ByteBuf delimiter = Unpooled.buffer(1, 1).writeByte(0x7e).retain();
        bufList.addFirst(delimiter);
        bufList.addLast(delimiter);

        CompositeByteBuf byteBufLs = Unpooled.compositeBuffer(bufList.size());
        byteBufLs.addComponents(true, bufList);
        return byteBufLs;
    }

    public static void sign(ByteBuf buf) {
        byte checkCode = bcc(buf);
        buf.writeByte(checkCode);
    }

    public static byte bcc(ByteBuf byteBuf) {
        byte cs = 0;
        while (byteBuf.isReadable())
            cs ^= byteBuf.readByte();
        byteBuf.resetReaderIndex();
        return cs;
    }

    protected static ByteBuf[] slice(ByteBuf byteBuf, int index, int length) {
        byte first = byteBuf.getByte(index + length - 1);

        ByteBuf[] byteBufList = new ByteBuf[2];
        byteBufList[0] = byteBuf.retainedSlice(index, length);

        if (first == 0x7d) {
            byteBufList[1] = Unpooled.buffer(1, 1).writeByte(0x01);
        } else {
            byteBuf.setByte(index + length - 1, 0x7d);
            byteBufList[1] = Unpooled.buffer(1, 1).writeByte(0x02);
        }
        return byteBufList;
    }
}
