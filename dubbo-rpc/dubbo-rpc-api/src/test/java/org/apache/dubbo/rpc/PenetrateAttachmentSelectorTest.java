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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@link PenetrateAttachmentSelector}
 */
class PenetrateAttachmentSelectorTest {

    @Test
    void test() {
        ExtensionLoader<PenetrateAttachmentSelector> selectorExtensionLoader = ApplicationModel.defaultModel().getExtensionLoader(PenetrateAttachmentSelector.class);
        Set<String> supportedSelectors = selectorExtensionLoader.getSupportedExtensions();
        Map<String, Object> allSelected = new HashMap<>();
        if (CollectionUtils.isNotEmpty(supportedSelectors)) {
            for (String supportedSelector : supportedSelectors) {
                Map<String, Object> selected = selectorExtensionLoader.getExtension(supportedSelector).select(null, null, null);
                allSelected.putAll(selected);
            }
        }
        Assertions.assertEquals(allSelected.get("testKey"), "testVal");
    }

    @Test
    public void testSelectReverse() {
        ExtensionLoader<PenetrateAttachmentSelector> selectorExtensionLoader = ApplicationModel.defaultModel().getExtensionLoader(PenetrateAttachmentSelector.class);
        Set<String> supportedSelectors = selectorExtensionLoader.getSupportedExtensions();
        Map<String, Object> allSelected = new HashMap<>();
        if (CollectionUtils.isNotEmpty(supportedSelectors)) {
            for (String supportedSelector : supportedSelectors) {
                Map<String, Object> selected = selectorExtensionLoader.getExtension(supportedSelector).selectReverse(null, null, null);
                allSelected.putAll(selected);
            }
        }
        Assertions.assertEquals(allSelected.get("reverseKey"), "reverseVal");
    }
}
