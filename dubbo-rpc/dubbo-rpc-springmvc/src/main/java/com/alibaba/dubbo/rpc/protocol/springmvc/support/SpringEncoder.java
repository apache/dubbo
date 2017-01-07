package com.alibaba.dubbo.rpc.protocol.springmvc.support;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;


/**
 * Created by wuyu on 2016/6/8.
 */

public class SpringEncoder implements Encoder {

    private List<HttpMessageConverter<?>> messageConverters;

    private Logger log = LoggerFactory.getLogger(SpringEncoder.class);


    public SpringEncoder(List<HttpMessageConverter<?>> messageConverters) {
        this.messageConverters = messageConverters;
    }

    @Override
    public void encode(Object requestBody, Type bodyType, RequestTemplate request)
            throws EncodeException {
        // template.body(conversionService.convert(object, String.class));
        if (requestBody != null) {
            Class<?> requestType = requestBody.getClass();
            Collection<String> contentTypes = request.headers().get("Content-Type");

            MediaType requestContentType = null;
            if (contentTypes != null && !contentTypes.isEmpty()) {
                String type = contentTypes.iterator().next();
                requestContentType = MediaType.valueOf(type);
            }

            for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
                if (messageConverter.canWrite(requestType, requestContentType)) {
                    if (log.isDebugEnabled()) {
                        if (requestContentType != null) {
                            log.debug("Writing [" + requestBody + "] as \""
                                    + requestContentType + "\" using ["
                                    + messageConverter + "]");
                        } else {
                            log.debug("Writing [" + requestBody + "] using ["
                                    + messageConverter + "]");
                        }

                    }

                    FeignOutputMessage outputMessage = new FeignOutputMessage(request);
                    try {
                        @SuppressWarnings("unchecked")
                        HttpMessageConverter<Object> copy = (HttpMessageConverter<Object>) messageConverter;
                        copy.write(requestBody, requestContentType, outputMessage);
                    } catch (IOException ex) {
                        throw new EncodeException("Error converting request body", ex);
                    }
                    // clear headers
                    request.headers(null);
                    // converters can modify headers, so update the request
                    // with the modified headers
                    request.headers(FeignUtils.getHeaders(outputMessage.getHeaders()));

                    request.body(outputMessage.getOutputStream().toByteArray(),
                            Charset.forName("UTF-8")); // TODO: set charset
                    return;
                }
            }
            String message = "Could not write request: no suitable HttpMessageConverter "
                    + "found for request type [" + requestType.getName() + "]";
            if (requestContentType != null) {
                message += " and content type [" + requestContentType + "]";
            }
            throw new EncodeException(message);
        }
    }

    private class FeignOutputMessage implements HttpOutputMessage {

        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        private final HttpHeaders httpHeaders;

        private FeignOutputMessage(RequestTemplate request) {
            httpHeaders = FeignUtils.getHttpHeaders(request.headers());
        }

        @Override
        public OutputStream getBody() throws IOException {
            return this.outputStream;
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.httpHeaders;
        }

        public ByteArrayOutputStream getOutputStream() {
            return this.outputStream;
        }

    }

}