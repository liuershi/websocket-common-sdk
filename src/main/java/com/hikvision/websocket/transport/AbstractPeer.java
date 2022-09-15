package com.hikvision.websocket.transport;

import com.hikvision.websocket.api.URL;
import com.hikvision.websocket.exception.RemotingException;

/**
 * AbstractPeer
 *
 * @author zhangwei151
 * @date 2022/9/14 15:29
 */
public abstract class AbstractPeer implements Endpoint, ChannelHandler {

    private final ChannelHandler handler;

    private volatile URL url;

    // closing closed means the process is being closed and close is finished
    private volatile boolean closing;

    private volatile boolean closed;

    public AbstractPeer(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.handler = handler;
        this.url = url;
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @Override
    public void close(int timeout) {
        close();
    }

    @Override
    public void startClose() {
        if (isClosed()) return;
        closing = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public boolean isClosing() {
        return !closed && closing;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return handler;
    }

    @Override
    public void connected(Channel channel) throws RemotingException {
        if (closed) return;
        handler.connected(channel);
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        handler.disconnected(channel);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        if (closed) return;
        handler.sent(channel, message);
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        if (closed) return;
        handler.received(channel, message);
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        handler.caught(channel, exception);
    }

    @Override
    public void send(Object message) throws RemotingException {
        send(message, false);
    }
}
