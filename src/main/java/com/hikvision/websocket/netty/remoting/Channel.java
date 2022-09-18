package com.hikvision.websocket.netty.remoting;

import java.net.InetSocketAddress;

/**
 * Channel
 *
 * @author zhangwei151
 * @date 2022/9/14 15:59
 */
public interface Channel extends Endpoint {

    /**
     * get remote address.
     *
     * @return remote address.
     */
    InetSocketAddress getRemoteAddress();

    /**
     * is connected.
     *
     * @return connected
     */
    boolean isConnected();
}
