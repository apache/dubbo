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
        Assertions.assertEquals(two, Profiler.release(three));

        ProfilerEntry four = Profiler.enter(two, "1-2-4");
        Assertions.assertEquals(two, Profiler.release(four));

        Assertions.assertEquals(2, two.getSub().size());
        Assertions.assertEquals(three, two.getSub().get(0));
        Assertions.assertEquals(four, two.getSub().get(1));

        Profiler.release(two);
        Profiler.release(one);
        Assertions.assertEquals(Profiler.buildDetail(one), Profiler.buildDetail(two));
        Assertions.assertEquals(Profiler.buildDetail(one), Profiler.buildDetail(three));
        Assertions.assertEquals(Profiler.buildDetail(one), Profiler.buildDetail(four));
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
