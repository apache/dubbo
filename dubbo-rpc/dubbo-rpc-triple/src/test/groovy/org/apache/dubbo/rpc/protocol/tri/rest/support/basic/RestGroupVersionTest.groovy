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

import org.apache.dubbo.remoting.http12.rest.Mapping
import org.apache.dubbo.remoting.http12.rest.Param
import org.apache.dubbo.rpc.RpcContext
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol
import org.apache.dubbo.rpc.protocol.tri.rest.test.BaseServiceTest
import org.apache.dubbo.rpc.protocol.tri.test.TestRequest
import org.apache.dubbo.rpc.protocol.tri.test.TestRunnerBuilder

import groovy.transform.CompileStatic

class RestGroupVersionTest extends BaseServiceTest {

    @Override
    void setupService(TestRunnerBuilder builder) {
        var service = new GroupVersionServiceImpl()
        builder.provider(service, ['group': 'g0', 'version': '1.0.0'])
        builder.provider(service, ['group': 'g1', 'version': '1.0.1'])
        builder.provider(service, ['group': 'g2', 'version': '1.0.2'])
        builder.provider(service, ['group': 'g3'])
        builder.provider(service, ['version': '1.0.4'])
    }

    def "group version condition test"() {
        given:
            def request = new TestRequest(
                path: path,
                headers: [
                    'rest-service-group'  : group,
                    'rest-service-version': version,
                ]
            )
        expect:
            runner.get(request) == output
        where:
            path                         | group | version | output
            '/groupVersionTest?name=cat' | 'g1'  | '1.0.1' | 'cat-g1-1.0.1'
            '/groupVersionTest?name=cat' | 'g1'  | null    | 'cat-g1-1.0.1'
            '/groupVersionTest?name=cat' | null  | '1.0.1' | 'cat-g1-1.0.1'
            '/groupVersionTest?name=cat' | 'g2'  | '1.0.2' | 'cat-g2-1.0.2'
            '/groupVersionTest?name=cat' | 'g2'  | null    | 'cat-g2-1.0.2'
            '/groupVersionTest?name=cat' | null  | '1.0.2' | 'cat-g2-1.0.2'
            '/groupVersionTest?name=cat' | 'g3'  | '1.0.3' | 'cat-g3-null'
            '/groupVersionTest?name=cat' | 'g4'  | '1.0.4' | 'cat-null-1.0.4'
            '/groupVersionTest?name=cat' | 'g0'  | '1.0.9' | 'cat-g0-1.0.0'
            '/groupVersionTest?name=cat' | 'g9'  | '1.0.9' | 'cat-g0-1.0.0'
    }

    @Mapping('/')
    @CompileStatic
    interface GroupVersionService {

        String groupVersionTest(@Param('name') String name);
    }

    @CompileStatic
    static class GroupVersionServiceImpl implements GroupVersionService {

        @Override
        String groupVersionTest(String name) {
            var url = RpcContext.serviceContext.url
            return "$name-$url.group-$url.version"
        }
    }

}
