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
package org.apache.dubbo.remoting.http12.message.codec;

import org.apache.dubbo.remoting.http12.HttpUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpUtilsTest {

    @Test
    void testParseCharset() {
        String charset = HttpUtils.parseCharset("text/html;charset=utf-8");
        Assertions.assertEquals("utf-8", charset);
        charset = HttpUtils.parseCharset("text/html");
        Assertions.assertEquals("", charset);
        charset = HttpUtils.parseCharset("application/json;charset=utf-8; boundary=__X_PAW_BOUNDARY__");
        Assertions.assertEquals("utf-8", charset);
        charset = HttpUtils.parseCharset("multipart/form-data; charset=utf-8; boundary=__X_PAW_BOUNDARY__");
        Assertions.assertEquals("utf-8", charset);
    }
}
