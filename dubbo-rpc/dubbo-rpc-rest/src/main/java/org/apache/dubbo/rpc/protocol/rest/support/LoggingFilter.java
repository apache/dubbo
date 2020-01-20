/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.rest.support;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import org.apache.commons.io.IOUtils;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * This logging filter is not highly optimized for now
 *
 */
@Priority(Integer.MIN_VALUE)
public class LoggingFilter implements ContainerRequestFilter, ClientRequestFilter, ContainerResponseFilter, ClientResponseFilter, WriterInterceptor, ReaderInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void filter(ClientRequestContext context) throws IOException {
        logHttpHeaders(context.getStringHeaders());
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        logHttpHeaders(responseContext.getHeaders());
    }

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        logHttpHeaders(context.getHeaders());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        logHttpHeaders(responseContext.getStringHeaders());
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        byte[] buffer = IOUtils.toByteArray(context.getInputStream());
        logger.info("The contents of request body is: \n" + new String(buffer, StandardCharsets.UTF_8) + "\n");
        context.setInputStream(new ByteArrayInputStream(buffer));
        return context.proceed();
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        OutputStreamWrapper wrapper = new OutputStreamWrapper(context.getOutputStream());
        context.setOutputStream(wrapper);
        context.proceed();
        logger.info("The contents of response body is: \n" + new String(wrapper.getBytes(), StandardCharsets.UTF_8) + "\n");
    }

    protected void logHttpHeaders(MultivaluedMap<String, String> headers) {
        StringBuilder msg = new StringBuilder("The HTTP headers are: \n");
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            msg.append(entry.getKey()).append(": ");
            for (int i = 0; i < entry.getValue().size(); i++) {
                msg.append(entry.getValue().get(i));
                if (i < entry.getValue().size() - 1) {
                    msg.append(", ");
                }
            }
            msg.append("\n");
        }
        logger.info(msg.toString());
    }

    protected static class OutputStreamWrapper extends OutputStream {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final OutputStream output;

        private OutputStreamWrapper(OutputStream output) {
            this.output = output;
        }

        @Override
        public void write(int i) throws IOException {
            buffer.write(i);
            output.write(i);
        }

        @Override
        public void write(byte[] b) throws IOException {
            buffer.write(b);
            output.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            buffer.write(b, off, len);
            output.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            output.flush();
        }

        @Override
        public void close() throws IOException {
            output.close();
        }

        public byte[] getBytes() {
            return buffer.toByteArray();
        }
    }
}
