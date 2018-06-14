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
package org.apache.dubbo.common.store.support;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SimpleDataStoreTest {
    private SimpleDataStore dataStore = new SimpleDataStore();

    @Test
    public void testPutGet() throws Exception {
        assertNull(dataStore.get("xxx", "yyy"));

        dataStore.put("name", "key", "1");
        assertEquals("1", dataStore.get("name", "key"));

        assertNull(dataStore.get("xxx", "yyy"));
    }

    @Test
    public void testRemove() throws Exception {
        dataStore.remove("xxx", "yyy");

        dataStore.put("name", "key", "1");
        dataStore.remove("name", "key");
        assertNull(dataStore.get("name", "key"));
    }

    @Test
    public void testGetComponent() throws Exception {
        Map<String, Object> map = dataStore.get("component");
        assertTrue(map != null && map.isEmpty());
        dataStore.put("component", "key", "value");
        map = dataStore.get("component");
        assertTrue(map != null && map.size() == 1);
        dataStore.remove("component", "key");
        assertNotEquals(map, dataStore.get("component"));
    }
}
