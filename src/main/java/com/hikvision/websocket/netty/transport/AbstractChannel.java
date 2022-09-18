package com.hikvision.websocket.netty.transport;

import com.hikvision.websocket.api.URL;
import com.hikvision.websocket.exception.RemotingException;
import com.hikvision.websocket.netty.remoting.ChannelHandler;
import com.hikvision.websocket.netty.remoting.Channel;

/**
 * AbstractChannel
 *
 * @author zhangwei151
 * @date 2022/9/14 18:10
 */
public abstract class AbstractChannel extends AbstractPeer implements Channel {

    public AbstractChannel(URL url, ChannelHandler handler) {
        super(url, handler);
    }

    @Override
    public void send(Object message) throws RemotingException {
        if (isClosed()) {
            throw new RemotingException(this, "Failed to send message "
                    + (message == null ? "" : message.getClass().getName()) + ":" + message
                    + ", cause: Channel closed. channel: " + getLocalAddress() + " -> " + getRemoteAddress());
        }
    }

    @Override
    public String toString() {
        return getLocalAddress() + " -> " + getRemoteAddress();
    }
}
