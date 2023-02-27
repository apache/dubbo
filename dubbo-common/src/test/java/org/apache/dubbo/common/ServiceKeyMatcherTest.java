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

class ServiceKeyMatcherTest {

    @Test
    void testInterface() {
        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, null),
            new ServiceKey(null, null, null)
        ));
        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey("DemoService", null, null),
            new ServiceKey(null, null, null)
        ));
        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, null),
            new ServiceKey("DemoService", null, null)
        ));


        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey("*", null, null),
            new ServiceKey("DemoService", null, null)
        ));
        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey("*", null, null),
            new ServiceKey(null, null, null)
        ));
    }

    @Test
    void testVersion() {
        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "1.0.0", null),
            new ServiceKey(null, "1.0.0", null)
        ));
        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, null),
            new ServiceKey(null, null, null)
        ));
        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "1.0.0", null),
            new ServiceKey(null, null, null)
        ));
        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, null),
            new ServiceKey(null, "1.0.0", null)
        ));

        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "1.0.0,1.0.1", null),
            new ServiceKey(null, "1.0.0", null)
        ));
        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "1.0.0,1.0.1", null),
            new ServiceKey(null, "1.0.2", null)
        ));


        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "1.0.0,1.0.1", null),
            new ServiceKey(null, null, null)
        ));

        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, ",1.0.0,1.0.1", null),
            new ServiceKey(null, null, null)
        ));
        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, ",1.0.0,1.0.1", null),
            new ServiceKey(null, "", null)
        ));

        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "1.0.0,,1.0.1", null),
            new ServiceKey(null, null, null)
        ));
        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "1.0.0,,1.0.1", null),
            new ServiceKey(null, "", null)
        ));

        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "1.0.0,1.0.1,", null),
            new ServiceKey(null, null, null)
        ));
        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "1.0.0,1.0.1,", null),
            new ServiceKey(null, "", null)
        ));

        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "1.0.0,1.0.1", null),
            new ServiceKey(null, null, null)
        ));
        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "1.0.0,1.0.1", null),
            new ServiceKey(null, "", null)
        ));

        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, ",1.0.0,1.0.1", null),
            new ServiceKey(null, "1.0.2", null)
        ));
        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, ",1.0.0,1.0.1", null),
            new ServiceKey(null, "1.0.2", null)
        ));


        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "*", null),
            new ServiceKey(null, null, null)
        ));
        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "*", null),
            new ServiceKey(null, "", null)
        ));
        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, "*", null),
            new ServiceKey(null, "1.0.0", null)
        ));
    }

    @Test
    void testGroup() {
        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "group1"),
            new ServiceKey(null, null, "group1")
        ));
        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "group1"),
            new ServiceKey(null, null, null)
        ));
        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, null),
            new ServiceKey(null, null, "group1")
        ));

        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "group1, group2"),
            new ServiceKey(null, null, "group1")
        ));

        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "group2, group3"),
            new ServiceKey(null, null, "group1")
        ));

        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "group2, group3"),
            new ServiceKey(null, null, null)
        ));

        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "group2, group3"),
            new ServiceKey(null, null, "")
        ));

        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, ",group2"),
            new ServiceKey(null, null, "")
        ));

        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "group2,"),
            new ServiceKey(null, null, "")
        ));

        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "group2, ,group3"),
            new ServiceKey(null, null, "")
        ));

        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, ",group2"),
            new ServiceKey(null, null, "group1")
        ));

        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "group2,"),
            new ServiceKey(null, null, "group1")
        ));

        Assertions.assertFalse(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "group2, ,group3"),
            new ServiceKey(null, null, "group1")
        ));

        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "*"),
            new ServiceKey(null, null, "")
        ));

        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "*"),
            new ServiceKey(null, null, null)
        ));

        Assertions.assertTrue(ServiceKey.Matcher.isMatch(
            new ServiceKey(null, null, "*"),
            new ServiceKey(null, null, "group1")
        ));
    }
}