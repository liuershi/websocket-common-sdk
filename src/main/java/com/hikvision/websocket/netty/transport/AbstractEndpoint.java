package com.hikvision.websocket.netty.transport;

import com.hikvision.websocket.api.URL;
import com.hikvision.websocket.netty.remoting.ChannelHandler;


/**
 * AbstractEndpoint
 *
 * @author zhangwei151
 * @date 2022/9/14 15:20
 */
public abstract class AbstractEndpoint extends AbstractPeer {

    private int connectTimeout;

    public AbstractEndpoint(URL url, ChannelHandler handler) {
        super(url, handler);
        this.connectTimeout = url.getConnectTimeout();
    }

    protected int getConnectTimeout() {
        return connectTimeout;
    }
}
