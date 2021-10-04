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
package org.apache.dubbo.metadata;

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.metadata.MetadataServiceType.COMPOSITE;
import static org.apache.dubbo.metadata.MetadataServiceType.DEFAULT;
import static org.apache.dubbo.metadata.MetadataServiceType.REMOTE;
import static org.apache.dubbo.metadata.MetadataServiceType.getOrDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link MetadataServiceType} Test-Cases
 *
 * @since 2.7.8
 */
public class MetadataServiceTypeTest {

    @Test
    public void testGetValue() {
        assertEquals("local", DEFAULT.getValue());
        assertEquals("remote", REMOTE.getValue());
        assertEquals("composite", COMPOSITE.getValue());
    }

    @Test
    public void testGetOrDefault() {
        assertEquals(DEFAULT, getOrDefault("local"));
        assertEquals(REMOTE, getOrDefault("remote"));
        assertEquals(COMPOSITE, getOrDefault("composite"));
        assertEquals(DEFAULT, getOrDefault("others"));
    }
}
