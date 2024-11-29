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
package org.apache.dubbo.config.nested;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TripleConfigTest {
    @Test
    void testDefaultTriple() {
        TripleConfig tripleConfig = new TripleConfig();

        // check default value
        Assertions.assertEquals(1 << 23, tripleConfig.getMaxChunkSizeOrDefault());
        Assertions.assertEquals(8192, tripleConfig.getMaxHeaderSizeOrDefault());
        Assertions.assertEquals(4096, tripleConfig.getMaxInitialLineLengthOrDefault());
        Assertions.assertEquals(16384, tripleConfig.getInitialBufferSizeOrDefault());
        Assertions.assertEquals(4096, tripleConfig.getHeaderTableSizeOrDefault());
        Assertions.assertFalse(tripleConfig.getEnablePushOrDefault());
        Assertions.assertEquals(Integer.MAX_VALUE, tripleConfig.getMaxConcurrentStreamsOrDefault());
        Assertions.assertEquals(1 << 23, tripleConfig.getInitialWindowSizeOrDefault());
        Assertions.assertEquals(1 << 16, tripleConfig.getConnectionInitialWindowSizeOrDefault());
        Assertions.assertEquals(1 << 23, tripleConfig.getMaxFrameSizeOrDefault());
        Assertions.assertEquals(1 << 15, tripleConfig.getMaxHeaderListSizeOrDefault());
    }
}
