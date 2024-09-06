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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.nested.TripleConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TripleBuilderTest {

    @Test
    void maxBodySize() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.maxBodySize(10240);
        Assertions.assertEquals(10240, builder.build().getMaxBodySize());
    }

    @Test
    void maxResponseBodySize() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.maxResponseBodySize(8192);
        Assertions.assertEquals(8192, builder.build().getMaxResponseBodySize());
    }

    @Test
    void maxChunkSize() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.maxChunkSize(2048);
        Assertions.assertEquals(2048, builder.build().getMaxChunkSize());
    }

    @Test
    void maxHeaderSize() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.maxHeaderSize(40960);
        Assertions.assertEquals(40960, builder.build().getMaxHeaderSize());
    }

    @Test
    void maxInitialLineLength() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.maxInitialLineLength(Integer.MAX_VALUE);
        Assertions.assertEquals(Integer.MAX_VALUE, builder.build().getMaxInitialLineLength());
    }

    @Test
    void initialBufferSize() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.initialBufferSize(3000);
        Assertions.assertEquals(3000, builder.build().getInitialBufferSize());
    }

    @Test
    void headerTableSize() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.headerTableSize(1000);
        Assertions.assertEquals(1000, builder.build().getHeaderTableSize());
    }

    @Test
    void enablePush() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.enablePush(false);
        Assertions.assertFalse(builder.build().getEnablePush());
    }

    @Test
    void maxConcurrentStreams() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.maxConcurrentStreams(3000);
        Assertions.assertEquals(3000, builder.build().getMaxConcurrentStreams());
    }

    @Test
    void initialWindowSize() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.initialWindowSize(10240);
        Assertions.assertEquals(10240, builder.build().getInitialWindowSize());
    }

    @Test
    void maxFrameSize() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.maxFrameSize(4096);
        Assertions.assertEquals(4096, builder.build().getMaxFrameSize());
    }

    @Test
    void maxHeaderListSize() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.maxHeaderListSize(2000);
        Assertions.assertEquals(2000, builder.build().getMaxHeaderListSize());
    }

    @Test
    void build() {
        TripleBuilder builder = TripleBuilder.newBuilder();
        builder.maxBodySize(2048)
                .maxResponseBodySize(3072)
                .maxChunkSize(10240)
                .maxHeaderSize(400)
                .maxInitialLineLength(100)
                .initialBufferSize(8192)
                .headerTableSize(300)
                .enablePush(true)
                .maxConcurrentStreams(Integer.MAX_VALUE)
                .initialWindowSize(4096)
                .maxFrameSize(1024)
                .maxHeaderListSize(500);

        TripleConfig config = builder.build();
        TripleConfig config2 = builder.build();

        Assertions.assertEquals(2048, config.getMaxBodySize());
        Assertions.assertEquals(3072, config.getMaxResponseBodySize());
        Assertions.assertEquals(10240, config.getMaxChunkSize());
        Assertions.assertEquals(400, config.getMaxHeaderSize());
        Assertions.assertEquals(100, config.getMaxInitialLineLength());
        Assertions.assertEquals(8192, config.getInitialBufferSize());
        Assertions.assertEquals(300, config.getHeaderTableSize());
        Assertions.assertTrue(config.getEnablePush());
        Assertions.assertEquals(Integer.MAX_VALUE, config.getMaxConcurrentStreams());
        Assertions.assertEquals(4096, config.getInitialWindowSize());
        Assertions.assertEquals(1024, config.getMaxFrameSize());
        Assertions.assertEquals(500, config.getMaxHeaderListSize());
        Assertions.assertNotSame(config, config2);
    }
}
