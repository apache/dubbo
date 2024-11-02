/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the 'License'); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.rpc.protocol.tri.rest.mapping

import spock.lang.Specification

class RadixTreeTest extends Specification {

    def "match"() {
        given:
            def tree = new RadixTree<String>()
            tree.addPath('/a/*', 'abc')
            tree.addPath('/a/{x}/d/e', 'bcd')
            tree.addPath('/a/{v:.*}/e', 'bcd')
        when:
            def match = tree.match('/a/b/d/e')
        then:
            !match.empty
    }

    def "clear"() {
        given:
            def tree = new RadixTree<String>()
            tree.addPath('/a/*', 'abc')
            tree.addPath('/a/{x}/d/e', 'bcd')
            tree.addPath('/a/{v:.*}/e', 'bcd')
        when:
            tree.remove(s -> s in ['abc', 'bcd'])
        then:
            tree.empty
    }

    def "test end match"() {
        given:
            def tree = new RadixTree<Boolean>()
            tree.addPath('/a/*/*', true)
        expect:
            tree.match(path).size() == len
        where:
            path       | len
            '/a'       | 0
            '/a/b'     | 0
            '/a/b/c'   | 1
            '/a/b/c/d' | 0
    }

    def "test sub path match"() {
        given:
            def tree = new RadixTree<String>();
            tree.addPath("/update/{ruleId}", "a")
            tree.addPath("/update/{ruleId}/state", "b")
        expect:
            tree.match(path).get(0).value == result
        where:
            path                    | result
            '/update/1222222'       | 'a'
            '/update/1222222/state' | 'b'
    }
}
