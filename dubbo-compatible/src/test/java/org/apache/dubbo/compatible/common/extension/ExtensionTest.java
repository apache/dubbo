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

package org.apache.dubbo.compatible.common.extension;

import org.apache.dubbo.common.extension.ExtensionFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ExtensionTest {

    @Test
    public void testExtensionFactory() {
        try {
            ExtensionFactory factory = ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getExtension("myfactory");
            Assert.assertTrue(factory instanceof ExtensionFactory);
            Assert.assertTrue(factory instanceof com.alibaba.dubbo.common.extension.ExtensionFactory);
            Assert.assertTrue(factory instanceof MyExtensionFactory);

            ExtensionFactory spring = ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getExtension("spring");
            Assert.assertTrue(spring instanceof ExtensionFactory);
            Assert.assertFalse(spring instanceof com.alibaba.dubbo.common.extension.ExtensionFactory);
        } catch (IllegalArgumentException expected) {
            fail();
        }
    }
}
