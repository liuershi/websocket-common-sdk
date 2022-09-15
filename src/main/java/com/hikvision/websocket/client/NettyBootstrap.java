package com.hikvision.websocket.client;

import com.hikvision.websocket.constants.Constants;
import com.hikvision.websocket.handler.NettyClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;

import java.net.InetSocketAddress;

import static com.hikvision.websocket.constants.Constants.DEFAULT_CONNECT_TIMEOUT;
import static com.hikvision.websocket.factory.NettyEventLoopFactory.eventLoopGroup;
import static com.hikvision.websocket.factory.NettyEventLoopFactory.socketChannelClass;

/**
 * NettyBootstrap
 *
 * @author zhangwei151
 * @date 2022/9/15 10:06
 */
public class NettyBootstrap {

    private static volatile NettyBootstrap INSTANCE;

    /**
     * native netty bootstrap
     */
    private final Bootstrap bootstrap;

    /**
     * netty client bootstrap
     */
    private static final EventLoopGroup EVENT_LOOP_GROUP = eventLoopGroup(Constants.DEFAULT_IO_THREADS, "NettyClientWorker");

    private NettyBootstrap() {
        this.bootstrap = new Bootstrap();
        initBootstrap();
    }

    protected void initBootstrap() {
        bootstrap.group(EVENT_LOOP_GROUP)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(socketChannelClass());

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.max(DEFAULT_CONNECT_TIMEOUT, /*getConnectTimeout()*/0 /* todo */));
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public static NettyBootstrap getInstance() {
        if (INSTANCE == null) {
            synchronized (NettyBootstrap.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NettyBootstrap();
                }
            }
        }
        return INSTANCE;
    }
}
