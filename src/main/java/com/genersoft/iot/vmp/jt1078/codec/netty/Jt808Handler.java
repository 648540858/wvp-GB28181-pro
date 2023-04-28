package com.genersoft.iot.vmp.jt1078.codec.netty;

import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:14
 * @email qingtaij@163.com
 */
public class Jt808Handler extends ChannelInboundHandlerAdapter {

    private final static Logger log = LoggerFactory.getLogger(Jt808Handler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Rs) {
            ctx.writeAndFlush(msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        Session session = SessionManager.INSTANCE.newSession(channel);
        channel.attr(Session.KEY).set(session);
        log.info("> Tcp connect {}", session);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Session session = ctx.channel().attr(Session.KEY).get();
        log.info("< Tcp disconnect {}", session);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        Session session = ctx.channel().attr(Session.KEY).get();
        String message = e.getMessage();
        if (message.toLowerCase().contains("Connection reset by peer".toLowerCase())) {
            log.info("< exception{} {}", session, e.getMessage());
        } else {
            log.info("< exception{} {}", session, e.getMessage(), e);
        }

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            IdleState state = event.state();
            if (state == IdleState.READER_IDLE || state == IdleState.WRITER_IDLE) {
                Session session = ctx.channel().attr(Session.KEY).get();
                log.warn("< Proactively disconnect{}", session);
                ctx.close();
            }
        }
    }

}
