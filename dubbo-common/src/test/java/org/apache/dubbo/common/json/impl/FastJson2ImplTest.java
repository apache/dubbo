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
package org.apache.dubbo.common.json.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Type;

import static org.mockito.Answers.CALLS_REAL_METHODS;

class FastJson2ImplTest {
    private static MockedStatic<JSON> fastjson2Mock;

    @BeforeAll
    static void setup() {
        fastjson2Mock = Mockito.mockStatic(JSON.class, CALLS_REAL_METHODS);
    }

    @AfterAll
    static void teardown() {
        fastjson2Mock.close();
    }

    @Test
    void testSupported() {
        Assertions.assertTrue(new FastJson2Impl().isSupport());

        fastjson2Mock.when(() -> JSON.toJSONString(Mockito.any(), (JSONWriter.Feature) Mockito.any())).thenThrow(new RuntimeException());
        Assertions.assertFalse(new FastJson2Impl().isSupport());
        fastjson2Mock.reset();

        fastjson2Mock.when(() -> JSON.toJSONString(Mockito.any(), (JSONWriter.Feature) Mockito.any())).thenReturn(null);
        Assertions.assertFalse(new FastJson2Impl().isSupport());
        fastjson2Mock.reset();

        fastjson2Mock.when(() -> JSON.parseObject((String) Mockito.any(), (Type) Mockito.any())).thenReturn(null);
        Assertions.assertFalse(new FastJson2Impl().isSupport());
        fastjson2Mock.reset();

        fastjson2Mock.when(() -> JSON.parseArray(Mockito.any(), (Class) Mockito.any())).thenReturn(null);
        Assertions.assertFalse(new FastJson2Impl().isSupport());
        fastjson2Mock.reset();
    }
}
