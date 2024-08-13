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

package org.apache.dubbo.rpc.protocol.tri.rest.filter

import org.apache.dubbo.common.URL
import org.apache.dubbo.rpc.Invoker
import org.apache.dubbo.rpc.model.ApplicationModel

import spock.lang.Specification

class RestFilterTest extends Specification {

    @SuppressWarnings('GroovyAccessibility')
    def "test filter patterns"() {
        given:
            Invoker invoker = Mock(Invoker)
            invoker.getUrl() >> URL.valueOf("tri://127.0.0.1/test?extension=org.apache.dubbo.rpc.protocol.tri.rest.filter.TestRestFilter")

            var filter = new RestExtensionExecutionFilter(ApplicationModel.defaultModel())
        expect:
            filter.matchFilters(filter.getFilters(invoker), path).length == len
        where:
            path            | len
            '/filter/one'   | 1
            '/filter/one/1' | 1
            '/one.filter'   | 2
            '/filter/two'   | 2
    }

}
