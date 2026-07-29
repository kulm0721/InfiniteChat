package com.shanyangcode.realtimeservice.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.nio.NioSctpServerChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.NettyRuntime;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyService {

    private final int port = 9101;

    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);

    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup(NettyRuntime.availableProcessors() * 2);

    @PostConstruct
    public void start() throws InterruptedException {
        run();
    }
    public void run() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        ChannelPipeline channelPipeline=socketChannel.pipeline();
                        channelPipeline.addLast(new HttpServerCodec());
                        channelPipeline.addLast(new HttpObjectAggregator(65536));
                        channelPipeline.addLast(new WebSocketServerProtocolHandler("/ws/netty"));
                        channelPipeline.addLast(new WebSocketHandler());
                    }
                });
        serverBootstrap.bind(port).sync();
    }

    @PreDestroy
    public void destroy() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
