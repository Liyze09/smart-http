/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: RequestLineDecoder.java
 * Date: 2020-03-30
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server.decode;

import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.common.exception.HttpException;
import org.smartboot.http.common.utils.Constant;
import org.smartboot.http.common.utils.StringUtils;
import org.smartboot.http.server.HttpServerConfiguration;
import org.smartboot.http.server.impl.Request;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2020/3/30
 */
class HttpProtocolDecoder extends AbstractDecoder {

    private final HttpHeaderDecoder decoder = new HttpHeaderDecoder(getConfiguration());

    private final AbstractDecoder lfDecoder = new AbstractDecoder(getConfiguration()) {
        @Override
        public Decoder decode(ByteBuffer byteBuffer, AioSession aioSession, Request request) {
            if (byteBuffer.hasRemaining()) {
                if (byteBuffer.get() != Constant.LF) {
                    throw new HttpException(HttpStatus.BAD_REQUEST);
                }
                return decoder.decode(byteBuffer, aioSession, request);
            } else
                return this;
        }
    };

    public HttpProtocolDecoder(HttpServerConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Decoder decode(ByteBuffer byteBuffer, AioSession aioSession, Request request) {
        String protocol = StringUtils.searchFromByteTree(byteBuffer, CR);
        if (protocol != null) {
            request.setProtocol(protocol);
            return lfDecoder.decode(byteBuffer, aioSession, request);
        } else {
            return this;
        }

    }
}
