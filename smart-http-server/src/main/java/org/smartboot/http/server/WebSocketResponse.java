/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: Http11Response.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server;

import java.io.OutputStream;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
public class WebSocketResponse extends AbstractResponse {


    public WebSocketResponse(WebSocketRequest request, OutputStream outputStream) {
        init(request, new WebSocketOutputStream(request, this, outputStream));
    }

}
