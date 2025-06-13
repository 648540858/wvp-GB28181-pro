package com.genersoft.iot.vmp.gat1400.backend.socket.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ProtocolException;
import java.net.URI;
import java.util.function.Consumer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private final Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);
    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakerFuture;
    private final Consumer<String> consumer;

    public WebSocketClientHandler(URI uri, String user, Consumer<String> consumer) {
        this.consumer = consumer;
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.add("User-Identify", user);
        this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, true,
                headers, 1280000);
    }

    public ChannelFuture handshakerFuture() {
        return this.handshakerFuture;
    }

    /**
     * 处理器加入到处理pipeline后，新建握手等待标识Future
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        handshakerFuture = ctx.newPromise();
    }

    /**
     * 连接建立成功后，发起握手请求
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("连接断开！");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (!this.handshakerFuture.isDone()) {
            this.handshakerFuture.cancel(true);
        }
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (!this.handshakerFuture.isDone()) {
            this.handshakerFuture.setFailure(cause);
        }
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        final Channel channel = ctx.channel();
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            if (!response.decoderResult().isSuccess()) {
                throw new ProtocolException("响应内容解析失败！");
            } else if (!this.handshaker.isHandshakeComplete()) {
                this.handshaker.finishHandshake(channel, (FullHttpResponse) msg);
                handshakerFuture.setSuccess();//标识握手成功
                return;
            }
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            logger.info(new String(response.content().array()));
        }
        final WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            this.consumer.accept(((TextWebSocketFrame) frame).text());
        } else if (frame instanceof CloseWebSocketFrame) {
            channel.close();
        } else if (frame instanceof PongWebSocketFrame) {
            logger.info(frame.toString());
        }
    }
}
