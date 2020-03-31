/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: WebSocketMessageProcessor.java
 * Date: 2020-03-29
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server;

import org.smartboot.http.HttpResponse;
import org.smartboot.http.enums.HttpStatus;
import org.smartboot.http.server.handle.HttpHandle;
import org.smartboot.http.utils.HttpHeaderConstant;
import org.smartboot.http.utils.SHA1;

import java.io.IOException;
import java.util.Base64;

/**
 * @author 三刀
 * @version V1.0 , 2020/3/29
 */
public class WebSocketHandle extends HttpHandle<WebSocketRequest> {
    public static final String WEBSOCKET_13_ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private final RFC2612RequestHandle rfc2612RequestHandle = new RFC2612RequestHandle();

    @Override
    public void doHandle(WebSocketRequest request, HttpResponse response) throws IOException {
        if (request.getWebsocketStatus() == WebSocketRequest.WebsocketStatus.HandShake) {
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
            request.setWebsocketStatus(WebSocketRequest.WebsocketStatus.DataFrame);
        } else {
            doNext(request, response);
        }

    }
}
