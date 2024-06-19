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

public class DemoServiceImpl implements DemoService {

    @Override
    public String hello(String name) {
        return "hello " + name;
    }

    @Override
    public String postTest(String name, int age) {
        return name + " is " + age + " years old";
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
}