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

package org.apache.dubbo.rpc.protocol.tri.rest

import org.apache.dubbo.rpc.protocol.tri.rest.service.Book
import org.apache.dubbo.rpc.protocol.tri.rest.service.DemoServiceImpl
import org.apache.dubbo.rpc.protocol.tri.rest.test.BaseServiceTest
import org.apache.dubbo.rpc.protocol.tri.test.TestRequest
import org.apache.dubbo.rpc.protocol.tri.test.TestRunnerBuilder

class RestProtocolTest extends BaseServiceTest {

    @Override
    void setupService(TestRunnerBuilder builder) {
        builder.provider(new DemoServiceImpl())
    }

    def "Hello world"() {
        given:
            def request = new TestRequest(path: path)
        expect:
            runner.run(request, String.class) == output
        where:
            path                | output
            '/hello?name=world' | 'hello world'
    }

    def "post test"() {
        given:
            def request = new TestRequest(path: path).post(body)
        expect:
            runner.run(request, String.class) == output
        where:
            path                 | body                        | output
            '/postTest'          | '["Sam","8"]'               | 'Sam is 8 years old'
            '/postTest'          | '{"name": "Sam", "age": 8}' | 'Sam is 8 years old'
            '/postTest?name=Sam' | '{"age": 8}'                | 'Sam is 8 years old'
            '/postTest?name=Sam' | '{"age": 8}'                | 'Sam is 8 years old'
    }

    def "bean test"() {
        given:
            def request = new TestRequest(path: path).post(body)
        expect:
            runner.run(request, Book.class).name == output
        where:
            path   | body                                                                  | output
            '/buy' | [new Book(name: "Dubbo", price: 80, publishDate: new Date())]         | 'Dubbo'
            '/buy' | ['book': new Book(name: "Dubbo", price: 80, publishDate: new Date())] | 'Dubbo'
    }
}
