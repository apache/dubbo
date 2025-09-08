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

import org.apache.dubbo.common.store.DataStoreUpdateListener;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleDataStoreTest {
    private SimpleDataStore dataStore = new SimpleDataStore();

    @Test
    void testPutGet() throws Exception {
        assertNull(dataStore.get("xxx", "yyy"));

        dataStore.put("name", "key", "1");
        assertEquals("1", dataStore.get("name", "key"));

        assertNull(dataStore.get("xxx", "yyy"));
    }

    @Test
    void testRemove() throws Exception {
        dataStore.remove("xxx", "yyy");

        dataStore.put("name", "key", "1");
        dataStore.remove("name", "key");
        assertNull(dataStore.get("name", "key"));
    }

    @Test
    void testGetComponent() throws Exception {
        Map<String, Object> map = dataStore.get("component");
        assertTrue(map != null && map.isEmpty());
        dataStore.put("component", "key", "value");
        map = dataStore.get("component");
        assertTrue(map != null && map.size() == 1);
        dataStore.remove("component", "key");
        assertNotEquals(map, dataStore.get("component"));
    }

    @Test
    void testNotify() {
        DataStoreUpdateListener listener = Mockito.mock(DataStoreUpdateListener.class);
        dataStore.addListener(listener);

        ArgumentCaptor<String> componentNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);

        dataStore.put("name", "key", "1");
        Mockito.verify(listener).onUpdate(componentNameCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());
        assertEquals("name", componentNameCaptor.getValue());
        assertEquals("key", keyCaptor.getValue());
        assertEquals("1", valueCaptor.getValue());

        dataStore.remove("name", "key");
        Mockito.verify(listener, Mockito.times(2))
                .onUpdate(componentNameCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());
        assertEquals("name", componentNameCaptor.getValue());
        assertEquals("key", keyCaptor.getValue());
        assertNull(valueCaptor.getValue());

        dataStore.remove("name2", "key");
        Mockito.verify(listener, Mockito.times(0)).onUpdate("name2", "key", null);
    }
}
