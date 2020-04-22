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
package org.apache.dubbo.common;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by LinShunkang on 2020/03/12
 */
public class URLStrParserTest {

    @Test
    public void test() {
        String str = "dubbo%3A%2F%2Fadmin%3Aadmin123%40192.168.1.41%3A28113%2Forg.test.api.DemoService%24Iface%3Fanyhost%3Dtrue%26application%3Ddemo-service%26dubbo%3D2.6.1%26generic%3Dfalse%26interface%3Dorg.test.api.DemoService%24Iface%26methods%3DorbCompare%2CcheckText%2CcheckPicture%26pid%3D65557%26revision%3D1.4.17%26service.filter%3DbootMetrics%26side%3Dprovider%26status%3Dserver%26threads%3D200%26timestamp%3D1583136298859%26version%3D1.0.0";
        System.out.println(URLStrParser.parseEncodedStr(str));

        String decodeStr = URL.decode(str);
        URL originalUrl = URL.valueOf(decodeStr);
        assertThat(URLStrParser.parseEncodedStr(str), equalTo(originalUrl));
        assertThat(URLStrParser.parseDecodedStr(decodeStr), equalTo(originalUrl));
    }

}
