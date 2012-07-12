/*
 * Copyright 1999-2011 Alibaba Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.alibaba.dubbo.common.store.support;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:ding.lid@alibaba-inc.com">ding.lid</a>
 */
public class SimpleDataStoreTest {
    SimpleDataStore dataStore = new SimpleDataStore();

    @Test
    public void testPut_Get() throws Exception {
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
}
