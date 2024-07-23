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

import org.apache.dubbo.rpc.protocol.tri.rest.service.DemoServiceImpl
import org.apache.dubbo.rpc.protocol.tri.rest.support.spring.service.SpringDemoServiceImpl
import org.apache.dubbo.rpc.protocol.tri.rest.test.BaseServiceTest
import org.apache.dubbo.rpc.protocol.tri.test.TestRunnerBuilder

class RestProtocolTest extends BaseServiceTest {

    @Override
    void setupService(TestRunnerBuilder builder) {
        builder.provider(new DemoServiceImpl())
        builder.provider(new SpringDemoServiceImpl())
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

    def "spring bean argument test"() {
        expect:
            runner.get(path) contains output
        where:
            path                                                                                  | output
            '/springBean?id=1&name=sam'                                                           | '"id":1,"name":"sam"'
            '/springBean?name=sam&phone=123&email=a@b.com'                                        | '"email":"a@b.com","name":"sam","phone":"123"'
            '/springBean?group.name=g1&group.owner.name=jack'                                     | '"group":{"id":0,"name":"g1","owner":{"name":"jack"'
            '/springBean?group.parent.parent.children[0].name=xx'                                 | '"group":{"id":0,"parent":{"id":0,"parent":{"children":[{"id":0,"name":"xx"}],"id":0}}}'
            '/springBean?ids=3&ids=4'                                                             | '"ids":[3,4]'
            '/springBean?ids[]=3&ids[]=4'                                                         | '"ids":[3,4]'
            '/springBean?ids[1]=3&ids[2]=4'                                                       | '"ids":[0,3,4]'
            '/springBean?scores=3&scores=4'                                                       | '"scores":[3,4]'
            '/springBean?scores[]=3&scores[]=4'                                                   | '"scores":[3,4]'
            '/springBean?scores[1]=3&scores[2]=4'                                                 | '"scores":[null,3,4]'
            '/springBean?tags[0].name=a&tags[0].value=b&tags[1].name=c&tags[1].value=d'           | '"tags":[{"name":"a","value":"b"},{"name":"c","value":"d"}]'
            '/springBean?tagsA[0].name=a&tagsA[0].value=b&tagsA[1].name=c&tagsA[1].value=d'       | '"tagsA":[{"name":"a","value":"b"},{"name":"c","value":"d"}]'
            '/springBean?tagsB[0].name=e&tagsB[1].name=c&tagsB[1].value=d'                        | '"tagsB":[{"name":"e","value":"b"},{"name":"c","value":"d"}]'
            '/springBean?tagsC[0].name=e&tagsC[1].name=c&tagsC[1].value=d'                        | '"tagsC":[{"name":"e","value":"b"},{"name":"c","value":"d"}]'
            '/springBean?groupMaps[0].one.name=a&groupMaps[1].two.name=b'                         | '"groupMaps":[{},{}]' //x
            '/springBean?id=1&features.a=xx&features.b=2'                                         | '"features":{},"id":1' //x
            '/springBean?id=1&features[a]=xx&features[b]=2'                                       | '"features":{"a":"xx","b":"2"}'
            '/springBean?group.id=2&group.features.a=1&group.features.b=xx'                       | '"group":{"features":{},"id":2}' //x
            '/springBean?tagMap.a.name=a&tagMap.a.value=b&tagMap.b.name=c&tagMap.b.value=d'       | '"tagMap":{}' //x
            '/springBean?tagMapA.a.name=e&tagMapA.b.name=c&tagMapA.b.value=d'                     | '"tagMapA":{"a":{"name":"a","value":"b"}}' //x
            '/springBean?tagMapB[2].name=a&tagMapB[2].value=b&tagMapB[3].name=c'                  | '"tagMapB":{2:{"name":"a","value":"b"},3:{"name":"c"}}'
            '/springBean?groupsMap.one[0].name=a&groupsMap.one[1].name=b&groupsMap.two[1].name=c' | '"groupsMap":{}' //x
    }

}
