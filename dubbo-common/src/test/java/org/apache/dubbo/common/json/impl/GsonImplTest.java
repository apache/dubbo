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

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class GsonImplTest {

    private static Gson gson = new Gson();
    private static AtomicReference<Gson> gsonReference = new AtomicReference<>();
    private static MockedConstruction<Gson> gsonMock;
    private static AtomicReference<Consumer<Gson>> gsonInit = new AtomicReference<>();

    @BeforeAll
    static void setup() {
        gsonMock = Mockito.mockConstruction(Gson.class,
            (mock, context) -> {
                gsonReference.set(mock);
                Mockito.when(mock.toJson((Object) Mockito.any())).thenAnswer(invocation -> gson.toJson((Object) invocation.getArgument(0)));
                Mockito.when(mock.fromJson(Mockito.anyString(), (Type) Mockito.any())).thenAnswer(invocation -> gson.fromJson((String) invocation.getArgument(0), (Type) invocation.getArgument(1)));
                Consumer<Gson> gsonConsumer = gsonInit.get();
                if (gsonConsumer != null) {
                    gsonConsumer.accept(mock);
                }
            });
    }

    @AfterAll
    static void teardown() {
        gsonMock.close();
    }

    @Test
    void testSupported() {
        Assertions.assertTrue(new GsonImpl().isSupport());

        gsonInit.set(g -> Mockito.when(g.toJson((Object) Mockito.any())).thenThrow(new RuntimeException()));
        Assertions.assertFalse(new GsonImpl().isSupport());
        gsonInit.set(null);

        gsonInit.set(g -> Mockito.when(g.fromJson(Mockito.anyString(), (Type) Mockito.any())).thenThrow(new RuntimeException()));
        Assertions.assertFalse(new GsonImpl().isSupport());
        gsonInit.set(null);

        gsonInit.set(g -> Mockito.when(g.toJson((Object) Mockito.any())).thenReturn(null));
        Assertions.assertFalse(new GsonImpl().isSupport());
        gsonInit.set(null);

        gsonInit.set(g -> Mockito.when(g.fromJson(Mockito.anyString(), (Type) Mockito.any())).thenReturn(null));
        Assertions.assertFalse(new GsonImpl().isSupport());
        gsonInit.set(null);

        gsonInit.set(g -> Mockito.when(g.fromJson(Mockito.eq("[\"json\"]"), (Type) Mockito.any())).thenReturn(null));
        Assertions.assertFalse(new GsonImpl().isSupport());
        gsonInit.set(null);
    }
}
