package com.hikvision.websocket.netty.exchange;

/**
 * Request
 *
 * @author zhangwei151
 * @date 2022/9/14 19:15
 */
public class Request {

    /**
     * request data, contain request header data
     */
    private byte[] content;

    public Request(byte[] content) {
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
