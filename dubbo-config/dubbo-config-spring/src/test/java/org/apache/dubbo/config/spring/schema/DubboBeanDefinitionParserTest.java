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
package org.apache.dubbo.config.spring.schema;

import org.apache.dubbo.config.ProtocolConfig;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DubboBeanDefinitionParserTest {

    // In the Turkish locale "I".toLowerCase() is the dotless "ı", which turned the
    // property behind setIothreads() into "ıothreads", so the iothreads attribute was ignored.
    @Test
    void testGetPropertyMapUnderTurkishLocale() throws Exception {
        Method getPropertyMap =
                DubboBeanDefinitionParser.class.getDeclaredMethod("getPropertyMap", Class.class, Map.class);
        getPropertyMap.setAccessible(true);
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            Map<String, Class> properties = new HashMap<>();
            getPropertyMap.invoke(null, ProtocolConfig.class, properties);
            assertEquals(
                    Integer.class,
                    properties.get("iothreads"),
                    properties.keySet().toString());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
