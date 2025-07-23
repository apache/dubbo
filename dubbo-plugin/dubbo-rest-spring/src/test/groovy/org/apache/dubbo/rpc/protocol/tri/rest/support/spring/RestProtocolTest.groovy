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

package org.apache.dubbo.rpc.protocol.tri.rest.support.spring

import org.apache.dubbo.remoting.http12.message.MediaType
import org.apache.dubbo.rpc.protocol.tri.rest.service.Book
import org.apache.dubbo.rpc.protocol.tri.rest.support.spring.service.SpringDemoServiceImpl
import org.apache.dubbo.rpc.protocol.tri.rest.support.spring.service.SpringDemoService
import org.apache.dubbo.rpc.protocol.tri.rest.test.BaseServiceTest
import org.apache.dubbo.rpc.protocol.tri.test.TestRequest
import org.apache.dubbo.rpc.protocol.tri.test.TestRunnerBuilder

class RestProtocolTest extends BaseServiceTest {

    @Override
    void setupService(TestRunnerBuilder builder) {
        builder.provider(SpringDemoService.class, new SpringDemoServiceImpl())
    }

    def "hello world"() {
        expect:
            runner.get(path) == output
        where:
            path                           | output
            '/spring/hello?name=world'     | 'hello world'
            '/spring/hello/?name=world'    | 'hello world'
            '/spring/hello.yml?name=world' | 'hello world'
    }

    def "list argument body test"() {
        expect:
            runner.post(path, body) contains output
        where:
            path                            | body    | output
            '/spring/listArgBodyTest?age=2' | '[1,2]' | '[1,2]'
    }

    def "map argument body test"() {
        expect:
            runner.post(path, body) contains output
        where:
            path                           | body                        | output
            '/spring/mapArgBodyTest?age=2' | '{"1":["2",3],"4":[5,"6"]}' | '{4:[5,6],1:[2,3]}'
    }

    def "bean argument get test"() {
        expect:
            runner.get(path, Book.class).price == output
        where:
            path                          | output
            '/spring/beanArgTest'         | 0
            '/spring/beanArgTest?quote=5' | 5
    }

    def "bean argument post test"() {
        expect:
            runner.post(path, body, Book.class).price == output
        where:
            path                   | body           | output
            '/spring/beanArgTest'  | [:]            | 0
            '/spring/beanArgTest'  | [price: 6]     | 6
            '/spring/beanArgTest2' | '{"price": 5}' | 5
    }

    def "spring bean argument test"() {
        expect:
            runner.get(path) contains output
        where:
            path                                                                             | output
            '/spring/bean?id=1&name=sam'                                                     | '"id":1,"name":"sam"'
            '/spring/bean?name=sam&phone=123&email=a@b.com'                                  | '"email":"a@b.com","name":"sam","phone":"123"'
            '/spring/bean?group.name=g1&group.owner.name=jack'                               | '"group":{"id":0,"name":"g1","owner":{"name":"jack"'
            '/spring/bean?group.parent.parent.children[0].name=xx'                           | '"group":{"id":0,"parent":{"id":0,"parent":{"children":[{"id":0,"name":"xx"}],"id":0}}}'
            '/spring/bean?ids=3&ids=4'                                                       | '"ids":[3,4]'
            '/spring/bean?ids[]=3&ids[]=4'                                                   | '"ids":[3,4]'
            '/spring/bean?ids[1]=3&ids[2]=4'                                                 | '"ids":[0,3,4]'
            '/spring/bean?scores=3&scores=4'                                                 | '"scores":[3,4]'
            '/spring/bean?scores[]=3&scores[]=4'                                             | '"scores":[3,4]'
            '/spring/bean?scores[1]=3&scores[2]=4'                                           | '"scores":[null,3,4]'
            '/spring/bean?tags[0].name=a&tags[0].value=b&tags[1].name=c&tags[1].value=d'     | '"tags":[{"name":"a","value":"b"},{"name":"c","value":"d"}]'
            '/spring/bean?tagsA[0].name=a&tagsA[0].value=b&tagsA[1].name=c&tagsA[1].value=d' | '"tagsA":[{"name":"a","value":"b"},{"name":"c","value":"d"}]'
            '/spring/bean?tagsB[0].name=e&tagsB[1].name=c&tagsB[1].value=d'                  | '"tagsB":[{"name":"e","value":"b"},{"name":"c","value":"d"}]'
            '/spring/bean?tagsC[0].name=e&tagsC[1].name=c&tagsC[1].value=d'                  | '"tagsC":[{"name":"e","value":"b"},{"name":"c","value":"d"}]'
            '/spring/bean?id=1&features[a]=xx&features[b]=2'                                 | '"features":{"a":"xx","b":"2"}'
            '/spring/bean?tagMapB[2].name=a&tagMapB[2].value=b&tagMapB[3].name=c'            | '"tagMapB":{2:{"name":"a","value":"b"},3:{"name":"c"}}'
    }

    def "bean body test"() {
        expect:
            runner.post(path, body) contains output
        where:
            path                         | body                                          | output
            '/spring/beanBodyTest?age=2' | '[{"id":1,"name":"g1"},{"id":2,"name":"g2"}]' | '[{"id":1,"name":"g1"},{"id":2,"name":"g2"}]'
    }

    def "multiValueMap test"() {
        expect:
            runner.get(path) contains output
        where:
            path                                            | output
            '/spring/multiValueMapTest?name=1&name=2&age=8' | '{"name":[1,2],"age":[8]}'
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
            path              | output
            '/spring/argTest' | 'Sam is 8 years old'
    }

    def "no interface method test"() {
        expect:
            runner.get(path) contains output
        where:
            path                            | output
            '/spring/noInterface'           | 'ok'
            '/spring/noInterfaceAndMapping' | '404'
    }

    def "use interface argument name test"() {
        expect:
            runner.get(path) contains output
        where:
            path                           | output
            '/spring/argNameTest?name=Sam' | 'Sam'
    }

    def "pb server stream test"() {
        expect:
            runner.posts(path, body).size() == output
        where:
            path                     | body               | output
            '/spring/pbServerStream' | '{"service": "3"}' | 3
            '/spring/pbServerStream' | '{}'               | 0
    }

}
