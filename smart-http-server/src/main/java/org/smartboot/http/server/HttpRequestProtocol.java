package org.smartboot.http.server;

import org.smartboot.http.enums.HttpMethodEnum;
import org.smartboot.http.enums.HttpStatus;
import org.smartboot.http.enums.State;
import org.smartboot.http.exception.HttpException;
import org.smartboot.http.utils.AttachKey;
import org.smartboot.http.utils.Attachment;
import org.smartboot.http.utils.CharsetUtil;
import org.smartboot.http.utils.Consts;
import org.smartboot.http.utils.DelimiterFrameDecoder;
import org.smartboot.http.utils.FixedLengthFrameDecoder;
import org.smartboot.http.utils.HttpHeaderConstant;
import org.smartboot.http.utils.HttpVersion;
import org.smartboot.http.utils.SmartDecoder;
import org.smartboot.http.utils.StringUtils;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/31
 */
public class HttpRequestProtocol implements Protocol<Http11Request> {

    static final AttachKey<Http11Request> ATTACH_KEY_REQUEST = AttachKey.valueOf("request");
    private static final ThreadLocal<byte[]> BYTE_LOCAL = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[1024];
        }
    };
    private static final byte[] SCAN_URI = new byte[]{'?', Consts.SP};
    private final List<StringCache>[] String_CACHE = new List[512];

    {
        for (int i = 0; i < String_CACHE.length; i++) {
            String_CACHE[i] = new ArrayList<>();
        }
    }

    @Override
    public Http11Request decode(ByteBuffer buffer, AioSession<Http11Request> session) {
        Attachment attachment = session.getAttachment();
        Http11Request request = attachment.get(ATTACH_KEY_REQUEST);
        byte[] cacheBytes = getCacheBytes(buffer, attachment);
        buffer.mark();
        State curState = request._state;
        boolean flag;
        do {
            flag = false;
            switch (curState) {
                case method:
                    int mPos = buffer.position();
                    if (buffer.remaining() < 8) {
                        break;
                    }
                    byte firstByte = buffer.get();
                    switch (firstByte) {
                        case 'G':
                            buffer.position(mPos + 3);
                            if (buffer.get() == Consts.SP) {
                                request.setMethod(HttpMethodEnum.GET);
                            }
                            break;
                        case 'P':
                            buffer.position(mPos + 3);
                            if (buffer.get() == Consts.SP) {
                                request.setMethod(HttpMethodEnum.PUT);
                            } else if (buffer.get() == Consts.SP) {
                                request.setMethod(HttpMethodEnum.POST);
                            }
                            break;
                        case 'H':
                            buffer.position(mPos + 4);
                            if (buffer.get() == Consts.SP) {
                                request.setMethod(HttpMethodEnum.HEAD);
                            }
                            break;
                        case 'D':
                            buffer.position(mPos + 6);
                            if (buffer.get() == Consts.SP) {
                                request.setMethod(HttpMethodEnum.DELETE);
                            }
                            break;
                        case 'C':
                            buffer.position(mPos + 7);
                            if (buffer.get() == Consts.SP) {
                                request.setMethod(HttpMethodEnum.CONNECT);
                            }
                            break;
                        case 'O':
                            buffer.position(mPos + 7);
                            if (buffer.get() == Consts.SP) {
                                request.setMethod(HttpMethodEnum.OPTIONS);
                            }
                            break;
                        case 'T':
                            buffer.position(mPos + 5);
                            if (buffer.get() == Consts.SP) {
                                request.setMethod(HttpMethodEnum.TRACE);
                            }
                            break;
                    }
                    if (request.getMethodEnum() == null) {
                        byte[] b1 = new byte[buffer.remaining()];
                        buffer.get(b1);
                        throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED);
                    }
                case uri:
                    int uriLength = scanUntilAndTrim(buffer, SCAN_URI, cacheBytes);
                    if (uriLength > 0) {
                        request._originalUri = convertToString(cacheBytes, uriLength);
                        if (buffer.get(buffer.position() - 1) == '?') {
                            curState = State.queryString;
                        } else {
                            curState = State.protocol;
                            flag = true;
                            break;
                        }
                    } else {
                        break;
                    }
                case queryString:
                    int queryLength = scanUntil(buffer, Consts.SP, cacheBytes);
                    if (queryLength == 0) {
                        curState = State.protocol;
                    } else if (queryLength > 0) {
                        request.setQueryString(convertToString(cacheBytes, queryLength));
                        curState = State.protocol;
                    } else {
                        break;
                    }
                case protocol:
                    int pos = buffer.position();
                    if (buffer.remaining() < 9) {
                        break;
                    } else if (buffer.get(pos + 8) == Consts.CR) {
                        byte p5 = buffer.get(pos + 5);
                        byte p7 = buffer.get(pos + 7);
                        if (p5 == '0' && p7 == '9') {
                            request.setProtocol(HttpVersion.HTTP_0_9);
                        } else if (p5 == '1') {
                            if (p7 == '0') {
                                request.setProtocol(HttpVersion.HTTP_1_0);
                            } else if (p7 == '1') {
                                request.setProtocol(HttpVersion.HTTP_1_1);
                            }
                        } else if (p5 == '2') {
                            request.setProtocol(HttpVersion.HTTP_2_0);
                        }
                        if (request.getProtocol() == null) {
                            throw new HttpException(HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
                        }
                        curState = State.request_line_end;
                        buffer.position(pos + 9);
                    } else {
                        throw new HttpException(HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
                    }
                case request_line_end:
                    if (buffer.remaining() >= 2) {
                        if (buffer.get() != Consts.LF) {
                            throw new RuntimeException("");
                        }
                        if (buffer.get(buffer.position()) == Consts.CR) {
                            curState = State.head_line_end;
                        } else {
                            curState = State.head_name;
                        }
                    } else {
                        break;
                    }
                case head_name:
                    int nameLength = scanUntilAndTrim(buffer, Consts.COLON, cacheBytes);
                    if (nameLength > 0) {
                        curState = State.head_value;
                        request.tmpHeaderName = convertToString(cacheBytes, nameLength);
                    } else {
                        break;
                    }
                case head_value:
                    if (request.headValueDecoderEnable) {
                        DelimiterFrameDecoder valueDecoder = request.getHeaderValueDecoder();
                        if (valueDecoder.decode(buffer)) {
                            curState = State.head_line_LF;
                            ByteBuffer valBuffer = valueDecoder.getBuffer();
                            trim(valueDecoder.getBuffer());
                            byte[] valBytes = new byte[valBuffer.remaining()];
                            valBuffer.get(valBytes);
                            request.setHeader(request.tmpHeaderName, convertToString(valBytes, valBytes.length));
                            valueDecoder.reset();
                        } else {
                            break;
                        }
                    } else {
                        int valueLength = scanUntilAndTrim(buffer, Consts.CR, cacheBytes);
                        if (valueLength > 0) {
                            curState = State.head_line_LF;
                            request.setHeader(request.tmpHeaderName, convertToString(cacheBytes, valueLength));
                        }
                        //value字段长度超过readBuffer空间大小
                        else if (buffer.remaining() == buffer.capacity()) {
                            request.headValueDecoderEnable = true;
                            request.getHeaderValueDecoder().decode(buffer);
                            break;
                        } else {
                            break;
                        }
                    }
                case head_line_LF:
                    if (buffer.remaining() >= 2) {
                        if (buffer.get() != Consts.LF) {
                            throw new RuntimeException("");
                        }
                        if (buffer.get(buffer.position()) == Consts.CR) {
                            curState = State.head_line_end;
                        } else {
                            curState = State.head_name;
                            flag = true;
                            buffer.mark();
                            break;
                        }
                    } else {
                        break;
                    }
                case head_line_end:
                    if (buffer.remaining() < 2) {
                        break;
                    }
                    if (buffer.get() == Consts.CR && buffer.get() == Consts.LF) {
                        curState = State.head_finished;
                    } else {
                        throw new RuntimeException();
                    }
                case head_finished:
                    //Post请求
                    if (HttpMethodEnum.POST == request.getMethodEnum()
                            && StringUtils.startsWith(request.getContentType(), HttpHeaderConstant.Values.X_WWW_FORM_URLENCODED)) {
                        int postLength = request.getContentLength();
                        if (postLength > Consts.maxPostSize) {
                            throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
                        } else if (postLength <= 0) {
                            throw new HttpException(HttpStatus.LENGTH_REQUIRED);
                        }
                        attachment.put(Consts.ATTACH_KEY_FIX_LENGTH_DECODER, new FixedLengthFrameDecoder(request.getContentLength()));
                        curState = State.body;
                    } else {
                        curState = State.finished;
                        break;
                    }
                case body:
                    SmartDecoder smartDecoder = attachment.get(Consts.ATTACH_KEY_FIX_LENGTH_DECODER);
                    if (smartDecoder.decode(buffer)) {
                        request.setPostData(smartDecoder.getBuffer().array());
                        attachment.remove(Consts.ATTACH_KEY_FIX_LENGTH_DECODER);
                        curState = State.finished;
                    }
                    buffer.mark();
                    break;
                case finished:
                    break;
                default:
                    throw new RuntimeException("aa");
            }
        } while (flag);
        if (curState == State.finished) {
            return request;
        }
        request._state = curState;
        if (buffer.remaining() == buffer.capacity()) {
            throw new RuntimeException("buffer is too small when decode " + curState + " ," + request.tmpHeaderName);
        }
        return null;
    }

    private byte[] getCacheBytes(ByteBuffer buffer, Attachment attachment) {
        Thread attachThread = attachment.get(Consts.ATTACH_KEY_CURRENT_THREAD);
        Thread currentThread = Thread.currentThread();
        if (attachThread != currentThread) {
            attachment.put(Consts.ATTACH_KEY_CURRENT_THREAD, currentThread);
            attachment.put(Consts.ATTACH_KEY_CACHE_BYTES, BYTE_LOCAL.get());
        }
        byte[] cacheBytes = attachment.get(Consts.ATTACH_KEY_CACHE_BYTES);
        if (cacheBytes.length < buffer.remaining()) {
            cacheBytes = new byte[buffer.remaining()];
            BYTE_LOCAL.set(cacheBytes);
            attachment.put(Consts.ATTACH_KEY_CACHE_BYTES, cacheBytes);
        }
        return cacheBytes;
    }

    private String convertToString(byte[] bytes, int length) {
        if (length >= String_CACHE.length) {
            return new String(bytes, 0, length, CharsetUtil.US_ASCII);
        }
        List<StringCache> list = String_CACHE[length];
        for (int i = list.size() - 1; i > -1; i--) {
            StringCache cache = list.get(i);
            if (equals(cache.bytes, bytes)) {
                return cache.value;
            }
        }
        synchronized (list) {
            for (StringCache cache : list) {
                if (equals(cache.bytes, bytes)) {
                    return cache.value;
                }
            }
            String str = new String(bytes, 0, length, CharsetUtil.US_ASCII);
            byte[] bak = new byte[length];
            System.arraycopy(bytes, 0, bak, 0, bak.length);
            list.add(new StringCache(bak, str));
            return str;
        }
    }

    private boolean equals(byte[] b0, byte[] b1) {
        for (int i = b0.length - 1; i > 0; i--) {
            if (b0[i] != b1[i]) {
                return false;
            }
        }
        return b0[0] == b1[0];
    }


    private int scanUntil(ByteBuffer buffer, byte split, byte[] bytes) {
        int avail = buffer.remaining();
        for (int i = 0; i < avail; ) {
            bytes[i] = buffer.get();
            if (bytes[i] == split) {
                buffer.mark();
                return i;
            }
            i++;
        }
        buffer.reset();
        return -1;
    }

    private int scanUntilAndTrim(ByteBuffer buffer, byte split, byte[] bytes) {
        int avail = buffer.remaining();
        for (int i = 0; i < avail; ) {
            bytes[i] = buffer.get();
            if (i == 0 && bytes[i] == Consts.SP) {
                avail--;
                continue;
            }
            if (bytes[i] == split) {
                buffer.mark();
                //反向去空格
                while (bytes[i - 1] == Consts.SP) {
                    i--;
                }
                return i;
            }
            i++;
        }
        buffer.reset();
        return 0;
    }

    private int scanUntilAndTrim(ByteBuffer buffer, byte[] splits, byte[] bytes) {
        int avail = buffer.remaining();
        for (int i = 0; i < avail; ) {
            bytes[i] = buffer.get();
            if (i == 0 && bytes[i] == Consts.SP) {
                avail--;
                continue;
            }
            byte b = bytes[i];
            for (byte split : splits) {
                if (b == split) {
                    buffer.mark();
                    //反向去空格
                    while (bytes[i - 1] == Consts.SP) {
                        i--;
                    }
                    return i;
                }
            }
            i++;
        }
        buffer.reset();
        return 0;
    }

    private void trim(ByteBuffer buffer) {
        int pos = buffer.position();
        int limit = buffer.limit();

        while (pos < limit) {
            byte b = buffer.get(pos);
            if (b != Consts.SP && b != Consts.CR && b != Consts.LF) {
                break;
            }
            pos++;
        }
        buffer.position(pos);

        while (pos < limit) {
            byte b = buffer.get(limit - 1);
            if (b != Consts.SP && b != Consts.CR && b != Consts.LF) {
                break;
            }
            limit--;
        }
        buffer.limit(limit);
    }


    private class StringCache {
        final byte[] bytes;
        final String value;

        public StringCache(byte[] bytes, String value) {
            this.bytes = bytes;
            this.value = value;
        }
    }

}
