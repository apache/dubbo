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

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GzipTest {

    private static final String TEST_STR;

    static {
        StringBuilder builder = new StringBuilder();
        int charNum = 1000000;
        for (int i = 0; i < charNum; i++) {
            builder.append("a");
        }

        TEST_STR = builder.toString();
    }

    @ValueSource(strings = {"gzip"})
    @ParameterizedTest
    void compression(String compressorName) {
        Compressor compressor = ApplicationModel.defaultModel().getDefaultModule()
            .getExtensionLoader(Compressor.class)
            .getExtension(compressorName);
        String loadByStatic = Compressor.getCompressor(new FrameworkModel(), compressorName)
            .getMessageEncoding();
        Assertions.assertEquals(loadByStatic, compressor.getMessageEncoding());

        byte[] compressedByteArr = compressor.compress(TEST_STR.getBytes());

        DeCompressor deCompressor = ApplicationModel.defaultModel().getDefaultModule()
            .getExtensionLoader(DeCompressor.class)
            .getExtension(compressorName);

        byte[] decompressedByteArr = deCompressor.decompress(compressedByteArr);
        Assertions.assertEquals(new String(decompressedByteArr), TEST_STR);
    }
}
