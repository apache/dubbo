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
package org.apache.dubbo.common.profiler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProfilerTest {

    @Test
    public void testProfiler() {
        ProfilerEntry one = Profiler.start("1");
        ProfilerEntry two = Profiler.enter(one, "1-2");

        ProfilerEntry three = Profiler.enter(two, "1-2-3");

        ProfilerEntry four = Profiler.enter(three, "1-2-3-4");
        Assertions.assertEquals(three, Profiler.release(four));

        ProfilerEntry five = Profiler.enter(three, "1-2-3-5");
        Assertions.assertEquals(three, Profiler.release(five));

        Assertions.assertEquals(two, Profiler.release(three));

        ProfilerEntry six = Profiler.enter(two, "1-2-6");
        Assertions.assertEquals(two, Profiler.release(six));

        ProfilerEntry seven = Profiler.enter(six, "1-2-6-7");
        Assertions.assertEquals(six, Profiler.release(seven));

        ProfilerEntry eight = Profiler.enter(six, "1-2-6-8");
        Assertions.assertEquals(six, Profiler.release(eight));

        Assertions.assertEquals(2, two.getSub().size());
        Assertions.assertEquals(three, two.getSub().get(0));
        Assertions.assertEquals(six, two.getSub().get(1));

        Profiler.release(two);

        ProfilerEntry nine = Profiler.enter(one, "1-9");
        Profiler.release(nine);

        Profiler.release(one);

        /*
         * Start time: 287395734500659
         * +-[ Offset: 0.000000ms; Usage: 4.721583ms, 100% ] 1
         *   +-[ Offset: 0.013136ms; Usage: 4.706288ms, 99% ] 1-2
         *   |  +-[ Offset: 0.027903ms; Usage: 4.662918ms, 98% ] 1-2-3
         *   |  |  +-[ Offset: 0.029742ms; Usage: 0.003785ms, 0% ] 1-2-3-4
         *   |  |  +-[ Offset: 4.688477ms; Usage: 0.001398ms, 0% ] 1-2-3-5
         *   |  +-[ Offset: 4.693346ms; Usage: 0.000316ms, 0% ] 1-2-6
         *   |     +-[ Offset: 4.695191ms; Usage: 0.000212ms, 0% ] 1-2-6-7
         *   |     +-[ Offset: 4.696655ms; Usage: 0.000195ms, 0% ] 1-2-6-8
         *   +-[ Offset: 4.721044ms; Usage: 0.000270ms, 0% ] 1-9
         */
        Assertions.assertEquals(Profiler.buildDetail(one), Profiler.buildDetail(two));
        Assertions.assertEquals(Profiler.buildDetail(one), Profiler.buildDetail(three));
        Assertions.assertEquals(Profiler.buildDetail(one), Profiler.buildDetail(four));
        Assertions.assertEquals(Profiler.buildDetail(one), Profiler.buildDetail(five));
        Assertions.assertEquals(Profiler.buildDetail(one), Profiler.buildDetail(six));
        Assertions.assertEquals(Profiler.buildDetail(one), Profiler.buildDetail(seven));
        Assertions.assertEquals(Profiler.buildDetail(one), Profiler.buildDetail(eight));
        Assertions.assertEquals(Profiler.buildDetail(one), Profiler.buildDetail(nine));
    }

    @Test
    public void testBizProfiler() {
        Assertions.assertNull(Profiler.getBizProfiler());

        ProfilerEntry one = Profiler.start("1");

        Profiler.setToBizProfiler(one);

        Profiler.release(Profiler.enter(Profiler.getBizProfiler(), "1-2"));

        Assertions.assertEquals(one, Profiler.getBizProfiler());
        Assertions.assertEquals(1, one.getSub().size());

        Profiler.removeBizProfiler();
        Assertions.assertNull(Profiler.getBizProfiler());
    }
}
