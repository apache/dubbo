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

import java.util.Arrays;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.apache.dubbo.metadata.URLRevisionResolver.UNKNOWN_REVISION;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link URLRevisionResolver} Test
 *
 * @since 2.7.5
 */
public class URLRevisionResolverTest {

    private static final String URL = "dubbo://192.168.0.102:20881/org.apache.dubbo.metadata.URLRevisionResolverTest";

    private final URLRevisionResolver resolver = URLRevisionResolver.INSTANCE;

    @Test
    public void testResolve() {
        String revision = resolver.resolve(Arrays.<String>asList());
        assertEquals(UNKNOWN_REVISION, revision);

        revision = resolver.resolve((Collection<String>) null);
        assertEquals(UNKNOWN_REVISION, revision);

        revision = resolver.resolve(asList(URL));
        assertEquals("2ca0638f189ce569", revision);
    }
}
