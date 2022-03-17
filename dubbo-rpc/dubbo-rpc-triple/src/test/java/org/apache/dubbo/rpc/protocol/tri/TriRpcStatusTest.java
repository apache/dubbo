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

import org.apache.dubbo.rpc.TriRpcStatus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TriRpcStatusTest {

    @Test
    public void fromMessage() {
        String origin = "haha test 😊";
        final TriRpcStatus status = TriRpcStatus.INTERNAL
            .withDescription(origin);
        final String encoded = TriRpcStatus.encodeMessage(origin);
        Assertions.assertNotEquals(origin, encoded);
        final String decoded = TriRpcStatus.decodeMessage(encoded);
        Assertions.assertEquals(origin, decoded);
    }

    @Test
    public void toMessage() {
        String content = "\t\ntest with whitespace\r\nand Unicode BMP ☺ and non-BMP 😈\t\n";
        final TriRpcStatus status = TriRpcStatus.INTERNAL
            .withDescription(content);
        Assertions.assertEquals(content, status.toMessage());
    }
}
