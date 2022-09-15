package com.hikvision.websocket.exchange;

import com.hikvision.websocket.api.URL;

import java.util.concurrent.Callable;

/**
 * @author zhangwei151
 * @date 2022/9/14 19:15
 */
public class Request<V> {

    private URL url;

    private byte[] content;

    private Callable<V> callable;

    public Request(URL url, byte[] content, Callable<V> callable) {
        this.url = url;
        this.content = content;
        this.callable = callable;
    }
}
