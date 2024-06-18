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

import org.apache.dubbo.remoting.http12.HttpMethods
import org.apache.dubbo.remoting.http12.message.MediaType
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

    def "hello world"() {
        given:
            def request = new TestRequest(path)
        expect:
            runner.run(request, String.class) == output
        where:
            path                    | output
            '/hello?name=world'     | 'hello world'
            '/hello/?name=world'    | 'hello world'
            '/hello.yml?name=world' | 'hello world'
    }

    def "post test"() {
        given:
            def request = new TestRequest(path).post(body)
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
            def request = new TestRequest(path).post(body)
        expect:
            runner.run(request, Book.class).name == output
        where:
            path   | body                                                                  | output
            '/buy' | [new Book(name: "Dubbo", price: 80, publishDate: new Date())]         | 'Dubbo'
            '/buy' | ['book': new Book(name: "Dubbo", price: 80, publishDate: new Date())] | 'Dubbo'
    }

    def "urlEncodeForm test"() {
        given:
            def request = new TestRequest(
                method: HttpMethods.POST,
                path: path,
                contentType: MediaType.APPLICATION_FROM_URLENCODED,
                params: [
                    'name': 'Sam',
                    'age' : 8
                ]
            )
        expect:
            runner.run(request, String.class) == output
        where:
            path        | body          | output
            '/postTest' | '["Sam","8"]' | 'Sam is 8 years old'
    }

    def "override mapping test"() {
        given:
            def request = new TestRequest(path: path)
        expect:
            runner.run(request, String.class) == output
        where:
            path                          | output
            '/say?name=sam&count=2'       | '2'
            '/say?name=sam'               | '1'
            '/say~SL'                     | '2'
            '/say~S'                      | '1'
            '/say~S?name=sam&count=2'     | '1'
            '/say~S.yml?name=sam&count=2' | '1'
    }

    def "ambiguous mapping test"() {
        given:
            def request = new TestRequest(path: path)
        expect:
            runner.run(request, String.class) contains "Ambiguous mapping"
        where:
            path   | output
            '/say' | '1'
    }
}
