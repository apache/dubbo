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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.rpc.protocol.rest.util.DataParseUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

public class DataParseUtilsTest {
    @Test
    void testJsonConvert() throws Exception {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataParseUtils.writeJsonContent(User.getInstance(), byteArrayOutputStream);

        Assertions.assertEquals("{\"age\":18,\"id\":404,\"name\":\"dubbo\"}",
            new String(byteArrayOutputStream.toByteArray()));


    }

    @Test
    void testStr() {
        Object convert = DataParseUtils.stringTypeConvert(boolean.class, "true");

        Assertions.assertEquals(Boolean.TRUE, convert);

        convert = DataParseUtils.stringTypeConvert(Boolean.class, "true");

        Assertions.assertEquals(Boolean.TRUE, convert);

        convert = DataParseUtils.stringTypeConvert(String.class, "true");

        Assertions.assertEquals("true", convert);

        convert = DataParseUtils.stringTypeConvert(int.class, "1");

        Assertions.assertEquals(1, convert);

        convert = DataParseUtils.stringTypeConvert(Integer.class, "1");

        Assertions.assertEquals(1, convert);


    }
}
