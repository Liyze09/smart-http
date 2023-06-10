/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpServerHandle.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

public interface HttpServerHandler extends ServerHandler<HttpRequest, HttpResponse> {
    void handle(HttpRequest request, HttpResponse response);
    
    default boolean onBodyStream(ByteBuffer buffer, Request request) {
        if (HttpMethodEnum.GET.getMethod().equals(request.getMethod())) {
            return true;
        }
        //Post请求
        if (HttpMethodEnum.POST.getMethod().equals(request.getMethod())
                && StringUtils.startsWith(request.getContentType(), HeaderValueEnum.X_WWW_FORM_URLENCODED.getName())) {
            int postLength = request.getContentLength();
            if (postLength > request.getConfiguration().getMaxFormContentSize()) {
                throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
            } else if (postLength < 0) {
                throw new HttpException(HttpStatus.LENGTH_REQUIRED);
            } else if (postLength == 0) {
                return true;
            }

            RequestAttachment attachment = request.getAioSession().getAttachment();
            SmartDecoder smartDecoder = attachment.getBodyDecoder();
            if (smartDecoder == null) {
                smartDecoder = new FixedLengthFrameDecoder(request.getContentLength());
                attachment.setBodyDecoder(smartDecoder);
            }

            if (smartDecoder.decode(buffer)) {
                request.setFormUrlencoded(new String(smartDecoder.getBuffer().array()));
                attachment.setBodyDecoder(null);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
}
