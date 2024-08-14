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
package org.apache.dubbo.rpc.protocol.dubbo.pu;

import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DubboDetectorTest {
    @Test
    void testDetect_Recognized() {
        DubboDetector detector = new DubboDetector();
        ChannelBuffer in = ChannelBuffers.wrappedBuffer(new byte[] {(byte) 0xda, (byte) 0xbb});
        assertEquals(DubboDetector.Result.RECOGNIZED, detector.detect(in));
    }

    @Test
    void testDetect_Unrecognized() {
        DubboDetector detector = new DubboDetector();
        ChannelBuffer in = ChannelBuffers.wrappedBuffer(new byte[] {(byte) 0x00, (byte) 0x00});
        assertEquals(DubboDetector.Result.UNRECOGNIZED, detector.detect(in));
    }

    @Test
    void testDetect_NeedMoreData() {
        DubboDetector detector = new DubboDetector();
        ChannelBuffer in = ChannelBuffers.wrappedBuffer(new byte[] {(byte) 0xda});
        assertEquals(DubboDetector.Result.NEED_MORE_DATA, detector.detect(in));
    }
}
