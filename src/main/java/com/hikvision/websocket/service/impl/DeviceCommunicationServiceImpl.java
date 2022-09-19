package com.hikvision.websocket.service.impl;

import com.hikvision.websocket.netty.handler.DefaultChannelHandler;
import com.hikvision.websocket.netty.NettyClient;
import com.hikvision.websocket.netty.exchange.Request;
import com.hikvision.websocket.netty.exchange.Response;
import com.hikvision.websocket.service.IDeviceCommunicationService;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * DeviceCommunicationServiceImpl
 *
 * @author zhangwei151
 * @date 2022/9/17 22:41
 */
public class DeviceCommunicationServiceImpl implements IDeviceCommunicationService {

    private final int requestTimeout;

    public DeviceCommunicationServiceImpl(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @Override
    public void connect(InetSocketAddress address) {
       NettyClient.getOrAddClient(address, DefaultChannelHandler.getInstance());
    }

    @Override
    public void disconnected(InetSocketAddress address) {
        NettyClient.removeClient(address);
    }

    @Override
    public Response send(InetSocketAddress address, Request request) throws TimeoutException {
        NettyClient client = NettyClient.getOrAddClient(address, DefaultChannelHandler.getInstance());
        Future<Response> future = client.request(request);
        try {
            return future.get(requestTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new TimeoutException("Exceeds the default wait time of 10 seconds");
        }
    }

    @Override
    public Future<Response> sendAsync(InetSocketAddress address, Request request) {
        return NettyClient.getOrAddClient(address, DefaultChannelHandler.getInstance()).request(request);
    }

    @Override
    public void confirm(InetSocketAddress address, Request request) {
        NettyClient.getOrAddClient(address, DefaultChannelHandler.getInstance()).send(request);
    }
}
