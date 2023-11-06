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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.activate.ActivateExt1;
import org.apache.dubbo.common.extension.activate.impl.OldActivateExt1Impl2;
import org.apache.dubbo.common.extension.activate.impl.OldActivateExt1Impl3;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.junit.jupiter.api.Assertions.fail;

class ExtensionTest {

    @Test
    void testExtensionFactory() {
        try {
            ExtensionInjector myfactory =
                    ExtensionLoader.getExtensionLoader(ExtensionInjector.class).getExtension("myfactory");
            Assertions.assertTrue(myfactory instanceof ExtensionInjector);
            Assertions.assertTrue(myfactory instanceof ExtensionFactory);
            Assertions.assertTrue(myfactory instanceof com.alibaba.dubbo.common.extension.ExtensionFactory);
            Assertions.assertTrue(myfactory instanceof MyExtensionFactory);

            ExtensionInjector spring =
                    ExtensionLoader.getExtensionLoader(ExtensionInjector.class).getExtension("spring");
            Assertions.assertTrue(spring instanceof ExtensionInjector);
            Assertions.assertFalse(spring instanceof ExtensionFactory);
            Assertions.assertFalse(spring instanceof com.alibaba.dubbo.common.extension.ExtensionFactory);
        } catch (IllegalArgumentException expected) {
            fail();
        }
    }

    private <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        return ApplicationModel.defaultModel().getExtensionDirector().getExtensionLoader(type);
    }

    @Test
    void testLoadActivateExtension() {
        // test default
        URL url = URL.valueOf("test://localhost/test").addParameter(GROUP_KEY, "old_group");
        List<ActivateExt1> list =
                getExtensionLoader(ActivateExt1.class).getActivateExtension(url, new String[] {}, "old_group");
        Assertions.assertEquals(2, list.size());
        Assertions.assertTrue(list.get(0).getClass() == OldActivateExt1Impl2.class
                || list.get(0).getClass() == OldActivateExt1Impl3.class);
    }
}
