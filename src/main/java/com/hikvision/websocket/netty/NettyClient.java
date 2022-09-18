package com.hikvision.websocket.netty;

import com.hikvision.websocket.api.URL;
import com.hikvision.websocket.netty.codec.CodecAdapter;
import com.hikvision.websocket.constants.Constants;
import com.hikvision.websocket.exception.RemotingException;
import com.hikvision.websocket.netty.exchange.Request;
import com.hikvision.websocket.netty.exchange.Response;
import com.hikvision.websocket.netty.handler.NettyClientHandler;
import com.hikvision.websocket.netty.remoting.ChannelHandler;
import com.hikvision.websocket.netty.transport.AbstractClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
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
public class NettyClient extends AbstractClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    /**
     * client cache
     */
    private static final ConcurrentMap<InetSocketAddress, NettyClient> CLIENT_MAP = new ConcurrentHashMap<>();

    /**
     * all client shared worker thread pool
     */
    private static final EventLoopGroup EVENT_LOOP_GROUP = eventLoopGroup(Constants.DEFAULT_IO_THREADS, "NettyClientWorker");

    /**
     * netty client bootstrap
     */
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
    }

    /**
     * Get the encapsulated client through netty's channel
     *
     * @param socketAddress service socket address
     * @param handler       wrapper handler
     * @return
     */
    public static NettyClient getOrAddClient(InetSocketAddress socketAddress, ChannelHandler handler) {
        if (socketAddress == null) return null;

        NettyClient res = CLIENT_MAP.get(socketAddress);
        if (res == null) {
            URL url = new URL(socketAddress.getHostString(), socketAddress.getPort());
            NettyClient nettyClient = new NettyClient(url, handler);
            res = CLIENT_MAP.putIfAbsent(socketAddress, nettyClient);
            if (res == null) {
                res = nettyClient;
            }
        }
        return res;
    }

    /**
     * Remove invalid client
     *
     * @param socketAddress service socket address
     */
    public static void removeClient(InetSocketAddress socketAddress) {
        if (socketAddress != null) {
            NettyClient client = CLIENT_MAP.remove(socketAddress);
            if (client != null) {
                client.close();
            }
        }
    }

    /**
     * init bootstrap
     * @throws Throwable
     */
    @Override
    protected void doOpen() throws Throwable {
        this.bootstrap = new Bootstrap();
        final NettyClientHandler nettyClientHandler = createNettyClientHandler();
        initBootstrap(nettyClientHandler);
    }

    @Override
    protected void doClose() throws Throwable {

    }

    protected NettyClientHandler createNettyClientHandler() {
        return new NettyClientHandler(getUrl(), this, resultCollector);
    }

    protected void initBootstrap(NettyClientHandler nettyClientHandler) {
        final CodecAdapter codecAdapter = new CodecAdapter();

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
                        .addLast("skipDecoder", codecAdapter.getSkipDecoder(1024, 2, 2, -4, 0))
                        .addLast("decoder", codecAdapter.getDecoder())
                        .addLast("encoder", codecAdapter.getEncoder())
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
                            if (logger.isInfoEnabled()) {
                                logger.info("Close old netty channel " + oldChannel + " on create new netty channel " + channel);
                            }
                            oldChannel.close();
                        } finally {
                            NettyChannel.removeChannelIfDisconnected(oldChannel);
                        }
                    }
                } finally {
                    if (NettyClient.this.isClosed()) {
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close new netty channel " + channel + ", because the client closed.");
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
                RemotingException remotingException = new RemotingException(this, "client(url: " + getLocalAddress() + ") failed to connect to server "
                        + getRemoteAddress() + ", error message is:" + cause.getMessage(), cause);

                logger.error("network disconnected. Failed to connect to provider server by other reason.", cause);

                throw remotingException;
            } else {
                // client side timeout
                RemotingException remotingException = new RemotingException(this, "client(url: " + getLocalAddress() + ") failed to connect to server "
                        + getRemoteAddress() + " client-side timeout "
                        + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms) from netty client ");

                logger.error("provider crash. Client-side timeout.", remotingException);

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
            logger.warn(t.getMessage());
        }
    }

    @Override
    protected com.hikvision.websocket.netty.remoting.Channel getChannel() {
        Channel c = channel;
        if (c == null) return null;
        return NettyChannel.getOrAddChannel(channel, getUrl(), this);
    }

    public Future<Response> request(Request request) {
        Future<Response> future = resultCollector.createFuture(request);
        send(request);
        return future;
    }
}
