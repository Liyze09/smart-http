/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: WebSocketResponse.java
 * Date: 2020-03-31
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.client;

/**
 * WebSocket消息响应接口
 *
 * @author 三刀
 * @version V1.0 , 2020/3/31
 */
public interface WebSocketRequest {
    /**
     * 发送文本响应
     *
     * @param text
     */
    void sendTextMessage(String text);

    /**
     * 发送二进制响应
     *
     * @param bytes
     */
    void sendBinaryMessage(byte[] bytes);

    /**
     * 关闭ws通道
     */
    void close();

    /**
     * 输出数据
     */
    void flush();
}
