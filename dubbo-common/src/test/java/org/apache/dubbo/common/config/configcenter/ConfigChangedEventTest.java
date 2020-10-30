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
public class ConfigChangedEventTest {

    private static final String key = "k";

    private static final String group = "g";

    private static final String content = "c";

    @Test
    public void testGetter() {

        ConfigChangedEvent event = new ConfigChangedEvent(key, group, content);

        assertEquals(key, event.getKey());
        assertEquals(group, event.getGroup());
        assertEquals(content, event.getContent());
        assertEquals(ConfigChangeType.MODIFIED, event.getChangeType());
        assertEquals("k,g", event.getSource());

        event = new ConfigChangedEvent(key, group, content, ConfigChangeType.ADDED);

        assertEquals(key, event.getKey());
        assertEquals(group, event.getGroup());
        assertEquals(content, event.getContent());
        assertEquals(ConfigChangeType.ADDED, event.getChangeType());
        assertEquals("k,g", event.getSource());
    }

    @Test
    public void testEqualsAndHashCode() {
        for (ConfigChangeType type : ConfigChangeType.values()) {
            assertEquals(new ConfigChangedEvent(key, group, content, type), new ConfigChangedEvent(key, group, content, type));
            assertEquals(new ConfigChangedEvent(key, group, content, type).hashCode(), new ConfigChangedEvent(key, group, content, type).hashCode());
            assertEquals(new ConfigChangedEvent(key, group, content, type).toString(), new ConfigChangedEvent(key, group, content, type).toString());
        }
    }

    @Test
    public void testToString() {
        ConfigChangedEvent event = new ConfigChangedEvent(key, group, content);
        assertNotNull(event.toString());
    }
}
