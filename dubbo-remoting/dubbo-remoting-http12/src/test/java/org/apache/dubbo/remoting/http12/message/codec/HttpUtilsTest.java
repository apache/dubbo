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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    @Test
    void testParseQuality() {
        Assertions.assertEquals(0.7F, HttpUtils.parseQuality("en-US;q=0.7;level=1", 5));
        Assertions.assertEquals(0.7F, HttpUtils.parseQuality("en-US;q=0.7, zh-CN;q=0.9", 5));
        Assertions.assertEquals(0.8F, HttpUtils.parseQuality("fr-FR; q=0.8 ", 5));
        Assertions.assertEquals(1.0F, HttpUtils.parseQuality("ja", -1));
    }

    @Test
    void testParseAcceptLanguage() {
        List<Locale> locales = HttpUtils.parseAcceptLanguage("en;q=0.5, zh-CN;q=0.9, fr-FR; q=0.8, ja");
        Assertions.assertEquals(
                Arrays.asList(Locale.JAPANESE, Locale.SIMPLIFIED_CHINESE, Locale.FRANCE, Locale.ENGLISH), locales);

        Assertions.assertEquals(
                Arrays.asList(Locale.GERMANY, Locale.US),
                HttpUtils.parseAcceptLanguage("en-US;q=0.7;level=1, de-DE;q=0.8"));
        Assertions.assertEquals(
                Collections.singletonList(Locale.US), HttpUtils.parseAcceptLanguage(";q=0.5, en-US;q=0.8"));
        Assertions.assertEquals(Collections.emptyList(), HttpUtils.parseAcceptLanguage(null));
    }
}
