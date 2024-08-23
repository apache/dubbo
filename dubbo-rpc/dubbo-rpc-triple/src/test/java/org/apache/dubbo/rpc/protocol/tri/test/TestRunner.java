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
package org.apache.dubbo.rpc.protocol.tri.test;

import java.util.List;

public interface TestRunner {

    TestResponse run(TestRequest request);

    <T> T run(TestRequest request, Class<T> type);

    <T> T get(TestRequest request, Class<T> type);

    String get(TestRequest request);

    <T> T get(String path, Class<T> type);

    <T> List<T> gets(String path, Class<T> type);

    String get(String path);

    List<String> gets(String path);

    <T> T post(TestRequest request, Class<T> type);

    String post(TestRequest request);

    <T> T post(String path, Object body, Class<T> type);

    <T> List<T> posts(String path, Object body, Class<T> type);

    String post(String path, Object body);

    List<String> posts(String path, Object body);

    <T> T put(TestRequest request, Class<T> type);

    String put(TestRequest request);

    <T> T put(String path, Object body, Class<T> type);

    String put(String path, Object body);

    <T> T patch(TestRequest request, Class<T> type);

    String patch(TestRequest request);

    <T> T patch(String path, Object body, Class<T> type);

    String patch(String path, Object body);

    <T> T delete(TestRequest request, Class<T> type);

    String delete(TestRequest request);

    <T> T delete(String path, Class<T> type);

    String delete(String path);

    void destroy();
}
