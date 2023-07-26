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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrayUtilsTest {

    @Test
    void isEmpty() {
        assertTrue(ArrayUtils.isEmpty(null));
        assertTrue(ArrayUtils.isEmpty(new Object[0]));
        assertFalse(ArrayUtils.isEmpty(new Object[]{"abc"}));
    }

    @Test
    void isNotEmpty() {
        assertFalse(ArrayUtils.isNotEmpty(null));
        assertFalse(ArrayUtils.isNotEmpty(new Object[0]));
        assertTrue(ArrayUtils.isNotEmpty(new Object[]{"abc"}));
    }

}
