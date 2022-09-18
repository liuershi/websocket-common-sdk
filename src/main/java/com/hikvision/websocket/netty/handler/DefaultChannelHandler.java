package com.hikvision.websocket.netty.handler;

import com.hikvision.websocket.exception.RemotingException;
import com.hikvision.websocket.netty.exchange.Response;
import com.hikvision.websocket.netty.remoting.Channel;
import com.hikvision.websocket.netty.remoting.ChannelHandler;

/**
 * @author zhangwei151
 * @date 2022/9/15 21:47
 */
public class DefaultChannelHandler implements ChannelHandler {

    private static volatile DefaultChannelHandler INSTANCE;

    private DefaultChannelHandler() {
    }

    @Override
    public void connected(Channel channel) throws RemotingException {

    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        channel.close();
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {

    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        if (message instanceof Response) {

        }
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {

    }

    public static DefaultChannelHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (DefaultChannelHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DefaultChannelHandler();
                }
            }
        }
        return INSTANCE;
    }
}
