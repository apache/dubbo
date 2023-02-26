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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match;


import org.apache.dubbo.rpc.RpcInvocation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DubboMethodMatchTest {

    @Test
    void nameMatch() {
        DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();

        StringMatch nameStringMatch = new StringMatch();
        nameStringMatch.setExact("sayHello");

        dubboMethodMatch.setName_match(nameStringMatch);

        assertTrue(dubboMethodMatch.isMatch(new RpcInvocation(null, "sayHello", "",  "", new Class[]{}, new Object[]{})));
    }


    @Test
    void argcMatch() {
        DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
        dubboMethodMatch.setArgc(1);

        assertFalse(dubboMethodMatch.isMatch(new RpcInvocation(null, "sayHello", "",  "", new Class[]{}, new Object[]{})));
        assertTrue(dubboMethodMatch.isMatch(new RpcInvocation(null, "sayHello", "",  "", new Class[]{}, new Object[]{"1"})));
    }

    @Test
    void argpMatch() {
        DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();

        List<StringMatch> argpMatch = new ArrayList<>();

        StringMatch first = new StringMatch();
        first.setExact("java.lang.Long");

        StringMatch second = new StringMatch();
        second.setRegex(".*");

        argpMatch.add(first);
        argpMatch.add(second);

        dubboMethodMatch.setArgp(argpMatch);


        assertTrue(dubboMethodMatch.isMatch(new RpcInvocation(null, "sayHello", "",  "", new Class[]{Long.class, String.class}, new Object[]{})));
        assertFalse(dubboMethodMatch.isMatch(new RpcInvocation(null, "sayHello", "",  "", new Class[]{Long.class, String.class, String.class}, new Object[]{})));
        assertFalse(dubboMethodMatch.isMatch(new RpcInvocation(null, "sayHello", "",  "", new Class[]{}, new Object[]{})));
    }

    @Test
    void parametersMatch() {

        DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();

        List<DubboMethodArg> parametersMatch = new ArrayList<>();

        //----- index 0
        {
            DubboMethodArg dubboMethodArg0 = new DubboMethodArg();
            dubboMethodArg0.setIndex(0);

            ListDoubleMatch listDoubleMatch = new ListDoubleMatch();
            List<DoubleMatch> oneof = new ArrayList<>();

            DoubleMatch doubleMatch1 = new DoubleMatch();
            doubleMatch1.setExact(10.0);

            oneof.add(doubleMatch1);

            listDoubleMatch.setOneof(oneof);

            dubboMethodArg0.setNum_value(listDoubleMatch);

            parametersMatch.add(dubboMethodArg0);
        }

        //-----index 1

        {

            DubboMethodArg dubboMethodArg1 = new DubboMethodArg();
            dubboMethodArg1.setIndex(1);

            ListStringMatch listStringMatch = new ListStringMatch();

            List<StringMatch> oneof = new ArrayList<>();

            StringMatch stringMatch1 = new StringMatch();
            stringMatch1.setExact("sayHello");

            oneof.add(stringMatch1);

            listStringMatch.setOneof(oneof);

            dubboMethodArg1.setStr_value(listStringMatch);

            parametersMatch.add(dubboMethodArg1);
        }

        dubboMethodMatch.setArgs(parametersMatch);

        assertTrue(dubboMethodMatch.isMatch(new RpcInvocation(null, "test", "",  "", new Class[]{int.class, String.class}, new Object[]{10, "sayHello"})));
        assertFalse(dubboMethodMatch.isMatch(new RpcInvocation(null, "test", "",  "", new Class[]{int.class, String.class}, new Object[]{10, "sayHi"})));


        //-----index 2

        {

            DubboMethodArg dubboMethodArg2 = new DubboMethodArg();
            dubboMethodArg2.setIndex(2);

            BoolMatch boolMatch = new BoolMatch();
            boolMatch.setExact(true);


            dubboMethodArg2.setBool_value(boolMatch);

            parametersMatch.add(dubboMethodArg2);
        }


        assertTrue(dubboMethodMatch.isMatch(new RpcInvocation(null, "test", "",  "", new Class[]{int.class, String.class, boolean.class}, new Object[]{10, "sayHello", true})));
        assertFalse(dubboMethodMatch.isMatch(new RpcInvocation(null, "test", "",  "", new Class[]{int.class, String.class, boolean.class}, new Object[]{10, "sayHello", false})));
    }
}
