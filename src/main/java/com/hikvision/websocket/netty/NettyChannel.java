package com.hikvision.websocket.netty;

import com.hikvision.websocket.api.URL;
import com.hikvision.websocket.exception.RemotingException;
import com.hikvision.websocket.netty.transport.AbstractChannel;
import com.hikvision.websocket.netty.remoting.ChannelHandler;
import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintain current active connections
 *
 * @author zhangwei151
 * @date 2022/9/14 18:10
 */
public class NettyChannel extends AbstractChannel {

    private static final Logger logger = LoggerFactory.getLogger(NettyChannel.class);

    /**
     * currently active channel cache
     */
    private static final ConcurrentMap<Channel, NettyChannel> CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * native channel (netty channel)
     */
    private final Channel channel;

    private final AtomicBoolean active = new AtomicBoolean();

    public NettyChannel(Channel channel, URL url, ChannelHandler handler) {
        super(url, handler);
        if (channel == null) {
            throw new IllegalArgumentException("netty channel == null;");
        }
        this.channel = channel;
    }

    public static NettyChannel getChannel(Channel ch) {
        return CHANNEL_MAP.get(ch);
    }

    /**
     * Get the encapsulated channel through netty's channel
     *
     * @param ch netty channel
     * @param url wrapper URL
     * @param handler wrapper handler
     * @return
     */
    public static NettyChannel getOrAddChannel(Channel ch, URL url, ChannelHandler handler) {
        if (ch == null) return null;

        NettyChannel res = CHANNEL_MAP.get(ch);
        if (res == null) {
            NettyChannel nettyChannel = new NettyChannel(ch, url, handler);
            if (ch.isActive()) {
                nettyChannel.markActive(true);
                res = CHANNEL_MAP.putIfAbsent(ch, nettyChannel);
            }
            if (res == null) {
                res = nettyChannel;
            }
        }
        return res;
    }

    /**
     * Remove invalid connections
     *
     * @param ch netty channel
     */
    public static void removeChannelIfDisconnected(Channel ch) {
        if (ch != null && !ch.isActive()) {
            NettyChannel channel = CHANNEL_MAP.remove(ch);
            if (channel != null) {
                channel.markActive(false);
            }
        }
    }

    /**
     * remove channel
     *
     * @param ch netty channel
     */
    public static void removeChannel(Channel ch) {
        if (ch != null) {
            NettyChannel channel = CHANNEL_MAP.remove(ch);
            if (channel != null) {
                channel.markActive(false);
            }
        }
    }

    public void markActive(boolean isActive) {
        active.set(isActive);
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public boolean isConnected() {
        return !isClosed() && active.get();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) channel.localAddress();
    }

    /**
     * 通过netty的channel发送消息以及是否等待数据发送完成
     *
     * @param message 消息
     * @param sent    是否等待数据发送完成
     * @throws RemotingException 如果超时或者方法体中发送异常时抛出该异常
     */
    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        // check channel closed
        super.send(message);

        boolean success = true;
        int timeout = 0;
        try {
            ChannelFuture future = channel.writeAndFlush(message);
            if (sent) {
                // todo timeout set
                timeout = 5000;
                success = future.await(timeout);
            }
            Throwable cause = future.cause();
            if (cause != null) {
                throw cause;
            }
        } catch (Throwable e) {
            removeChannelIfDisconnected(channel);
            throw new RemotingException(this, "Failed to send message " + message + " to " + getRemoteAddress() + ", cause: " + e.getMessage(), e);
        }
        // failed when
        if (!success) {
            throw new RemotingException(this, "Failed to send message " + message + " to " + getRemoteAddress() + "in timeout(" + timeout + "ms) limit");
        }
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            removeChannelIfDisconnected(channel);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Close netty channel " + channel);
            }
            channel.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "NettyChannel [channel=" + channel + "]";
    }
}
