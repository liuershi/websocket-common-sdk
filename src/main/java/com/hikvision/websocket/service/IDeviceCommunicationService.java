package com.hikvision.websocket.service;

import com.hikvision.websocket.netty.exchange.Request;
import com.hikvision.websocket.netty.exchange.Response;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Device communication interface
 *
 * @author zhangwei151
 * @date 2022/9/17 12:09
 */
public interface IDeviceCommunicationService {

    /**
     * connect the specified device socket
     *
     * @param address device socket address
     */
    void connect(InetSocketAddress address);

    /**
     * disconnect the specified device socket
     *
     * @param address device socket address
     */
    void disconnected(InetSocketAddress address);

    /**
     * synchronous send a request to the specified device
     *
     * @param address device socket address
     * @param request request data
     * @return response body
     * @throws TimeoutException if the wait timed out（default 10 seconds）
     */
    Response send(InetSocketAddress address, Request request) throws TimeoutException;

    /**
     * asynchronous send a request to the specified device
     *
     * @param address device socket address
     * @param request request data
     * @return asynchronous result
     */
    Future<Response> sendAsync(InetSocketAddress address, Request request);
}
