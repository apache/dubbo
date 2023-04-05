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
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import org.apache.dubbo.common.utils.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * netty request facade
 */
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
    public String getMethod() {
        return request.method().name();
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getContextPath() {
        // TODO add ContextPath
        return null;
    }


    @Override
    public String getRequestURI() {
        return request.uri();
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

        Iterator<String> iterator = parameters.keySet().iterator();

        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };

    }

    @Override
    public String[] getParameterValues(String name) {

        if (!parameters.containsKey(name)) {

            return null;
        }
        return parameters.get(name).toArray(new String[0]);
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
    public String getRemoteAddr() {
        return getChannel().remoteAddress().getHostString();
    }

    @Override
    public String getRemoteHost() {
        return getRemoteAddr() + ":" + getRemotePort();
    }

    @Override
    public int getRemotePort() {
        return getChannel().remoteAddress().getPort();
    }

    @Override
    public String getLocalAddr() {
        return getChannel().localAddress().getHostString();
    }

    @Override
    public String getLocalHost() {
        return getRemoteAddr() + ":" + getLocalPort();
    }

    private NioSocketChannel getChannel() {
        return (NioSocketChannel) context.channel();
    }

    @Override
    public int getLocalPort() {
        return getChannel().localAddress().getPort();
    }

    @Override
    public byte[] getInputStream() throws IOException {

        return body;
    }

    protected void parseBody() {
        ByteBuf byteBuf = ((HttpContent) request).content();

        if (byteBuf.readableBytes() > 0) {

            try {
                body = IOUtils.toByteArray(new ByteBufInputStream(byteBuf));
            } catch (IOException e) {

            }
        }

    }
}
