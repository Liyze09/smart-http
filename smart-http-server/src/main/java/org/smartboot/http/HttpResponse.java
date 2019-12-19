/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpResponse.java
 * Date: 2018-02-03
 * Author: sandao
 */

package org.smartboot.http;

import org.smartboot.http.enums.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
public interface HttpResponse {


    OutputStream getOutputStream();

    HttpStatus getHttpStatus();

    void setHttpStatus(HttpStatus httpStatus);

    /**
     * Sets a response header with the given name and value. If the header had
     * already been set, the new value overwrites the previous one. The
     * <code>containsHeader</code> method can be used to test for the presence
     * of a header before setting its value.
     *
     * @param name  the name of the header
     * @param value the header value If it contains octet string, it should be
     *              encoded according to RFC 2047
     *              (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #addHeader
     */
    public void setHeader(String name, String value);

    /**
     * Adds a response header with the given name and value. This method allows
     * response headers to have multiple values.
     *
     * @param name  the name of the header
     * @param value the additional header value If it contains octet string, it
     *              should be encoded according to RFC 2047
     *              (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #setHeader
     */
    public void addHeader(String name, String value);

    String getHeader(String name);

    /**
     * Return a Collection of all the header values associated with the
     * specified header name.
     *
     * @param name Header name to look up
     * @return The values for the specified header. These are the raw values so
     * if multiple values are specified in a single header that will be
     * returned as a single header value.
     * @since Servlet 3.0
     */
    public Collection<String> getHeaders(String name);

    /**
     * Get the header names set for this HTTP response.
     *
     * @return The header names set for this HTTP response.
     * @since Servlet 3.0
     */
    public Collection<String> getHeaderNames();

    void setContentLength(int contentLength);

    void setContentType(String contentType);

    void write(byte[] data) throws IOException;


    public String getCharacterEncoding();

    public void setCharacterEncoding(String charset);
}
