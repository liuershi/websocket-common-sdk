package com.hikvision.websocket.client;

import com.hikvision.websocket.api.URL;
import com.hikvision.websocket.constants.Constants;
import com.hikvision.websocket.exception.RemotingException;
import com.hikvision.websocket.handler.NettyClientHandler;
import com.hikvision.websocket.transport.ChannelHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.hikvision.websocket.constants.Constants.DEFAULT_CONNECT_TIMEOUT;
import static com.hikvision.websocket.factory.NettyEventLoopFactory.eventLoopGroup;
import static com.hikvision.websocket.factory.NettyEventLoopFactory.socketChannelClass;

/**
 * NettyClient
 *
 * @author zhangwei151
 * @date 2022/9/14 14:54
 */
@Slf4j
public class NettyClient extends AbstractClient {

    /**
     * client cache
     */
    private static final ConcurrentMap<InetSocketAddress, NettyClient> CLIENT_MAP = new ConcurrentHashMap<>();

    /**
     * netty client bootstrap
     */
    private static final EventLoopGroup EVENT_LOOP_GROUP = eventLoopGroup(Constants.DEFAULT_IO_THREADS, "NettyClientWorker");

    private Bootstrap bootstrap;

    /**
     * current channel. Each successful invocation of {@link NettyClient#doConnect()} will
     * replace this with new channel and close old channel.
     * <b>volatile, please copy reference to use.</b>
     */
    private volatile Channel channel;

    /**
     * The constructor of NettyClient.
     * It wil init and start netty.
     */
    public NettyClient(final URL url, final ChannelHandler handler) throws RemotingException {
        super(url, handler);
        this.bootstrap = new Bootstrap();
    }

    /**
     * 通过netty的channel获取封装的client
     *
     * @param url wrapper URL
     * @param handler wrapper handler
     * @return
     */
    public static NettyClient getOrAddClient(URL url, ChannelHandler handler) {
        if (url == null) return null;

        InetSocketAddress inetSocketAddress = new InetSocketAddress(url.getHost(), url.getPort());
        NettyClient res = CLIENT_MAP.get(inetSocketAddress);
        if (res == null) {
            NettyClient nettyClient = new NettyClient(url, handler);
            res = CLIENT_MAP.putIfAbsent(inetSocketAddress, nettyClient);
            if (res == null) {
                res = nettyClient;
            }
        }
        return res;
    }

    /**
     * init bootstrap
     * @throws Throwable
     */
    @Override
    protected void doOpen() throws Throwable {
        final NettyClientHandler nettyClientHandler = createNettyClientHandler();
        initBootstrap(nettyClientHandler);
    }

    protected NettyClientHandler createNettyClientHandler() {
        return new NettyClientHandler(getUrl(), this);
    }

    protected void initBootstrap(NettyClientHandler nettyClientHandler) {
        bootstrap.group(EVENT_LOOP_GROUP)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(socketChannelClass());

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.max(DEFAULT_CONNECT_TIMEOUT, getConnectTimeout()));
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                        // todo 编解码器的创建
                        .addLast("", new LengthFieldBasedFrameDecoder(65535, 2, 2))
                        .addLast("decoder", null)
                        .addLast("encoder", null)
                        .addLast("handler", nettyClientHandler);
            }
        });
    }

    @Override
    protected void doConnect() throws Throwable {
        long start = System.currentTimeMillis();
        ChannelFuture future = bootstrap.connect(getConnectAddress());
        try {
            // wait specified time
            boolean res = future.awaitUninterruptibly(getConnectTimeout(), TimeUnit.MILLISECONDS);
            if (res && future.isSuccess()) {
                Channel channel = future.channel();

                try {
                    // close old channel
                    Channel oldChannel = NettyClient.this.channel;
                    if (oldChannel != null) {
                        try {
                            if (log.isInfoEnabled()) {
                                log.info("Close old netty channel " + oldChannel + " on create new netty channel " + channel);
                            }
                            oldChannel.close();
                        } finally {
                            NettyChannel.removeChannelIfDisconnected(oldChannel);
                        }
                    }
                } finally {
                    if (NettyClient.this.isClosed()) {
                        try {
                            if (log.isInfoEnabled()) {
                                log.info("Close new netty channel " + channel + ", because the client closed.");
                            }
                            channel.close();
                        } finally {
                            NettyClient.this.channel = null;
                            NettyChannel.removeChannelIfDisconnected(channel);
                        }
                    } else {
                        NettyClient.this.channel = channel;
                    }
                }
            } else if (future.cause() != null){
                // connection failed
                Throwable cause = future.cause();
                RemotingException remotingException = new RemotingException(this, "client(url: " + getUrl() + ") failed to connect to server "
                        + getRemoteAddress() + ", error message is:" + cause.getMessage(), cause);

                log.error("network disconnected. Failed to connect to provider server by other reason.", cause);

                throw remotingException;
            } else {
                // client side timeout
                RemotingException remotingException = new RemotingException(this, "client(url: " + getUrl() + ") failed to connect to server "
                        + getRemoteAddress() + " client-side timeout "
                        + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms) from netty client ");

                log.error("provider crash. Client-side timeout.", remotingException);

                throw remotingException;
            }
        } finally {
            // todo
        }
    }

    @Override
    protected void doDisConnect() throws Throwable {
        try {
            NettyChannel.removeChannelIfDisconnected(channel);
        } catch (Throwable t) {
            log.warn(t.getMessage());
        }
    }

    @Override
    protected com.hikvision.websocket.transport.Channel getChannel() {
        Channel c = channel;
        if (c == null) return null;
        return NettyChannel.getOrAddChannel(channel, getUrl(), this);
    }
}
