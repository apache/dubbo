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

import org.apache.dubbo.common.URL
import org.apache.dubbo.rpc.protocol.tri.rest.PathParserException

import spock.lang.Specification

class PathUtilsTest extends Specification {

    def "GetContextPath"() {
        expect:
            PathUtils.getContextPath(URL.valueOf(url)) == result
        where:
            result | url
            ''     | 'tri://127.0.0.1'
            ''     | 'tri://127.0.0.1/'
            ''     | 'tri://127.0.0.1/one'
            ''     | 'tri://127.0.0.1/test.Demo?interface=test.DeMo'
            'one'  | 'tri://127.0.0.1/one/test.Demo?interface=test.Demo'
            'one'  | 'tri://127.0.0.1/one/?interface=test.Demo'
    }

    def "IsDirectPath"() {
        expect:
            PathUtils.isDirectPath(path) == result
        where:
            result | path
            true   | '/one'
            true   | '/one/{ab/c'
            false  | '/one/*'
            false  | '/one/**'
            false  | '/one/a?c'
            false  | '/one/{ab}/c'
    }

    def "Combine"() {
        expect:
            PathUtils.combine(path1, path2) == result
        where:
            path1        | path2          | result
            '/hotels'    | ''             | '/hotels'
            ''           | '/hotels'      | '/hotels'
            '/hotels'    | '/bookings'    | '/hotels/bookings'
            '/hotels'    | 'bookings'     | '/hotels/bookings'
            '/hotels/'   | '/bookings'    | '/hotels/bookings'
            '/hotels/'   | '/bookings/'   | '/hotels/bookings/'
            '/hotels/*'  | '/bookings'    | '/hotels/bookings'
            '/hotels/**' | '/bookings'    | '/hotels/**/bookings'
            '/hotels'    | '{hotel}'      | '/hotels/{hotel}'
            '/{hotels}'  | '/hotel'       | '/{hotels}/hotel'
            '/hotels/*'  | '{hotel}'      | '/hotels/{hotel}'
            '/hotels/**' | '{hotel}'      | '/hotels/**/{hotel}'
            '/*.html'    | '/hotels.html' | '/hotels.html'
            '/hotels.*'  | '/hotels.html' | '/hotels.html'
            '/*.html'    | '/hotels'      | '/hotels.html'
            '/'          | '/bookings'    | '/bookings'
            '/hotels'    | '/'            | '/hotels/'
    }

    def "Combine failed"() {
        when:
            PathUtils.combine(path1, path2)
        then:
            thrown(PathParserException)
        where:
            path1     | path2
            '/*.html' | '/*.txt'
            '/*.html' | '/hotel.txt'
    }

    def "Normalize"() {
        expect:
            PathUtils.normalize(path) == result
        where:
            path                      | result
            ''                        | '/'
            '/'                       | '/'
            '//'                      | '/'
            '/.'                      | '/'
            '/./'                     | '/'
            '/./.'                    | '/'
            '..'                      | '/..'
            '/..'                     | '/..'
            '../'                     | '/../'
            '/../'                    | '/../'
            'one'                     | '/one'
            '.one'                    | '/.one'
            '/one.two'                | '/one.two'
            ' /one '                  | '/one'
            '\t /one \t\n \r'         | '/one'
            '/one#two'                | '/one'
            '/one?two'                | '/one'
            '/one'                    | '/one'
            '/one/'                   | '/one/'
            '/one//'                  | '/one/'
            '/one/.'                  | '/one/'
            'a/..'                    | '/'
            '/one/..'                 | '/'
            '/one/.../'               | '/one/.../'
            '/one/../'                | '/'
            '/one/../../'             | '/../'
            '/one/../../../'          | '/../../'
            '/./one/../../../'        | '/../../'
            '/one/../two'             | '/two'
            '/one/./two'              | '/one/two'
            '/one/./two/..'           | '/one/'
            '/one/./two/../'          | '/one/'
            '/one/./two/../three'     | '/one/three'
            '/one/./two/../three/'    | '/one/three/'
            '/one/./two/../three/.'   | '/one/three/'
            '/one/./two/../three/..'  | '/one/'
            '/one/./two/../three/../' | '/one/'
    }

    def "Normalize with context path"() {
        expect:
            PathUtils.normalize(contextPath, path) == result
        where:
            contextPath | path   | result
            ''          | ''     | '/'
            ''          | '/'    | '/'
            '/'         | ''     | '/'
            ''          | '/one' | '/one'
            '/one'      | ''     | '/one'
            '/one'      | 'two'  | '/one/two'
            'one'       | 'two'  | '/one/two'
            '/one'      | '/two' | '/one/two'
            '/one//'    | '/two' | '/one/two'
    }
}
