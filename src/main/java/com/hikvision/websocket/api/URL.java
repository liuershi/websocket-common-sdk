package com.hikvision.websocket.api;

import java.net.InetSocketAddress;

/**
 * URL - Uniform Resource Locator (Immutable, ThreadSafe)
 *
 * @author zhangwei151
 * @date 2022/9/14 15:04
 * @see java.net.URL
 */
public class URL {

    protected String host;
    protected int port;
    protected int connectTimeout;

    // cache
    protected transient String rawAddress;
    protected transient long timestamp;

    public URL(String host, int port) {
        this(host, port, 10000);
    }

    public URL(String host, int port, int connectTimeout) {
        this.host = host;
        this.port = port;
        this.connectTimeout = connectTimeout;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public String getRawAddress() {
        return rawAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }


    public InetSocketAddress toInetSocketAddress() {
        return new InetSocketAddress(getHost(), getPort());
    }
}
