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
package org.apache.dubbo.metadata.rest;

import org.apache.dubbo.config.annotation.DubboService;

import java.util.Map;

/**
 * The default implementation of {@link RestService}
 *
 * @since 2.7.6
 */
@DubboService(version = "1.0.0", group = "default")
public class DefaultRestService implements RestService {

    @Override
    public String param(String param) {
        return null;
    }

    @Override
    public String params(int a, String b) {
        return null;
    }

    @Override
    public String headers(String header, String header2, Integer param) {
        return null;
    }

    @Override
    public String pathVariables(String path1, String path2, String param) {
        return null;
    }

    @Override
    public String form(String form) {
        return null;
    }

    @Override
    public User requestBodyMap(Map<String, Object> data, String param) {
        return null;
    }

    @Override
    public Map<String, Object> requestBodyUser(User user) {
        return null;
    }

    public User user(User user) {
        return user;
    }
}
