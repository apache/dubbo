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
package org.apache.dubbo.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProtocolServiceKeyMatcherTest {

    @Test
    void testProtocol() {
        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo"),
            new ProtocolServiceKey(null, null, null, "dubbo")
        ));

        Assertions.assertFalse(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo"),
            new ProtocolServiceKey(null, null, null, null)
        ));

        Assertions.assertFalse(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo"),
            new ProtocolServiceKey("DemoService", null, null, "dubbo")
        ));

        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, null),
            new ProtocolServiceKey(null, null, null, "dubbo")
        ));
        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, ""),
            new ProtocolServiceKey(null, null, null, "dubbo")
        ));
        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "*"),
            new ProtocolServiceKey(null, null, null, "dubbo")
        ));

        Assertions.assertFalse(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo1,dubbo2"),
            new ProtocolServiceKey(null, null, null, "dubbo")
        ));
        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo1,dubbo2"),
            new ProtocolServiceKey(null, null, null, "dubbo1")
        ));
        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo1,dubbo2"),
            new ProtocolServiceKey(null, null, null, "dubbo2")
        ));

        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo1,,dubbo2"),
            new ProtocolServiceKey(null, null, null, null)
        ));
        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo1,,dubbo2"),
            new ProtocolServiceKey(null, null, null, "")
        ));

        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, ",dubbo1,dubbo2"),
            new ProtocolServiceKey(null, null, null, null)
        ));
        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, ",dubbo1,dubbo2"),
            new ProtocolServiceKey(null, null, null, "")
        ));

        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo1,dubbo2,"),
            new ProtocolServiceKey(null, null, null, null)
        ));
        Assertions.assertTrue(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo1,dubbo2,"),
            new ProtocolServiceKey(null, null, null, "")
        ));

        Assertions.assertFalse(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo1,,dubbo2"),
            new ProtocolServiceKey(null, null, null, "dubbo")
        ));
        Assertions.assertFalse(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, ",dubbo1,dubbo2"),
            new ProtocolServiceKey(null, null, null, "dubbo")
        ));
        Assertions.assertFalse(ProtocolServiceKey.Matcher.isMatch(
            new ProtocolServiceKey(null, null, null, "dubbo1,dubbo2,"),
            new ProtocolServiceKey(null, null, null, "dubbo")
        ));
    }
}