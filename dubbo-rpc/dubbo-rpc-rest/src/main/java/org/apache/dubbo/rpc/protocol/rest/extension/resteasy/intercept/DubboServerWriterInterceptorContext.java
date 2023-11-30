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
package org.apache.dubbo.rpc.protocol.rest.extension.resteasy.intercept;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.WriterInterceptor;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.jboss.resteasy.core.interception.ServerWriterInterceptorContext;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class DubboServerWriterInterceptorContext extends ServerWriterInterceptorContext {
    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(DubboServerWriterInterceptorContext.class);

    public DubboServerWriterInterceptorContext(
            WriterInterceptor[] interceptors,
            ResteasyProviderFactory providerFactory,
            Object entity,
            Class type,
            Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, Object> headers,
            OutputStream outputStream,
            HttpRequest request) {
        super(
                interceptors,
                providerFactory,
                entity,
                type,
                genericType,
                annotations,
                mediaType,
                headers,
                outputStream,
                request);
    }

    @Override
    public void proceed() throws IOException, WebApplicationException {
        logger.debug("Dubbo server writer intercept  context: " + getClass().getName() + "  Method : proceed");

        if (interceptors == null || index >= interceptors.length) {
            return;
        } else {

            logger.debug("Dubbo server writer intercept  context WriterInterceptor: "
                    + interceptors[index].getClass().getName());
            interceptors[index++].aroundWriteTo(this);
        }
    }
}
