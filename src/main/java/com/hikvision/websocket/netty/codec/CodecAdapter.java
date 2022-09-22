package com.hikvision.websocket.netty.codec;

import com.hikvision.websocket.netty.exchange.Request;
import com.hikvision.websocket.netty.exchange.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * codec adapter
 *
 * @author zhangwei151
 * @date 2022/9/18 16:53
 */
public class CodecAdapter {


    /*
     * 28byte in total
     * |0--------------1|2--------------3|4-------------15|16-------------19|20------------21|22------------|23------------27|
     *   wPakageHeader       wLength        struAddress         dwCommand         wStatus         byVersion     byRes[5]
     */
    private static final int RESPONSE_HEAD_LENGTH = 28;

    public ChannelHandler getEncoder() {
        return new RequestMessageEncoder();
    }

    public ChannelHandler getDecoder() {
        return new ResponseMessageDecoder();
    }

    public ChannelHandler getSkipDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        return new SkipServerEchoEncoder(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    static class SkipServerEchoEncoder extends LengthFieldBasedFrameDecoder {

        public SkipServerEchoEncoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
            super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
        }

        @Override
        protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
            // 本地模拟过程中初始连接设备时设备端会响应固定四字节的应答
            // 后续通信中会影响解码器的正常工作，故在此特判有且仅当当前
            // 可读字节数为4时跳过
            // 可能会影响 LengthFieldBasedFrameDecoder 的正常工作 (后续实际部署测试)
            if (in.readableBytes() == 4) {
                in.skipBytes(in.readableBytes());
                return null;
            }
            return super.decode(ctx, in);
        }
    }

    static class RequestMessageEncoder extends MessageToByteEncoder<Request> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Request msg, ByteBuf out) throws Exception {
            if (msg != null && msg.getContent() != null) {
                out.writeBytes(msg.getContent());
            }
        }
    }

    static class ResponseMessageDecoder extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
            ByteBuf header = msg.readBytes(RESPONSE_HEAD_LENGTH);
            byte[] bytes = new byte[RESPONSE_HEAD_LENGTH];
            header.readBytes(bytes);
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            // data packet head == 0xaabb
            buf.getChar();
            // data body length
            int bodyLen = (buf.getChar() & 0xFFFF) - RESPONSE_HEAD_LENGTH;

            int readableBytesLen = msg.readableBytes();
            if (readableBytesLen >= bodyLen) {
                byte[] contentBytes = new byte[bodyLen + RESPONSE_HEAD_LENGTH];
                System.arraycopy(bytes, 0, contentBytes, 0, RESPONSE_HEAD_LENGTH);
                msg.readBytes(contentBytes, RESPONSE_HEAD_LENGTH, bodyLen);
                Response response = new Response(contentBytes);
                out.add(response);
            }
            header.release();
        }
    }
}
