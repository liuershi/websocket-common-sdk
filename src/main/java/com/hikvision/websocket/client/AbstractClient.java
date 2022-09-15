package com.hikvision.websocket.client;

import com.hikvision.websocket.api.URL;
import com.hikvision.websocket.exception.RemotingException;
import com.hikvision.websocket.transport.AbstractEndpoint;
import com.hikvision.websocket.transport.Channel;
import com.hikvision.websocket.transport.ChannelHandler;
import com.hikvision.websocket.transport.Client;
import com.hikvision.websocket.utils.NetUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * client skeletal implementation
 *
 * @author zhangwei151
 * @date 2022/9/14 14:48
 */
@Slf4j
public abstract class AbstractClient extends AbstractEndpoint implements Client {

    private final Lock connectLock = new ReentrantLock();
    
    public AbstractClient(URL url, ChannelHandler handler) {
        super(url, handler);
        try {
            doOpen();
        } catch (Throwable t) {
            close();
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }
        try {
            // connect.
            connect();
            if (log.isInfoEnabled()) {
                log.info("Start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress() + " connect to the server " + getRemoteAddress());
            }
        } catch (Throwable t) {
            close();
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }
    }

    protected void connect() throws RemotingException {
        connectLock.lock();

        try {
            if (isConnected()) {
                return;
            }

            if (isClosed() || isClosing()) {
                log.warn("No need to connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + ", cause: client status is closed or closing.");
                return;
            }

            doConnect();

            if (!isConnected()) {
                throw new RemotingException(this, "Failed to connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + ", cause: Connect wait timeout: " + getConnectTimeout() + "ms.");

            } else {
                if (log.isInfoEnabled()) {
                    log.info("Successfully connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + ", channel is " + this.getChannel());
                }
            }

        } catch (RemotingException e) {
            throw e;
        } catch (Throwable e) {
            throw new RemotingException(this, "Failed to connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + ", cause: " + e.getMessage(), e);

        } finally {
            connectLock.unlock();
        }
    }

    public InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(getUrl().getHost(), getUrl().getPort());
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        Channel channel = getChannel();
        if (channel == null) {
            return getUrl().toInetSocketAddress();
        }
        return channel.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        Channel channel = getChannel();
        if (channel == null) {
            return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
        }
        return channel.getLocalAddress();
    }

    @Override
    public boolean isConnected() {
        Channel channel = getChannel();
        return channel != null && getChannel().isConnected();
    }

    @Override
    public void reconnect() throws RemotingException {
        connectLock.lock();
        try {
            disconnect();
            connect();
        } finally {
            connectLock.unlock();
        }
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        // default support reconnect
        if (!isConnected()) {
            connect();
        }
        Channel channel = getChannel();
        if (channel == null || !channel.isConnected()) {
            throw new RemotingException(this, "message can not send, because channel is closed . url:" + getUrl());
        }
        channel.send(message, sent);
    }

    public void disconnect() {
        connectLock.lock();
        try {
            try {
                Channel channel = getChannel();
                if (channel != null) {
                    channel.close();
                }
            } catch (Throwable e) {
                log.warn(e.getMessage(), e);
            }
            try {
                doDisConnect();
            } catch (Throwable e) {
                log.warn(e.getMessage(), e);
            }
        } finally {
            connectLock.unlock();
        }
    }

    /**
     * Open client.
     *
     * @throws Throwable
     */
    protected abstract void doOpen() throws Throwable;

    /**
     * Connect to server.
     *
     * @throws Throwable
     */
    protected abstract void doConnect() throws Throwable;

    /**
     * disConnect to server.
     *
     * @throws Throwable
     */
    protected abstract void doDisConnect() throws Throwable;

    /**
     * Get the connected channel.
     *
     * @return channel
     */
    protected abstract Channel getChannel();
}
