/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpOutputStream.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server;

import org.smartboot.http.HttpRequest;
import org.smartboot.http.enums.HeaderNameEnum;
import org.smartboot.http.enums.HttpMethodEnum;
import org.smartboot.http.utils.Constant;
import org.smartboot.http.utils.HttpHeaderConstant;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
abstract class AbstractOutputStream extends OutputStream implements Reset {

    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "flushDate");
            thread.setDaemon(true);
            return thread;
        }
    });
    protected static byte[] date;
    private static String SERVER_ALIAS_NAME = "smart-http";
    private static SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
    private static Date currentDate = new Date();

    static {
        String aliasServer = System.getProperty("smartHttp.server.alias");
        if (aliasServer != null) {
            SERVER_ALIAS_NAME = aliasServer + "smart-http";
        }
        flushDate();
        SCHEDULED_EXECUTOR_SERVICE.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                AbstractOutputStream.flushDate();
            }
        }, 900, 900, TimeUnit.MILLISECONDS);
    }

    protected final AbstractResponse response;
    protected final OutputStream outputStream;
    private final HttpRequest request;
    protected boolean committed = false, closed = false;
    protected boolean chunked = false;

    public AbstractOutputStream(HttpRequest request, AbstractResponse response, OutputStream outputStream) {
        this.response = response;
        this.request = request;
        this.outputStream = outputStream;
    }

    private static void flushDate() {
        currentDate.setTime(System.currentTimeMillis());
        AbstractOutputStream.date = ("\r\n" + HttpHeaderConstant.Names.DATE + ":" + sdf.format(currentDate) + "\r\n"
                + HttpHeaderConstant.Names.SERVER + ":" + SERVER_ALIAS_NAME + "\r\n\r\n").getBytes();
    }


    @Override
    public final void write(int b) {
        throw new UnsupportedOperationException();
    }

    /**
     * 输出Http响应
     *
     * @param b
     * @param off
     * @param len
     * @throws IOException
     */
    public final void write(byte b[], int off, int len) throws IOException {
        writeHead();
        if (HttpMethodEnum.HEAD.getMethod().equals(request.getMethod())) {
            throw new UnsupportedOperationException(request.getMethod() + " can not write http body");
        }
        if (chunked) {
            byte[] start = getBytes(Integer.toHexString(len) + "\r\n");
            outputStream.write(start);
            outputStream.write(b, off, len);
            outputStream.write(Constant.CRLF);
        } else {
            outputStream.write(b, off, len);
        }

    }

    /**
     * 输出Http消息头
     *
     * @throws IOException
     */
    abstract void writeHead() throws IOException;


    @Override
    public final void flush() throws IOException {
        writeHead();
        outputStream.flush();
    }

    @Override
    public abstract void close() throws IOException;

    final protected byte[] getHeaderNameBytes(String name) {
        HeaderNameEnum headerNameEnum = HttpHeaderConstant.HEADER_NAME_ENUM_MAP.get(name);
        if (headerNameEnum != null) {
            return headerNameEnum.getBytesWithColon();
        }
        byte[] extBytes = HttpHeaderConstant.HEADER_NAME_EXT_MAP.get(name);
        if (extBytes == null) {
            synchronized (name) {
                extBytes = getBytes("\r\n" + name + ":");
                HttpHeaderConstant.HEADER_NAME_EXT_MAP.put(name, extBytes);
            }
        }
        return extBytes;
    }

    protected final byte[] getBytes(String str) {
        return str.getBytes(StandardCharsets.US_ASCII);
    }

    public final void reset() {
        committed = closed = chunked = false;
    }

    public final boolean isClosed() {
        return closed;
    }
}
