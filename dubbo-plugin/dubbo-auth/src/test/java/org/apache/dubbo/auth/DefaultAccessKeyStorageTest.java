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
package org.apache.dubbo.auth;

import org.apache.dubbo.auth.model.AccessKeyPair;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;


class DefaultAccessKeyStorageTest {

    @Test
    void testGetAccessKey() {
        URL url = URL.valueOf("dubbo://10.10.10.10:2181")
                .addParameter(Constants.ACCESS_KEY_ID_KEY, "ak")
                .addParameter(Constants.SECRET_ACCESS_KEY_KEY, "sk");
        DefaultAccessKeyStorage defaultAccessKeyStorage = new DefaultAccessKeyStorage();
        AccessKeyPair accessKey = defaultAccessKeyStorage.getAccessKey(url, mock(Invocation.class));
        assertNotNull(accessKey);
        assertEquals(accessKey.getAccessKey(), "ak");
        assertEquals(accessKey.getSecretKey(), "sk");
    }
}
