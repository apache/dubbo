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

import static org.apache.dubbo.common.utils.MemberUtils.isPrivate;
import static org.apache.dubbo.common.utils.MemberUtils.isPublic;
import static org.apache.dubbo.common.utils.MemberUtils.isStatic;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MemberUtils} Test
 *
 * @since 2.7.6
 */
public class MemberUtilsTest {

    @Test
    public void testIsStatic() throws NoSuchMethodException {

        assertFalse(isStatic(getClass().getMethod("testIsStatic")));
        assertTrue(isStatic(getClass().getMethod("staticMethod")));
        assertTrue(isPrivate(getClass().getDeclaredMethod("privateMethod")));
        assertTrue(isPublic(getClass().getMethod("publicMethod")));
    }

    public static void staticMethod() {

    }

    private void privateMethod() {

    }

    public void publicMethod() {

    }
}
