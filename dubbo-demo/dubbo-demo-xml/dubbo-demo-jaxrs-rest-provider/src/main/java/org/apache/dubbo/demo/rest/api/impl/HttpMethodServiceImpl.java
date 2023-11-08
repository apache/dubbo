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
package org.apache.dubbo.demo.rest.api.impl;

import org.apache.dubbo.demo.rest.api.HttpMethodService;

import org.springframework.stereotype.Service;

@Service("httpMethodService")
public class HttpMethodServiceImpl implements HttpMethodService {

    @Override
    public String sayHelloPost(String hello) {
        return hello;
    }

    @Override
    public String sayHelloDelete(String hello) {
        return hello;
    }

    @Override
    public String sayHelloHead(String hello) {
        return hello;
    }

    @Override
    public String sayHelloGet(String hello) {
        return hello;
    }

    @Override
    public String sayHelloPut(String hello) {
        return hello;
    }

    @Override
    public String sayHelloPatch(String hello) {
        return hello;
    }

    @Override
    public String sayHelloOptions(String hello) {
        return hello;
    }
}
