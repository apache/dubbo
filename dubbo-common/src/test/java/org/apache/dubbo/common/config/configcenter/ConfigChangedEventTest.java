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
package org.apache.dubbo.common.config.configcenter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link ConfigChangedEvent} Test
 *
 * @since 2.7.5
 */
class ConfigChangedEventTest {

    private static final String KEY = "k";

    private static final String GROUP = "g";

    private static final String CONTENT = "c";

    @Test
    void testGetter() {

        ConfigChangedEvent event = new ConfigChangedEvent(KEY, GROUP, CONTENT);

        assertEquals(KEY, event.getKey());
        assertEquals(GROUP, event.getGroup());
        assertEquals(CONTENT, event.getContent());
        assertEquals(ConfigChangeType.MODIFIED, event.getChangeType());
        assertEquals("k,g", event.getSource());

        event = new ConfigChangedEvent(KEY, GROUP, CONTENT, ConfigChangeType.ADDED);

        assertEquals(KEY, event.getKey());
        assertEquals(GROUP, event.getGroup());
        assertEquals(CONTENT, event.getContent());
        assertEquals(ConfigChangeType.ADDED, event.getChangeType());
        assertEquals("k,g", event.getSource());
    }

    @Test
    void testEqualsAndHashCode() {
        for (ConfigChangeType type : ConfigChangeType.values()) {
            assertEquals(new ConfigChangedEvent(KEY, GROUP, CONTENT, type), new ConfigChangedEvent(KEY, GROUP, CONTENT, type));
            assertEquals(new ConfigChangedEvent(KEY, GROUP, CONTENT, type).hashCode(), new ConfigChangedEvent(KEY, GROUP, CONTENT, type).hashCode());
            assertEquals(new ConfigChangedEvent(KEY, GROUP, CONTENT, type).toString(), new ConfigChangedEvent(KEY, GROUP, CONTENT, type).toString());
        }
    }

    @Test
    void testToString() {
        ConfigChangedEvent event = new ConfigChangedEvent(KEY, GROUP, CONTENT);
        assertNotNull(event.toString());
    }
}