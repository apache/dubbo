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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.ExtensionLoader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CompressorTest {

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
        System.out.println("current compressor is " + compressorName);
        Compressor compressor = ExtensionLoader.getExtensionLoader(Compressor.class).getExtension(compressorName);

        byte[] compressedByteArr = compressor.compress(TEST_STR.getBytes());
        System.out.println("compressed byte length：" + compressedByteArr.length);

        byte[] decompressedByteArr = compressor.decompress(compressedByteArr);
        System.out.println("decompressed byte length：" + decompressedByteArr.length);
        Assertions.assertEquals(new String(decompressedByteArr), TEST_STR);
    }

}
