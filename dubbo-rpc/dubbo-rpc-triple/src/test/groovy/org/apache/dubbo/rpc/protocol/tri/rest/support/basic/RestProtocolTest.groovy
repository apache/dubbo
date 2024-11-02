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

package org.apache.dubbo.rpc.protocol.tri.rest.support.basic

import org.apache.dubbo.remoting.http12.message.MediaType
import org.apache.dubbo.rpc.protocol.tri.rest.service.Book
import org.apache.dubbo.rpc.protocol.tri.rest.service.DemoServiceImpl
import org.apache.dubbo.rpc.protocol.tri.rest.test.BaseServiceTest
import org.apache.dubbo.rpc.protocol.tri.test.TestRequest
import org.apache.dubbo.rpc.protocol.tri.test.TestRunnerBuilder

import io.netty.buffer.AbstractByteBuf
import io.netty.util.ResourceLeakDetector

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

    def "hello world by post"() {
        expect:
            runner.post(path, body) == output
        where:
            path                | body       | output
            '/hello?name=world' | null       | 'hello world'
            '/hello'            | ''         | 'hello '
            '/hello?name=world' | ''         | 'hello world'
            '/hello'            | '"world"'  | 'hello world'
            '/hello?name=world' | '"galaxy"' | 'hello galaxy'
    }

    def "argument test"() {
        expect:
            runner.post(path, body) == output
        where:
            path                | body                        | output
            '/argTest'          | '["Sam","8"]'               | 'Sam is 8 years old'
            '/argTest'          | '{"name": "Sam", "age": 8}' | 'Sam is 8 years old'
            '/argTest?name=Sam' | '{"age": 8}'                | 'Sam is 8 years old'
    }

    def "list argument body test"() {
        expect:
            runner.post(path, body) contains output
        where:
            path                      | body        | output
            '/listArgBodyTest?age=2'  | '[1,2]'     | '[1,2]'
            '/listArgBodyTest2?age=1' | '[[1,2],2]' | '[1,2]'
    }

    def "map argument body test"() {
        expect:
            runner.post(path, body) contains output
        where:
            path                     | body                        | output
            '/mapArgBodyTest?age=2'  | '{"1":["2",3],"4":[5,"6"]}' | '{4:[5,6],1:[2,3]}'
            '/mapArgBodyTest2?age=1' | '[{"1":[2,3],"4":[5,6]},2]' | '{4:[5,6],1:[2,3]}'
    }

    def "bean argument test"() {
        expect:
            runner.post(path, body, Book.class).name == output
        where:
            path    | body                                      | output
            '/buy'  | new Book(name: "Dubbo")                   | 'Dubbo'
            '/buy'  | [new Book(name: "Dubbo")]                 | 'Dubbo'
            '/buy2' | [new Book(name: "Dubbo"), 2]              | 'Dubbo'
            '/buy2' | [book: new Book(name: "Dubbo"), count: 2] | 'Dubbo'
    }

    def "bean argument get test"() {
        expect:
            runner.get(path, Book.class).price == output
        where:
            path                                     | output
            '/beanArgTest'                           | 0
            '/beanArgTest?quote=5'                   | 5
            '/beanArgTest?book={"price": 6}'         | 6
            '/beanArgTest?book={"price": 6}&quote=5' | 5
    }

    def "bean argument post test"() {
        expect:
            runner.post(path, body, Book.class).price == output
        where:
            path            | body                                 | output
            '/beanArgTest'  | []                                   | 0
            '/beanArgTest'  | [quote: 5]                           | 5
            '/beanArgTest'  | [book: new Book(price: 6)]           | 6
            '/beanArgTest'  | [book: new Book(price: 6), quote: 5] | 5
            '/beanArgTest'  | [price: 6, quote: 5]                 | 5
            '/beanArgTest2' | '{"price": 5}'                       | 5
            '/beanArgTest2' | '[{"price": 5}]'                     | 5
    }

    def "advance bean argument get test"() {
        expect:
            runner.get(path) contains output
        where:
            path                                                                            | output
            '/bean?id=1&name=sam'                                                           | '"id":1,"name":"sam"'
            '/bean?user.id=1&user.name=sam'                                                 | '"id":1,"name":"sam"'
            '/bean?name=sam&p=123&email=a@b.com'                                            | '"email":"a@b.com","name":"sam","phone":"123"'
            '/bean?group.name=g1&group.owner.name=jack'                                     | '"group":{"id":0,"name":"g1","owner":{"name":"jack"'
            '/bean?group.parent.parent.children[0].name=xx'                                 | '"group":{"id":0,"parent":{"id":0,"parent":{"children":[{"id":0,"name":"xx"}],"id":0}}}'
            '/bean?group={"name":"g1","id":2}'                                              | '"group":{"id":2,"name":"g1"}'
            '/bean?ids=3&ids=4'                                                             | '"ids":[3,4]'
            '/bean?ids[]=3&ids[]=4'                                                         | '"ids":[3,4]'
            '/bean?ids[1]=3&ids[2]=4'                                                       | '"ids":[0,3,4]'
            '/bean?scores=3&scores=4'                                                       | '"scores":[3,4]'
            '/bean?scores[]=3&scores[]=4'                                                   | '"scores":[3,4]'
            '/bean?scores[1]=3&scores[2]=4'                                                 | '"scores":[null,3,4]'
            '/bean?tags[0].name=a&tags[0].value=b&tags[1].name=c&tags[1].value=d'           | '"tags":[{"name":"a","value":"b"},{"name":"c","value":"d"}]'
            '/bean?tagsA[0].name=a&tagsA[0].value=b&tagsA[1].name=c&tagsA[1].value=d'       | '"tagsA":[{"name":"a","value":"b"},{"name":"c","value":"d"}]'
            '/bean?tagsB[0].name=e&tagsB[1].name=c&tagsB[1].value=d'                        | '"tagsB":[{"name":"e","value":"b"},{"name":"c","value":"d"}]'
            '/bean?tagsC[0].name=e&tagsC[1].name=c&tagsC[1].value=d'                        | '"tagsC":[{"name":"e","value":"b"},{"name":"c","value":"d"}]'
            '/bean?groupMaps[0].one.name=a&groupMaps[1].two.name=b'                         | '"groupMaps":[{"one":{"id":0,"name":"a"}},{"two":{"id":0,"name":"b"}}]'
            '/bean?id=1&features.a=xx&features.b=2'                                         | '"features":{"a":"xx","b":"2"}'
            '/bean?id=1&features[a]=xx&features[b]=2'                                       | '"features":{"a":"xx","b":"2"}'
            '/bean?group.id=2&group.features.a=1&group.features.b=xx'                       | '"group":{"features":{"a":"1","b":"xx"},"id":2}'
            '/bean?tagMap.a.name=a&tagMap.a.value=b&tagMap.b.name=c&tagMap.b.value=d'       | '"tagMap":{"a":{"name":"a","value":"b"},"b":{"name":"c","value":"d"}}'
            '/bean?tagMapA.a.name=e&tagMapA.b.name=c&tagMapA.b.value=d'                     | '"tagMapA":{"a":{"name":"e","value":"b"},"b":{"name":"c","value":"d"}}'
            '/bean?tagMapB[2].name=a&tagMapB[2].value=b&tagMapB[3].name=c'                  | '"tagMapB":{2:{"name":"a","value":"b"},3:{"name":"c"}}'
            '/bean?groupsMap.one[0].name=a&groupsMap.one[1].name=b&groupsMap.two[1].name=c' | '"groupsMap":{"one":[{"id":0,"name":"a"},{"id":0,"name":"b"}],"two":[null,{"id":0,"name":"c"}]}'
    }

    def "bean body test"() {
        expect:
            runner.post(path, body) contains output
        where:
            path                   | body                                              | output
            '/beanBodyTest?age=2'  | '[{"id":1,"name":"g1"},{"id":2,"name":"g2"}]'     | '[{"id":1,"name":"g1"},{"id":2,"name":"g2"}]'
            '/beanBodyTest2?age=1' | '[[{"id":1,"name":"g1"},{"id":2,"name":"g2"}],2]' | '[{"id":1,"name":"g1"},{"id":2,"name":"g2"}]'
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
            path       | output
            '/argTest' | 'Sam is 8 years old'
    }

    @SuppressWarnings('GroovyAccessibility')
    def "urlEncodeForm body test"() {
        given:
            def level = ResourceLeakDetector.level
            def leaks = AbstractByteBuf.leakDetector.allLeaks
            ResourceLeakDetector.level = ResourceLeakDetector.Level.PARANOID
            leaks.clear()
        and:
            def request = new TestRequest(
                path: path,
                contentType: MediaType.APPLICATION_FROM_URLENCODED,
                body: body
            )
        expect:
            runner.post(request) == output
            leaks.empty
        cleanup:
            ResourceLeakDetector.level = level
        where:
            path       | body             | output
            '/argTest' | 'name=Sam&age=8' | 'Sam is 8 years old'
            '/argTest' | '' | 'null is 0 years old'
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

    def "use interface argument name test"() {
        expect:
            runner.get(path) contains output
        where:
            path                    | output
            '/argNameTest?name=Sam' | 'Sam'
    }

    def "pb server stream test"() {
        expect:
            runner.posts(path, body).size() == output
        where:
            path              | body               | output
            '/pbServerStream' | '{"service": "3"}' | 3
            '/pbServerStream' | '{}'               | 0
    }

    def "pb server stream get test"() {
        expect:
            runner.gets(path).size() == output
        where:
            path                                       | output
            '/pbServerStream?request={"service": "3"}' | 3
    }

    def "produce test"() {
        given:
            def request = new TestRequest(
                path: path,
                accept: accept
            )
        expect:
            runner.post(request) == output
        where:
            path                      | accept             | output
            '/produceTest?name=world' | ''                 | 'world'
            '/produceTest?name=world' | 'text/plain'       | 'world'
            '/produceTest?name=world' | 'application/json' | '{"message":"Could not find acceptable representation","status":"406"}'
    }

    def "mismatch test"() {
        given:
            def request = new TestRequest(
                method: method,
                path: path,
                contentType: contentType,
                accept: accept
            )
        expect:
            runner.run(request, String.class) == output
        where:
            method | path                       | contentType        | accept             | output
            'POST' | '/mismatchTest?name=world' | 'text/plain'       | 'text/plain'       | 'world'
            'POST' | '/mismatchTest1'           | 'text/plain'       | 'text/plain'       | '{"message":"Invoker not found","status":"404"}'
            'GET'  | '/mismatchTest'            | ''                 | ''                 | '{"message":"Request method \'GET\' not supported","status":"405"}'
            'POST' | '/mismatchTest'            | 'application/json' | 'text/plain'       | '{"message":"Content type \'application/json\' not supported","status":"415"}'
            'POST' | '/mismatchTest'            | 'text/plain'       | 'application/json' | '{"message":"Could not find acceptable representation","status":"406"}'
            'POST' | '/mismatchTest?name=earth' | 'text/plain'       | 'text/plain'       | '{"message":"Unsatisfied query parameter conditions","status":"400"}'
    }

    def "consistent with SpringMVC"() {
        given:
            def request = new TestRequest(
                path: path,
                accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8'
            )
        expect:
            runner.run(request).contentType == contentType
        where:
            path | contentType
            '/beanArgTest' | 'application/json'
    }

}
