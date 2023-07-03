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


import java.util.Map;

/**
 * An interface for REST service
 *
 * @since 2.7.6
 */
public interface RestService {

    String param(String param);

    String params(int a, String b);

    String headers(String header, String header2, Integer param);

    String pathVariables(String path1, String path2, String param);

    String form(String form);

    User requestBodyMap(Map<String, Object> data, String param);

    Map<String, Object> requestBodyUser(User user);

    void noAnnotationJsonBody(User user);
    void noAnnotationFormBody(User user);
    void noAnnotationParam(String  text);
}
