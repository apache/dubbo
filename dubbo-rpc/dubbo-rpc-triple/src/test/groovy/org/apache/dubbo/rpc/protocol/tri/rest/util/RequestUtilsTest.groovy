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

package org.apache.dubbo.rpc.protocol.tri.rest.util

import org.apache.dubbo.remoting.http12.HttpRequest
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants

import spock.lang.Specification

class RequestUtilsTest extends Specification {

    def "get path variable"() {
        given:
            def request = Mock(HttpRequest)
            request.attribute(RestConstants.URI_TEMPLATE_VARIABLES_ATTRIBUTE) >> [name: value]
        expect:
            RequestUtils.getPathVariable(request, 'name') == result
        where:
            value                      | result
            'a%20b'                    | 'a b'
            'a%2Bb'                    | 'a+b'
            'john+tag@example.com'     | 'john+tag@example.com'
            'john+tag@example.com;x=1' | 'john+tag@example.com'
    }

    def "parse matrix variables"() {
        expect:
            RequestUtils.parseMatrixVariables(value) == result
        where:
            value               | result
            'tag=a%20b'         | [tag: ['a b']]
            'tag=1.0+build,2.0' | [tag: ['1.0+build', '2.0']]
    }
}
