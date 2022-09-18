package com.hikvision.websocket.netty.handler;

import com.hikvision.websocket.api.URL;
import com.hikvision.websocket.netty.NettyChannel;
import com.hikvision.websocket.netty.exchange.Request;
import com.hikvision.websocket.netty.exchange.Response;
import com.hikvision.websocket.netty.exchange.ResultCollector;
import com.hikvision.websocket.netty.remoting.ChannelHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhangwei151
 * @date 2022/9/14 15:35
 */
@io.netty.channel.ChannelHandler.Sharable
public class NettyClientHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);

    private final URL url;

    private final ChannelHandler handler;

    private final ResultCollector listener;

    public NettyClientHandler(URL url, ChannelHandler handler, ResultCollector listener) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }
        this.url = url;
        this.handler = handler;
        this.listener = listener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        handler.connected(channel);
        if (logger.isInfoEnabled()) {
            logger.info("The connection of " + channel.getLocalAddress() + " -> " + channel.getRemoteAddress() + " is established.");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        try {
            handler.disconnected(channel);
        } finally {
            NettyChannel.removeChannel(ctx.channel());
        }

        if (logger.isInfoEnabled()) {
            logger.info("The connection of " + channel.getLocalAddress() + " -> " + channel.getRemoteAddress() + " is disconnected.");
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        listener.received(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        boolean isRequest = msg instanceof Request;

        // 需要保证出站事件的正确，在大多数情况下，都是由于编码器
        // 工作错误导致的，在出错时直接返回
        promise.addListener(future -> {
            if (future.isSuccess()) {
                handler.sent(channel, msg);
                return;
            }

            Throwable cause = future.cause();
            if (cause != null && isRequest) {
                listener.received(buildErrorResponse(cause));
            }
        });
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 使用心跳保活时
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        try {
            handler.caught(channel, cause);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
    }

    /**
     * build a bad request's response
     *
     * @param t       the throwable. In most cases, serialization fails.
     * @return the response
     */
    private static Response buildErrorResponse(Throwable t) {
        return new Response(Response.BAD_REQUEST, t.getMessage());
    }
}
