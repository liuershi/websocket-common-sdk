package com.hikvision.websocket.transport;

import com.hikvision.websocket.api.URL;
import com.hikvision.websocket.exception.RemotingException;

import java.net.InetSocketAddress;

/**
 * Endpoint
 *
 * @author zhangwei151
 * @date 2022/9/14 15:26
 */
public interface Endpoint {

    /**
     * get url
     *
     * @return URL
     */
    URL getUrl();

    /**
     * get channel handler.
     *
     * @return channel handler
     */
    ChannelHandler getChannelHandler();

    /**
     * get local address.
     *
     * @return local address.
     */
    InetSocketAddress getLocalAddress();

    /**
     * send message.
     *
     * @param message
     * @throws RemotingException
     */
    void send(Object message) throws RemotingException;

    /**
     * send message.
     *
     * @param message
     * @param sent    already sent to socket?
     */
    void send(Object message, boolean sent) throws RemotingException;

    /**
     * close the channel.
     */
    void close();

    /**
     * Graceful close the channel.
     * @param timeout wait time
     */
    void close(int timeout);

    void startClose();

    /**
     * is closed.
     *
     * @return closed
     */
    boolean isClosed();

}
