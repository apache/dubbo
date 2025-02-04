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
package org.apache.dubbo.rpc.protocol.tri.rest.service;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.rest.Mapping;
import org.apache.dubbo.remoting.http12.rest.Param;
import org.apache.dubbo.rpc.protocol.tri.rest.service.User.Group;
import org.apache.dubbo.rpc.protocol.tri.rest.service.User.UserEx;

import java.util.List;
import java.util.Map;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;

import static org.apache.dubbo.remoting.http12.rest.ParamType.Body;

@Mapping("/")
public interface DemoService {

    String hello(String name);

    String argTest(String name, int age);

    Book beanArgTest(Book book, Integer quote);

    Book beanArgTest2(Book book);

    @Mapping("/bean")
    UserEx advanceBeanArgTest(UserEx user);

    List<Integer> listArgBodyTest(@Param(type = Body) List<Integer> list, int age);

    List<Integer> listArgBodyTest2(List<Integer> list, int age);

    Map<Integer, List<Long>> mapArgBodyTest(@Param(type = Body) Map<Integer, List<Long>> map, int age);

    Map<Integer, List<Long>> mapArgBodyTest2(Map<Integer, List<Long>> map, int age);

    List<Group> beanBodyTest(@Param(type = Body) List<Group> groups, int age);

    List<Group> beanBodyTest2(List<Group> groups, int age);

    Book buy(Book book);

    @Mapping("/buy2")
    Book buy(Book book, int count);

    String say(String name, Long count);

    String say(String name);

    String argNameTest(String name);

    void pbServerStream(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver);

    @Mapping(produces = "text/plain")
    String produceTest(String name);

    @Mapping(method = HttpMethods.POST, consumes = "text/plain", produces = "text/plain", params = "name=world")
    String mismatchTest(String name);
}
