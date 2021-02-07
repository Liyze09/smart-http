/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpRequestProtocol.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server.impl;

import org.smartboot.http.common.utils.AttachKey;
import org.smartboot.http.common.utils.Attachment;
import org.smartboot.http.server.decode.Decoder;
import org.smartboot.http.server.decode.HttpMethodDecoder;
import org.smartboot.http.server.decode.WebSocketFrameDecoder;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/31
 */
public class HttpRequestProtocol implements Protocol<Request> {

    public static final AttachKey<WebSocketRequestImpl> ATTACH_KEY_WS_REQ = AttachKey.valueOf("ws");
    /**
     * 普通Http消息解码完成
     */
    public static final Decoder HTTP_FINISH_DECODER = (byteBuffer, aioSession, request) -> null;
    /**
     * websocket握手消息
     */
    public static final Decoder WS_HANDSHARK_DECODER = (byteBuffer, aioSession, request) -> null;
    /**
     * websocket负载数据读取成功
     */
    public static final Decoder WS_FRAME_DECODER = (byteBuffer, aioSession, request) -> null;
    static final AttachKey<Request> ATTACH_KEY_REQUEST = AttachKey.valueOf("request");

    private static final ThreadLocal<ByteBuffer> CHAR_CACHE_LOCAL = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            return ByteBuffer.allocate(1024);
        }
    };
    private static final AttachKey<Decoder> ATTACH_KEY_DECHDE_CHAIN = AttachKey.valueOf("decodeChain");

    private final HttpMethodDecoder httpMethodDecoder = new HttpMethodDecoder();

    private final WebSocketFrameDecoder wsFrameDecoder = new WebSocketFrameDecoder();

    @Override
    public Request decode(ByteBuffer buffer, AioSession session) {
        Attachment attachment = session.getAttachment();
        Request request = attachment.get(ATTACH_KEY_REQUEST);
        Decoder decodeChain = attachment.get(ATTACH_KEY_DECHDE_CHAIN);
        if (decodeChain == null) {
            decodeChain = httpMethodDecoder;
        }

        decodeChain = decodeChain.decode(buffer, session, request);

        if (decodeChain == HTTP_FINISH_DECODER || decodeChain == WS_HANDSHARK_DECODER || decodeChain == WS_FRAME_DECODER) {
            if (decodeChain == HTTP_FINISH_DECODER) {
                attachment.remove(ATTACH_KEY_DECHDE_CHAIN);
            } else {
                attachment.put(ATTACH_KEY_DECHDE_CHAIN, wsFrameDecoder);
            }
            return request;
        }
        attachment.put(ATTACH_KEY_DECHDE_CHAIN, decodeChain);
        if (buffer.remaining() == buffer.capacity()) {
            throw new RuntimeException("buffer is too small when decode " + decodeChain.getClass().getName() + " ," + request);
        }
        return null;
    }
}

