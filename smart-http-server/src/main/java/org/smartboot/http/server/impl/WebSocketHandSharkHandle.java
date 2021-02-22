/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: WebSocketHandSharkHandle.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server.impl;

import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.common.enums.WebsocketStatus;
import org.smartboot.http.common.utils.HttpHeaderConstant;
import org.smartboot.http.common.utils.SHA1;
import org.smartboot.http.server.WebSocketHandle;
import org.smartboot.http.server.WebSocketRequest;
import org.smartboot.http.server.WebSocketResponse;

import java.io.IOException;
import java.util.Base64;

/**
 * websocket握手请求
 *
 * @author 三刀
 * @version V1.0 , 2020/3/29
 */
class WebSocketHandSharkHandle extends WebSocketHandle {
    public static final String WEBSOCKET_13_ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private final RFC2612RequestHandle rfc2612RequestHandle = new RFC2612RequestHandle();

    @Override
    public void doHandle(WebSocketRequest req, WebSocketResponse resp) throws IOException {
        WebSocketResponseImpl response = (WebSocketResponseImpl) resp;
        WebSocketRequestImpl request = (WebSocketRequestImpl) req;
        if (request.getWebsocketStatus() == WebsocketStatus.HandShake) {
            //Http规范校验
            rfc2612RequestHandle.doHandle(request, response);

            String key = request.getHeader(HttpHeaderConstant.Names.Sec_WebSocket_Key);
            String acceptSeed = key + WEBSOCKET_13_ACCEPT_GUID;
            byte[] sha1 = SHA1.encode(acceptSeed);
            String accept = Base64.getEncoder().encodeToString(sha1);
            response.setHttpStatus(HttpStatus.SWITCHING_PROTOCOLS);
            response.setHeader(HttpHeaderConstant.Names.UPGRADE, HttpHeaderConstant.Values.WEBSOCKET);
            response.setHeader(HttpHeaderConstant.Names.CONNECTION, HttpHeaderConstant.Values.UPGRADE);
            response.setHeader(HttpHeaderConstant.Names.Sec_WebSocket_Accept, accept);
            response.getOutputStream().flush();

            doNext(request, response);
            request.setWebsocketStatus(WebsocketStatus.DataFrame);
        } else {
            doNext(request, response);
        }
    }
}