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
package org.apache.dubbo.auth.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

class SignatureUtilsTest {

    @Test
    void testEncryptWithObject() {
        Object[] objects = new Object[] {new ArrayList<>(), "temp"};
        String encryptWithObject = SignatureUtils.sign(objects, "TestMethod#hello", "TOKEN");
        Assertions.assertEquals(encryptWithObject, "t6c7PasKguovqSrVRcTQU4wTZt/ybl0jBCUMgAt/zQw=");
    }
    
    @Test
    void testEncryptWithNoParameters() {
        String encryptWithNoParams = SignatureUtils.sign(null, "TestMethod#hello", "TOKEN");
        Assertions.assertEquals(encryptWithNoParams, "2DGkTcyXg4plU24rY8MZkEJwOMRW3o+wUP3HssRc3EE=");
    }
}