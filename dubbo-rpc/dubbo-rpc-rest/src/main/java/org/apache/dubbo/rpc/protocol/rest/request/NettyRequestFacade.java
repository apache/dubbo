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
package org.apache.dubbo.rpc.protocol.rest.request;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;


// TODO add some methods return
public class NettyRequestFacade extends RequestFacade<FullHttpRequest> {


    private ChannelHandlerContext context;

    public NettyRequestFacade(Object request, ChannelHandlerContext context) {
        super((FullHttpRequest) request);
        this.context = context;
    }


    protected void initHeaders() {
        for (Map.Entry<String, String> header : request.headers()) {

            String key = header.getKey();

            ArrayList<String> tmpHeaders = headers.get(key);

            if (tmpHeaders == null) {
                tmpHeaders = new ArrayList<>();
                headers.put(key, tmpHeaders);
            }

            tmpHeaders.add(header.getValue());
        }
    }

    @Override
    public String getHeader(String name) {

        List<String> values = headers.get(name);

        if (values == null || values.isEmpty()) {
            return null;
        } else {
            return values.get(0);
        }

    }

    @Override
    public Enumeration<String> getHeaders(String name) {

        List<String> list = headers.get(name);

        if (list == null) {
            list = new ArrayList<>();
        }


        ListIterator<String> stringListIterator = list.listIterator();

        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return stringListIterator.hasNext();
            }

            @Override
            public String nextElement() {
                return stringListIterator.next();
            }
        };
    }


    @Override
    public Enumeration<String> getHeaderNames() {

        Iterator<String> strings = headers.keySet().iterator();

        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return strings.hasNext();
            }

            @Override
            public String nextElement() {
                return strings.next();
            }
        };
    }

    @Override
    public int getIntHeader(String name) {

        String header = getHeader(name);
        if (header == null) {
            return -1;
        }
        return Integer.parseInt(header);
    }

    @Override
    public String getMethod() {
        return request.method().name();
    }

    @Override
    public String getPathInfo() {
        return path;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return request.uri();
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public String getParameter(String name) {
        ArrayList<String> strings = parameters.get(name);

        String value = null;
        if (strings != null && !strings.isEmpty()) {
            value = strings.get(0);

        }
        return value;
    }

    @Override
    public Enumeration<String> getParameterNames() {

        Set<String> strings = parameters.keySet();

        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return strings.iterator().hasNext();
            }

            @Override
            public String nextElement() {
                return strings.iterator().next();
            }
        };

    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.keySet().toArray(new String[0]);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        HashMap<String, String[]> map = new HashMap<>();
        parameters.entrySet().forEach(entry -> {
            map.put(entry.getKey(), entry.getValue().toArray(new String[0]));
        });
        return map;

    }

    @Override
    public int getServerPort() {
        return 0;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        ByteBuf byteBuf = ((HttpContent) request).content();

        if (byteBuf.readableBytes() > 0) {

            return new ByteBufInputStream(byteBuf.retain());
        }
        return new ByteArrayInputStream(new byte[0]);
    }
}
