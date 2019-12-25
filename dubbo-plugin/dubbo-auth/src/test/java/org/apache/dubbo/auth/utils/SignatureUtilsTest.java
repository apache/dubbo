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
import java.util.List;

public class SignatureUtilsTest {


    private Object[] objects = new Object[2];
    private List<String> list = new ArrayList<>();
    private String temp = "temp";
    private String key = "TOKEN";

    {
        objects[0] = list;
        objects[1] = temp;
    }

    @Test
    void testObjectToByteArray() {

        try {
            byte[] bytes = SignatureUtils.toByteArray(objects);
            Assertions.assertNotEquals(0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testEncryptObject() {
        String encrypt = SignatureUtils.sign(objects, "TestMethod#hello", key);
        String encryptNoParams = SignatureUtils.sign(null, "TestMethod#hello", key);
        Assertions.assertNotNull(encrypt);
        Assertions.assertNotNull(encryptNoParams);
        Assertions.assertNotEquals(encrypt, encryptNoParams);
    }
}
