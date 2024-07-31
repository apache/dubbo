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

package org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs

import org.apache.dubbo.remoting.http12.message.MediaType
import org.apache.dubbo.rpc.protocol.tri.rest.service.DemoServiceImpl
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.service.JaxrsDemoServiceImpl
import org.apache.dubbo.rpc.protocol.tri.rest.test.BaseServiceTest
import org.apache.dubbo.rpc.protocol.tri.test.TestRequest
import org.apache.dubbo.rpc.protocol.tri.test.TestRunnerBuilder

class RestProtocolTest extends BaseServiceTest {

    @Override
    void setupService(TestRunnerBuilder builder) {
        builder.provider(new DemoServiceImpl())
        builder.provider(new JaxrsDemoServiceImpl())
    }

    def "hello world"() {
        expect:
            runner.get(path) == output
        where:
            path                    | output
            '/hello?name=world'     | 'hello world'
            '/hello/?name=world'    | 'hello world'
            '/hello.yml?name=world' | 'hello world'
    }

    def "form param test"() {
        expect:
            TestRequest request = new TestRequest(
                path: path,
                contentType: MediaType.APPLICATION_FROM_URLENCODED,
                body: body
            )
            runner.post(request) == output
        where:
            path        | body                                      | output
            '/formTest' | ['user.first': 'sam', 'user.last': 'lee'] | '{"contentType":"application/x-www-form-urlencoded","firstName":"sam","lastName":"lee"}'
    }

    def "bean param test"() {
        expect:
            TestRequest request = new TestRequest(
                path: path,
                contentType: MediaType.APPLICATION_FROM_URLENCODED,
                body: body
            )
            runner.post(request) == output
        where:
            path                   | body                            | output
            '/beanTest/2?name=sam' | ['first': 'sam', 'last': 'lee'] | '{"form":{"contentType":"application/x-www-form-urlencoded","firstName":"sam","lastName":"lee"},"id":2,"name":"sam"}'
    }

    def "param converter test"() {
        expect:
            runner.get(path) == output
        where:
            path                      | output
            '/convertTest?user=3,sam' | '{"id":3,"name":"sam"}'
    }

    def "MultivaluedMap test"() {
        expect:
            TestRequest request = new TestRequest(
                path: path,
                contentType: MediaType.APPLICATION_FROM_URLENCODED,
                body: body
            )
            runner.post(request) == output
        where:
            path                  | body                  | output
            '/multivaluedMapTest' | 'name=1&name=2&age=8' | '{"name":[1,2],"age":[8]}'
    }
}
