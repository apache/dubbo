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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class ExceptionUtilsTest {

    private IllegalStateException exception = new IllegalStateException("Exception0");
    @Test
    void getStackTrace() {
        Assertions.assertTrue(ExceptionUtils.getStackTrace(exception).contains("Exception0"));
    }

    @Test
    void getStackFrameString() {
        String str = ExceptionUtils.getStackFrameString(
            Arrays.stream(exception.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.toList()));
        Assertions.assertTrue(str.contains("ExceptionUtilsTest"));
    }

    @Test
    void getStackFrames() {
        StackTraceElement[] traces = exception.getStackTrace();
        List<String> frames = Arrays.stream(traces)
            .map(StackTraceElement::toString)
            .collect(Collectors.toList());
        String str = ExceptionUtils.getStackFrameString(frames);
        List<String> stackFrames = Arrays.stream(ExceptionUtils.getStackFrames(str))
                .collect(Collectors.toList());
        Assertions.assertEquals(frames,stackFrames);
    }

    @Test
    void testGetStackFrames() {
        String[] stackFrames = ExceptionUtils.getStackFrames(exception);
        Assertions.assertNotEquals(0,stackFrames.length);
    }

    @Test
    void getStackFrameList() {
        List<String> stackFrameList = ExceptionUtils.getStackFrameList(exception, 10);
        Assertions.assertEquals(10,stackFrameList.size());
    }

    @Test
    void testGetStackFrameList() {
        List<String> stackFrameList = ExceptionUtils.getStackFrameList(exception);
        Assertions.assertNotEquals(10,stackFrameList.size());
    }
}
