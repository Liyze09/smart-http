/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpBootstrap.java
 * Date: 2018-01-28
 * Author: sandao
 */

package org.smartboot.http;

import org.smartboot.http.server.HttpMessageProcessor;
import org.smartboot.http.server.decode.Http11Request;
import org.smartboot.http.server.decode.HttpRequestProtocol;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;

public class HttpBootstrap {


    private AioQuickServer<Http11Request> server;
    /**
     * Http服务端口号
     */
    private int port = 8080;
    /**
     * read缓冲区大小
     */
    private int readBufferSize = 1024;
    /**
     * 服务线程数
     */
    private int threadNum = Runtime.getRuntime().availableProcessors() + 2;
    private HttpMessageProcessor processor = new HttpMessageProcessor();
    /**
     * http消息解码器
     */
    private Protocol<Http11Request> protocol = new HttpRequestProtocol();

//    static void https(MessageProcessor<Http11Request> processor) {
//        // 定义服务器接受的消息类型以及各类消息对应的处理器
//        AioSSLQuickServer<Http11Request> server = new AioSSLQuickServer<Http11Request>(8889, new HttpRequestProtocol(), processor);
//        server
//                .setClientAuth(ClientAuth.OPTIONAL)
//                .setKeyStore(ClassLoader.getSystemClassLoader().getResource("server.jks").getFile(), "storepass")
//                .setTrust(ClassLoader.getSystemClassLoader().getResource("trustedCerts.jks").getFile(), "storepass")
//                .setKeyPassword("keypass");
//        try {
//            server.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 设置HTTP服务端端口号
     *
     * @param port
     * @return
     */
    public HttpBootstrap setPort(int port) {
        this.port = port;
        return this;
    }

    public Pipeline pipeline() {
        return processor.pipeline();
    }

    /**
     * 设置read缓冲区大小
     *
     * @param readBufferSize
     * @return
     */
    public HttpBootstrap setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

    /**
     * 设置服务线程数
     *
     * @param threadNum
     * @return
     */
    public HttpBootstrap setThreadNum(int threadNum) {
        this.threadNum = threadNum;
        return this;
    }


    /**
     * 启动HTTP服务
     */
    public void start() {
        server = new AioQuickServer<Http11Request>(port, protocol, processor);
        server.setReadBufferSize(readBufferSize);
        server.setThreadNum(threadNum);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止服务
     */
    public void shutdown() {
        server.shutdown();
    }
}
