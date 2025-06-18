package com.genersoft.iot.vmp.gat1400.backend.socket.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.function.Consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.stream.ChunkedWriteHandler;

public class WebsocketClient implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(WebsocketClient.class);
    public static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
    private final URI uri;
    private Channel channel;
    private final WebSocketClientHandler handler;

    public WebsocketClient(String uri, String user, Consumer<String> consumer) {
        this.uri = URI.create(uri);
        this.handler = new WebSocketClientHandler(this.uri, user, consumer);
    }

    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            logger.error("错误导致WebSocketClient断开", e);
        }
    }

    public void start() throws Exception {
        Bootstrap client = new Bootstrap();
        client.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 20000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("http-codec", new HttpClientCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65535));
                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(handler);
                    }
                });
        this.channel = client.connect(uri.getHost(), uri.getPort()).sync().channel();
        ChannelFuture future = handler.handshakerFuture();
        future.sync();
        this.channel.closeFuture().sync();
    }

    public void send(final String text) {
        if (this.handler.handshakerFuture().isSuccess()) {
            this.channel.writeAndFlush(new TextWebSocketFrame(text));
        }
    }

    public void close() {
        this.channel.writeAndFlush(new CloseWebSocketFrame());
    }
}
