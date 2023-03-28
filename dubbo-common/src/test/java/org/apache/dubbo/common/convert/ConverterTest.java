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
package org.apache.dubbo.common.convert;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link Converter} Test-Cases
 *
 * @since 2.7.8
 */
class ConverterTest {

    private ConverterUtil converterUtil;

    @BeforeEach
    public void setup() {
        converterUtil = FrameworkModel.defaultModel().getBeanFactory().getBean(ConverterUtil.class);
    }

    @AfterEach
    public void tearDown() {
        FrameworkModel.destroyAll();
    }

    @Test
    void testGetConverter() {
        getExtensionLoader(Converter.class)
                .getSupportedExtensionInstances()
                .forEach(converter -> {
                    assertSame(converter, converterUtil.getConverter(converter.getSourceType(), converter.getTargetType()));
                });
    }

    @Test
    void testConvertIfPossible() {
        assertEquals(Integer.valueOf(2), converterUtil.convertIfPossible("2", Integer.class));
        assertEquals(Boolean.FALSE, converterUtil.convertIfPossible("false", Boolean.class));
        assertEquals(Double.valueOf(1), converterUtil.convertIfPossible("1", Double.class));
    }

    private <T> ExtensionLoader<T> getExtensionLoader(Class<T> extClass) {
        return ApplicationModel.defaultModel().getDefaultModule().getExtensionLoader(extClass);
    }
}
