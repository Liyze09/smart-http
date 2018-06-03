/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: DefaultHttpResponse.java
 * Date: 2018-02-17
 * Author: sandao
 */

package org.smartboot.http.server.http11;

import org.smartboot.http.common.HttpEntity;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.server.handle.HttpHandle;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
public class DefaultHttpResponse implements HttpResponse {

    /**
     * http响应码
     */
    private HttpStatus httpStatus;

    private Map<String, String> headMap = new HashMap<String, String>();

    private HttpOutputStream outputStream;

    public DefaultHttpResponse(AioSession<HttpEntity> session, Http11Request request, HttpHandle responseHandle) {
        this.outputStream = new HttpOutputStream(session, this, request, responseHandle);
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    Map<String, String> getHeadMap() {
        return headMap;
    }

    public void setHeader(String name, String value) {
        headMap.put(name, value);
    }

    public String getHeader(String name) {
        return headMap.get(name);
    }

    public void write(ByteBuffer buffer) throws IOException {
        outputStream.write(buffer);
    }
}
