/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import static org.apache.dubbo.common.utils.Assert.notEmptyString;
import static org.apache.dubbo.common.utils.Assert.notNull;

class AssertTest {
    @Test
    void testNotNull1() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> notNull(null, "null object"));
    }

    @Test
    void testNotNull2() {
        Assertions.assertThrows(IllegalStateException.class, () -> notNull(null, new IllegalStateException("null object")));
    }

    @Test
    void testNotNullWhenInputNotNull1() {
        notNull(new Object(), "null object");
    }

    @Test
    void testNotNullWhenInputNotNull2() {
        notNull(new Object(), new IllegalStateException("null object"));
    }

    @Test
    void testNotNullString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> notEmptyString(null, "Message can't be null"));
    }

    @Test
    void testNotEmptyString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> notEmptyString("", "Message can't be null or empty"));
    }

    @Test
    void testNotNullNotEmptyString() {
        notEmptyString("abcd", "Message can'be null or empty");
    }
}