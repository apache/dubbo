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
package org.apache.dubbo.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DatePatternTest {
    @Test
    public void testParse() throws Exception {
        Assertions.assertNotNull(DatePattern.parse("20200101"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01T00:00:00"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01T00:00:00+0800"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01T00:00:00-0800"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01T00:00:00.000"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01T00:00:00.000+0800"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01T00:00:00.000-0800"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01T00:00:00Z"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01 00:00:00"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01 00:00:00.000"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01 00:00:00+0800"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01 00:00:00-0800"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01 00:00:00.000+0800"));
        Assertions.assertNotNull(DatePattern.parse("2020-01-01 00:00:00.000-0800"));
        Assertions.assertNotNull(DatePattern.parse("2020-01"));

        Assertions.assertThrows(IllegalStateException.class, () -> DatePattern.parse("error string"));
    }
}
