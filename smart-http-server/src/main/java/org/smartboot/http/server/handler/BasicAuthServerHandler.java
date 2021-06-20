/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: BasicAuthServerHandle.java
 * Date: 2021-02-23
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server.handler;

import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.common.utils.StringUtils;
import org.smartboot.http.server.HttpRequest;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.http.server.HttpServerHandler;

import java.io.IOException;
import java.util.Base64;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/23
 */
public final class BasicAuthServerHandler extends HttpServerHandler {
    private final HttpServerHandler httpServerHandler;
    private final String basic;

    public BasicAuthServerHandler(String username, String password, HttpServerHandler httpServerHandler) {
        this.httpServerHandler = httpServerHandler;
        basic = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws IOException {
        String clientBasic = request.getHeader(HeaderNameEnum.AUTHORIZATION.getName());
        if (StringUtils.equals(clientBasic, this.basic)) {
            httpServerHandler.handle(request, response);
        } else {
            response.setHeader(HeaderNameEnum.WWW_AUTHENTICATE.getName(), "Basic realm=\"smart-http\"");
            response.setHttpStatus(HttpStatus.UNAUTHORIZED);
        }
    }

}