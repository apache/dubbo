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
package org.apache.dubbo.common.threadlocal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.threadlocal.InternalThreadLocalMap.UNSET;


public class InternalThreadLocalMapTest {

    @BeforeEach
    public void clear() {
        InternalThreadLocalMap.remove();
    }

    @AfterEach
    public void remove(){
        InternalThreadLocalMap.remove();
    }

    @Test
    public void getAndRemoveTest() throws InterruptedException {
        // slowGet
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
        Assertions.assertNotNull(threadLocalMap);
        InternalThreadLocalMap.remove();

        InternalThread internalThread = new InternalThread(() -> {
            // fastGet
            InternalThreadLocalMap internalThreadLocalMap = InternalThreadLocalMap.get();
            Assertions.assertNotNull(internalThreadLocalMap);
            InternalThreadLocalMap.remove();
        });

        internalThread.join(3000);
    }

    @Test
    public void nextVariableIndexTest() throws NoSuchFieldException, IllegalAccessException {
        Field next_index = InternalThreadLocalMap.class.getDeclaredField("NEXT_INDEX");
        next_index.setAccessible(true);

        AtomicInteger index = (AtomicInteger) next_index.get(null);
        index.set(Integer.MAX_VALUE);
        InternalThreadLocalMap.nextVariableIndex();

        Assertions.assertThrows(IllegalStateException.class, () -> {
            InternalThreadLocalMap.nextVariableIndex();
        });
        next_index.setAccessible(false);
    }

    @Test
    public void constructorTest() throws Exception {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
        Field indexedVariables = threadLocalMap.getClass().getDeclaredField("indexedVariables");
        indexedVariables.setAccessible(true);

        Object[] obs = (Object[]) indexedVariables.get(threadLocalMap);
        Assertions.assertEquals(obs.length, 32);
        for (Object o : obs) {
            Assertions.assertEquals(o, UNSET);
        }

        indexedVariables.setAccessible(false);
    }

    @Test
    public void sizeTest() {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
        Assertions.assertEquals(-1, threadLocalMap.size());
        int count = 5;
        for (int i = 1; i <= 5; i++) {
            threadLocalMap.setIndexedVariable(i, i);
        }
        Assertions.assertEquals(count - 1, threadLocalMap.size());
    }

    @Test
    public void setAndGetIndexedVariableTest() {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();

        String str1 = "java";
        boolean status1 = threadLocalMap.setIndexedVariable(1, str1);
        Assertions.assertTrue(status1);
        Assertions.assertEquals(str1, threadLocalMap.indexedVariable(1));


        String str2 = "dubbo";
        boolean status2 = threadLocalMap.setIndexedVariable(1, str2);
        Assertions.assertFalse(status2);
        Assertions.assertEquals(str2, threadLocalMap.indexedVariable(1));

        // Fill with 31 elements (the fist element in `indexedVariables` is a set to keep all the InternalThreadLocal to remove)
        for (int i = 0; i < 32; i++) {
            threadLocalMap.setIndexedVariable(i, i);
        }
        // 32 - 1
        Assertions.assertEquals(31, threadLocalMap.size());
        // Expansion occurs
        threadLocalMap.setIndexedVariable(33, "expandIndexedVariableTableAndSet");
        // Fill with 31 elements
        for (int i = 34; i < 65; i++) {
            threadLocalMap.setIndexedVariable(i, i);
        }
        // 64 -1
        Assertions.assertEquals(63, threadLocalMap.size());

    }
}
