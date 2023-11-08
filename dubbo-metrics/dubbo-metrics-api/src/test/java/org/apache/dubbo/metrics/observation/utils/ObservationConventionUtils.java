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
package org.apache.dubbo.metrics.observation.utils;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;

import java.lang.reflect.Field;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.mockito.Mockito;

public class ObservationConventionUtils {

    public static Invoker<?> getMockInvokerWithUrl() {
        URL url = URL.valueOf(
                "dubbo://127.0.0.1:12345/com.example.TestService?anyhost=true&application=test&category=providers&dubbo=2.0.2&generic=false&interface=com.example.TestService&methods=testMethod&pid=26716&side=provider&timestamp=1633863896653");
        Invoker<?> invoker = Mockito.mock(Invoker.class);
        Mockito.when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

    public static String getValueForKey(KeyValues keyValues, Object key)
            throws NoSuchFieldException, IllegalAccessException {
        Field f = KeyValues.class.getDeclaredField("keyValues");
        f.setAccessible(true);
        KeyValue[] kv = (KeyValue[]) f.get(keyValues);
        for (KeyValue keyValue : kv) {
            if (keyValue.getKey().equals(key)) {
                return keyValue.getValue();
            }
        }
        return null;
    }
}
