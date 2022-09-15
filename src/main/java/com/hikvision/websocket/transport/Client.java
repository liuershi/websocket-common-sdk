package com.hikvision.websocket.transport;

import com.hikvision.websocket.exception.RemotingException;

/**
 * Client
 *
 * @author zhangwei151
 * @date 2022/9/14 15:59
 */
public interface Client extends Endpoint, Channel {
    /**
     * reconnect.
     */
    void reconnect() throws RemotingException;
}
