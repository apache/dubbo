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
package org.apache.dubbo.registry.client.metadata;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.apache.dubbo.registry.client.metadata.URLRevisionResolver.NO_REVISION;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link URLRevisionResolver} Test
 *
 * @since 2.7.5
 */
public class URLRevisionResolverTest {

    private static final String URL = "dubbo://192.168.0.102:20881/org.apache.dubbo.registry.client.metadata.URLRevisionResolverTest";

    private final URLRevisionResolver resolver = new URLRevisionResolver();

    @Test
    public void testResolve() {
        String revision = resolver.resolve(asList());
        assertEquals(NO_REVISION, revision);

        revision = resolver.resolve(null);
        assertEquals(NO_REVISION, revision);

        revision = resolver.resolve(asList(URL));
        assertEquals("7960327984321481979", revision);
    }
}
