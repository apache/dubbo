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
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.rest.Mapping;
import org.apache.dubbo.rpc.protocol.tri.rest.service.User.Group;
import org.apache.dubbo.rpc.protocol.tri.rest.service.User.UserEx;

import java.util.List;
import java.util.Map;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;

public class DemoServiceImpl implements DemoService {

    @Override
    public String hello(String name) {
        return "hello " + name;
    }

    @Override
    public String argTest(String name, int age) {
        return name + " is " + age + " years old";
    }

    @Override
    public Book beanArgTest(Book book, Integer quote) {
        if (book == null) {
            book = new Book();
        }
        if (quote != null) {
            book.setPrice(quote);
        }
        return book;
    }

    @Override
    public Book beanArgTest2(Book book) {
        return beanArgTest(book, null);
    }

    @Override
    public UserEx advanceBeanArgTest(UserEx user) {
        return user;
    }

    @Override
    public List<Integer> listArgBodyTest(List<Integer> list, int age) {
        Assert.assertTrue(age == 2, "age must be 2");
        return list;
    }

    @Override
    public List<Integer> listArgBodyTest2(List<Integer> list, int age) {
        return listArgBodyTest(list, age);
    }

    @Override
    public Map<Integer, List<Long>> mapArgBodyTest(Map<Integer, List<Long>> map, int age) {
        Assert.assertTrue(age == 2, "age must be 2");
        return map;
    }

    @Override
    public Map<Integer, List<Long>> mapArgBodyTest2(Map<Integer, List<Long>> map, int age) {
        return mapArgBodyTest(map, age);
    }

    @Override
    public List<Group> beanBodyTest(List<Group> groups, int age) {
        Assert.assertTrue(age == 2, "age must be 2");
        return groups;
    }

    @Override
    public List<Group> beanBodyTest2(List<Group> groups, int age) {
        return beanBodyTest(groups, age);
    }

    @Override
    public Book buy(Book book) {
        return book;
    }

    @Override
    public Book buy(Book book, int count) {
        return book;
    }

    @Override
    public String say(String name, Long count) {
        return "2";
    }

    @Override
    public String say(String name) {
        return "1";
    }

    @Mapping
    public String noInterface() {
        return "ok";
    }

    public String noInterfaceAndMapping() {
        return "ok";
    }

    @Override
    public String argNameTest(String name1) {
        return name1;
    }

    @Override
    public void pbServerStream(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        String service = request.getService();
        if (StringUtils.isNotEmpty(service)) {
            int count = Integer.parseInt(service);
            for (int i = 0; i < count; i++) {
                responseObserver.onNext(HealthCheckResponse.newBuilder()
                        .setStatus(ServingStatus.SERVING)
                        .build());
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public String produceTest(String name) {
        return name;
    }

    @Override
    public String mismatchTest(String name) {
        return name;
    }
}
