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
package org.apache.dubbo.rpc.protocol.rest;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.dubbo.rpc.protocol.rest.request.NettyRequestFacade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class NettyRequestFacadeTest {

    @Test
    void testMethod() {

        String uri = "/a/b?c=c&d=d";
        DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);

        defaultFullHttpRequest.headers().add("h1", "a");
        defaultFullHttpRequest.headers().add("h1", "b");
        defaultFullHttpRequest.headers().add("h2", "c");
        NettyRequestFacade nettyRequestFacade = new NettyRequestFacade(defaultFullHttpRequest, null);


        Assertions.assertArrayEquals(new String[]{"c"}, nettyRequestFacade.getParameterValues("c"));
        Enumeration<String> parameterNames = nettyRequestFacade.getParameterNames();

        List<String> names = new ArrayList<>();
        while (parameterNames.hasMoreElements()) {

            names.add(parameterNames.nextElement());
        }

        Assertions.assertArrayEquals(Arrays.asList("c", "d").toArray(), names.toArray());

        Enumeration<String> headerNames = nettyRequestFacade.getHeaderNames();

        List<String> heads = new ArrayList<>();
        while (headerNames.hasMoreElements()) {

            heads.add(headerNames.nextElement());
        }

        Assertions.assertArrayEquals(Arrays.asList("h1", "h2").toArray(), heads.toArray());


        Assertions.assertEquals(uri, nettyRequestFacade.getRequestURI());


        Assertions.assertEquals("c", nettyRequestFacade.getHeader("h2"));

        Assertions.assertEquals("d", nettyRequestFacade.getParameter("d"));

        Assertions.assertEquals("/a/b", nettyRequestFacade.getPath());

        Assertions.assertEquals(null, nettyRequestFacade.getParameterValues("e"));

        Assertions.assertArrayEquals(new String[]{"d"}, nettyRequestFacade.getParameterValues("d"));

        Enumeration<String> h1s = nettyRequestFacade.getHeaders("h1");


        heads = new ArrayList<>();

        while (h1s.hasMoreElements()) {

            heads.add(h1s.nextElement());
        }

        Assertions.assertArrayEquals(new String[]{"a","b"}, heads.toArray());

        Map<String, String[]> parameterMap = nettyRequestFacade.getParameterMap();


        Assertions.assertArrayEquals(new String[]{"c"},parameterMap.get("c"));
        Assertions.assertArrayEquals(new String[]{"d"},parameterMap.get("d"));

        Assertions.assertEquals("GET",nettyRequestFacade.getMethod());

    }
}
