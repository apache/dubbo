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
package org.apache.dubbo.rpc.protocol.rest.intercept;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Priority(Priorities.USER)
public class DynamicTraceInterceptor implements ReaderInterceptor, WriterInterceptor {

    public DynamicTraceInterceptor() {}

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext readerInterceptorContext)
            throws IOException, WebApplicationException {
        System.out.println("Dynamic reader interceptor invoked");
        return readerInterceptorContext.proceed();
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext writerInterceptorContext)
            throws IOException, WebApplicationException {
        System.out.println("Dynamic writer interceptor invoked");
        writerInterceptorContext.getOutputStream().write("intercept".getBytes(StandardCharsets.UTF_8));
        writerInterceptorContext.proceed();
    }
}
