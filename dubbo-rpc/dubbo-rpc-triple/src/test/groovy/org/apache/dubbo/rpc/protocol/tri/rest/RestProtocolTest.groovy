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
        expect:
            runner.get(path) == output
        where:
            path                    | output
            '/hello?name=world'     | 'hello world'
            '/hello/?name=world'    | 'hello world'
            '/hello.yml?name=world' | 'hello world'
    }

    def "post test"() {
        expect:
            runner.post(path, body) == output
        where:
            path                 | body                        | output
            '/postTest'          | '["Sam","8"]'               | 'Sam is 8 years old'
            '/postTest'          | '{"name": "Sam", "age": 8}' | 'Sam is 8 years old'
            '/postTest?name=Sam' | '{"age": 8}'                | 'Sam is 8 years old'
            '/postTest?name=Sam' | '{"age": 8}'                | 'Sam is 8 years old'
    }

    def "bean test"() {
        expect:
            runner.post(path, body, Book.class).name == output
        where:
            path    | body                                      | output
            '/buy'  | new Book(name: "Dubbo")                   | 'Dubbo'
            '/buy'  | [new Book(name: "Dubbo")]                 | 'Dubbo'
            '/buy2' | [new Book(name: "Dubbo"), 2]              | 'Dubbo'
            '/buy2' | [book: new Book(name: "Dubbo"), count: 2] | 'Dubbo'
    }

    def "urlEncodeForm test"() {
        given:
            def request = new TestRequest(
                path: path,
                contentType: MediaType.APPLICATION_FROM_URLENCODED,
                params: [
                    'name': 'Sam',
                    'age' : 8
                ]
            )
        expect:
            runner.post(request) == output
        where:
            path        | output
            '/postTest' | 'Sam is 8 years old'
    }

    def "override mapping test"() {
        expect:
            runner.get(path) == output
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
        expect:
            runner.get(path) contains "Ambiguous mapping"
        where:
            path   | _
            '/say' | _
    }

    def "no interface method test"() {
        expect:
            runner.get(path) contains output
        where:
            path                     | output
            '/noInterface'           | 'ok'
            '/noInterfaceAndMapping' | '404'
    }
}
