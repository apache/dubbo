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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring.service;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.protocol.tri.rest.service.Book;
import org.apache.dubbo.rpc.protocol.tri.rest.service.User.Group;
import org.apache.dubbo.rpc.protocol.tri.rest.service.User.UserEx;

import java.util.List;
import java.util.Map;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/spring")
public interface SpringDemoService {

    @RequestMapping
    String hello(String name);

    @RequestMapping
    String argTest(String name, int age);

    @RequestMapping
    Book beanArgTest(@RequestBody(required = false) Book book, Integer quote);

    @RequestMapping
    Book beanArgTest2(@RequestBody Book book);

    @RequestMapping("/bean")
    UserEx advanceBeanArgTest(UserEx user);

    @RequestMapping
    List<Integer> listArgBodyTest(@RequestBody List<Integer> list, int age);

    @RequestMapping
    List<Integer> listArgBodyTest2(List<Integer> list, int age);

    @RequestMapping
    Map<Integer, List<Long>> mapArgBodyTest(@RequestBody Map<Integer, List<Long>> map, int age);

    @RequestMapping
    Map<Integer, List<Long>> mapArgBodyTest2(Map<Integer, List<Long>> map, int age);

    @RequestMapping
    List<Group> beanBodyTest(@RequestBody List<Group> groups, int age);

    @RequestMapping
    List<Group> beanBodyTest2(List<Group> groups, int age);

    @RequestMapping
    MultiValueMap<String, Integer> multiValueMapTest(MultiValueMap<String, Integer> params);

    @RequestMapping
    Book buy(Book book);

    @RequestMapping("/buy2")
    Book buy(Book book, int count);

    @RequestMapping
    String say(String name, Long count);

    @RequestMapping
    String say(String name);

    @RequestMapping
    String argNameTest(String name);

    @RequestMapping
    void pbServerStream(@RequestBody HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver);
}
