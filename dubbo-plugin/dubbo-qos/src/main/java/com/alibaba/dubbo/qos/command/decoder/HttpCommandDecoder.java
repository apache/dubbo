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
package com.alibaba.dubbo.qos.command.decoder;

import com.alibaba.dubbo.qos.command.CommandContext;
import com.alibaba.dubbo.qos.command.CommandContextFactory;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HttpCommandDecoder {
    public static CommandContext decode(HttpRequest request) {
        CommandContext commandContext = null;
        if (request != null) {
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
            String path = queryStringDecoder.path();
            String[] array = path.split("/");
            if (array.length == 2) {
                String name = array[1];

                // process GET request and POST request separately. Check url for GET, and check body for POST
                if (request.getMethod() == HttpMethod.GET) {
                    if (queryStringDecoder.parameters().isEmpty()) {
                        commandContext = CommandContextFactory.newInstance(name);
                        commandContext.setHttp(true);
                    } else {
                        List<String> valueList = new ArrayList<String>();
                        for (List<String> values : queryStringDecoder.parameters().values()) {
                            valueList.addAll(values);
                        }
                        commandContext = CommandContextFactory.newInstance(name, valueList.toArray(new String[]{}),true);
                    }
                } else if (request.getMethod() == HttpMethod.POST) {
                    HttpPostRequestDecoder httpPostRequestDecoder = new HttpPostRequestDecoder(request);
                    List<String> valueList = new ArrayList<String>();
                    for (InterfaceHttpData interfaceHttpData : httpPostRequestDecoder.getBodyHttpDatas()) {
                        if (interfaceHttpData.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                            Attribute attribute = (Attribute) interfaceHttpData;
                            try {
                                valueList.add(attribute.getValue());
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                    if (valueList.isEmpty()) {
                        commandContext = CommandContextFactory.newInstance(name);
                        commandContext.setHttp(true);
                    } else {
                        commandContext = CommandContextFactory.newInstance(name, valueList.toArray(new String[]{}),true);
                    }
                }
            }
        }

        return commandContext;
    }
}
