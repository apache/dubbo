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

import org.apache.dubbo.remoting.http12.HttpMethods
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta

import spock.lang.Specification

class RegistrationSpec extends Specification {

    def "isMethodOverlap should return true when methods overlap"() {
        given:
            def mapping1 = new RequestMapping.Builder().method(HttpMethods.GET.name(), HttpMethods.PUT.name()).build()
            def mapping2 = new RequestMapping.Builder().method(HttpMethods.PUT.name(), HttpMethods.POST.name()).build()
            def meta = GroovyMock(HandlerMeta)
            def reg1 = new Registration(mapping1, meta)
            def reg2 = new Registration(mapping2, meta)

        expect:
            reg1.isMappingOverlap(reg2)
    }

    def "isMethodOverlap should return false when methods do not overlap"() {
        given:
            def mapping1 = new RequestMapping.Builder().method(HttpMethods.GET.name()).build()
            def mapping2 = new RequestMapping.Builder().method(HttpMethods.PUT.name()).build()

            def meta = GroovyMock(HandlerMeta)
            def reg1 = new Registration(mapping1, meta)
            def reg2 = new Registration(mapping2, meta)

        expect:
            !reg1.isMappingOverlap(reg2)
    }

    def "isMethodOverlap should return true if both MethodsCondition is null"() {
        given:
            def mapping1 = new RequestMapping.Builder().build()
            def mapping2 = new RequestMapping.Builder().build()

            def meta = GroovyMock(HandlerMeta)
            def reg1 = new Registration(mapping1, meta)
            def reg2 = new Registration(mapping2, meta)

        expect:
            reg1.isMappingOverlap(reg2)
    }

    def "isMethodOverlap should return false if only one MethodsCondition is null"() {
        given:
            def mapping1 = new RequestMapping.Builder().method(HttpMethods.GET.name()).build()
            def mapping2 = new RequestMapping.Builder().method().build()

            def meta = GroovyMock(HandlerMeta)
            def reg1 = new Registration(mapping1, meta)
            def reg2 = new Registration(mapping2, meta)

        expect:
            reg1.isMappingOverlap(reg2)
    }

}
