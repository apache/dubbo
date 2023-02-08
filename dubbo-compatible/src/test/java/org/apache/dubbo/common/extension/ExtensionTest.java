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

package org.apache.dubbo.common.extension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class ExtensionTest {

    @Test
    void testExtensionFactory() {
        try {
            ExtensionInjector myfactory = ExtensionLoader.getExtensionLoader(ExtensionInjector.class).getExtension("myfactory");
            Assertions.assertTrue(myfactory instanceof ExtensionInjector);
            Assertions.assertTrue(myfactory instanceof ExtensionFactory);
            Assertions.assertTrue(myfactory instanceof com.alibaba.dubbo.common.extension.ExtensionFactory);
            Assertions.assertTrue(myfactory instanceof MyExtensionFactory);

            ExtensionInjector spring = ExtensionLoader.getExtensionLoader(ExtensionInjector.class).getExtension("spring");
            Assertions.assertTrue(spring instanceof ExtensionInjector);
            Assertions.assertFalse(spring instanceof ExtensionFactory);
            Assertions.assertFalse(spring instanceof com.alibaba.dubbo.common.extension.ExtensionFactory);
        } catch (IllegalArgumentException expected) {
            fail();
        }
    }
}
