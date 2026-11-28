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
package org.apache.dubbo.rpc.protocol.tri.compressor;

import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DeCompressorTest {

    @Test
    void getCompressor() {
        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            Assertions.assertNull(DeCompressor.getCompressor(frameworkModel, null));
            Assertions.assertSame(DeCompressor.NONE, DeCompressor.getCompressor(frameworkModel, "identity"));
            Assertions.assertEquals(
                    "gzip", DeCompressor.getCompressor(frameworkModel, "gzip").getMessageEncoding());
        } finally {
            frameworkModel.destroy();
        }
    }

    @Test
    void getCompressorReturnsNullForUnsupportedEncoding() {
        // the caller answers null with UNIMPLEMENTED, as gRPC requires for an unsupported grpc-encoding
        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            Assertions.assertNull(
                    Assertions.assertDoesNotThrow(() -> DeCompressor.getCompressor(frameworkModel, "x-test")));
            Assertions.assertNull(Assertions.assertDoesNotThrow(() -> DeCompressor.getCompressor(frameworkModel, "")));
        } finally {
            frameworkModel.destroy();
        }
    }
}
