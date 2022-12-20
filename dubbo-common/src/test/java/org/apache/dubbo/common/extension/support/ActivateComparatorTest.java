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
package org.apache.dubbo.common.extension.support;

import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ActivateComparatorTest {

    private ActivateComparator activateComparator;

    @BeforeEach
    public void setup() {
        activateComparator = new ActivateComparator(ApplicationModel.defaultModel().getExtensionDirector());
    }

    @Test
    void testActivateComparator(){
        Filter1 f1 = new Filter1();
        Filter2 f2 = new Filter2();
        Filter3 f3 = new Filter3();
        Filter4 f4 = new Filter4();
        OldFilter5 f5 = new OldFilter5();
        List<Class<?>> filters = new ArrayList<>();
        filters.add(f1.getClass());
        filters.add(f2.getClass());
        filters.add(f3.getClass());
        filters.add(f4.getClass());
        filters.add(f5.getClass());

        Collections.sort(filters, activateComparator);

        Assertions.assertEquals(f4.getClass(), filters.get(0));
        Assertions.assertEquals(f5.getClass(), filters.get(1));
        Assertions.assertEquals(f3.getClass(), filters.get(2));
        Assertions.assertEquals(f2.getClass(), filters.get(3));
        Assertions.assertEquals(f1.getClass(), filters.get(4));
    }

    @Test
    void testFilterOrder() {
        Order0Filter1 order0Filter1 = new Order0Filter1();
        Order0Filter2 order0Filter2 = new Order0Filter2();

        List<Class<?>> filters = null;

        {
            filters = new ArrayList<>();
            filters.add(order0Filter1.getClass());
            filters.add(order0Filter2.getClass());
            filters.sort(activateComparator);
            Assertions.assertEquals(order0Filter1.getClass(), filters.get(0));
            Assertions.assertEquals(order0Filter2.getClass(), filters.get(1));
        }

        {
            filters = new ArrayList<>();
            filters.add(order0Filter2.getClass());
            filters.add(order0Filter1.getClass());
            filters.sort(activateComparator);
            Assertions.assertEquals(order0Filter1.getClass(), filters.get(0));
            Assertions.assertEquals(order0Filter2.getClass(), filters.get(1));
        }
    }
}