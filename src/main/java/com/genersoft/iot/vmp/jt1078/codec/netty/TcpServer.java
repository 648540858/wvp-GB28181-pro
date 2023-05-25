package com.genersoft.iot.vmp.jt1078.codec.netty;

import com.genersoft.iot.vmp.jt1078.codec.decode.Jt808Decoder;
import com.genersoft.iot.vmp.jt1078.codec.encode.Jt808Encoder;
import com.genersoft.iot.vmp.jt1078.codec.encode.Jt808EncoderCmd;
import com.genersoft.iot.vmp.jt1078.proc.factory.CodecFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:01
 * @email qingtaij@163.com
 */

public class TcpServer {
    private final static Logger log = LoggerFactory.getLogger(TcpServer.class);

    private final Integer port;
    private boolean isRunning = false;
    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;

    private final ByteBuf DECODER_JT808 = Unpooled.wrappedBuffer(new byte[]{0x7e});

    public TcpServer(Integer port) {
        this.port = port;
    }

    private void startTcpServer() {
        try {
            CodecFactory.init();
            this.bossGroup = new NioEventLoopGroup();
            this.workerGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.group(bossGroup, workerGroup);

            bootstrap.option(NioChannelOption.SO_BACKLOG, 1024)
                    .option(NioChannelOption.SO_REUSEADDR, true)
                    .childOption(NioChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        public void initChannel(NioSocketChannel channel) {
                            channel.pipeline()
                                    .addLast(new IdleStateHandler(10, 0, 0, TimeUnit.MINUTES))
                                    .addLast(new DelimiterBasedFrameDecoder(1024 * 2, DECODER_JT808))
                                    .addLast(new Jt808Decoder())
                                    .addLast(new Jt808Encoder())
                                    .addLast(new Jt808EncoderCmd())
                                    .addLast(new Jt808Handler());
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            // 监听设备TCP端口是否启动成功
            channelFuture.addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("Binding port:{} fail!  cause: {}", port, future.cause().getCause(), future.cause());
                }
            });
            log.info("服务:JT808 Server 启动成功, port:{}", port);
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.warn("服务:JT808 Server 启动异常, port:{},{}", port, e.getMessage(), e);
        } finally {
            stop();
        }
    }

    /**
     * 开启一个新的线程,拉起来Netty
     */
    public synchronized void start() {
        if (this.isRunning) {
            log.warn("服务:JT808 Server 已经启动, port:{}", port);
            return;
        }
        this.isRunning = true;
        new Thread(this::startTcpServer).start();
    }

    public synchronized void stop() {
        if (!this.isRunning) {
            log.warn("服务:JT808 Server 已经停止, port:{}", port);
        }
        this.isRunning = false;
        Future<?> future = this.bossGroup.shutdownGracefully();
        if (!future.isSuccess()) {
            log.warn("bossGroup 无法正常停止", future.cause());
        }
        future = this.workerGroup.shutdownGracefully();
        if (!future.isSuccess()) {
            log.warn("workerGroup 无法正常停止", future.cause());
        }
        log.warn("服务:JT808 Server 已经停止, port:{}", port);
    }
}
