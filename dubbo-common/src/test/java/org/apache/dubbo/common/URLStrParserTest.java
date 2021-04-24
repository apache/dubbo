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

import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by LinShunkang on 2020/03/12
 */
public class URLStrParserTest {
    private static Set<String> testCases = new HashSet<>(16);
    private static Set<String> errorDecodedCases = new HashSet<>(8);
    private static Set<String> errorEncodedCases = new HashSet<>(8);

    static {
        testCases.add("dubbo://192.168.1.1");
        testCases.add("dubbo://192.168.1.1?");
        testCases.add("dubbo://127.0.0.1?test=中文测试");
        testCases.add("dubbo://admin:admin123@192.168.1.41:28113/org.test.api.DemoService$Iface?anyhost=true&application=demo-service&dubbo=2.6.1&generic=false&interface=org.test.api.DemoService$Iface&methods=orbCompare,checkText,checkPicture&pid=65557&revision=1.4.17&service.filter=bootMetrics&side=provider&status=server&threads=200&timestamp=1583136298859&version=1.0.0");
        // super long text test
        testCases.add("dubbo://192.168.1.1/" + RandomString.make(10240));
        testCases.add("file:/path/to/file.txt");
        testCases.add("dubbo://fe80:0:0:0:894:aeec:f37d:23e1%en0/path?abc=abc");

        errorDecodedCases.add("dubbo:192.168.1.1");
        errorDecodedCases.add("://192.168.1.1");
        errorDecodedCases.add(":/192.168.1.1");

        errorEncodedCases.add("dubbo%3a%2f%2f192.168.1.41%3fabc%3");
        errorEncodedCases.add("dubbo%3a192.168.1.1%3fabc%3dabc");
        errorEncodedCases.add("%3a%2f%2f192.168.1.1%3fabc%3dabc");
        errorEncodedCases.add("%3a%2f192.168.1.1%3fabc%3dabc");
        errorEncodedCases.add("dubbo%3a%2f%2f127.0.0.1%3ftest%3d%e2%96%b2%e2%96%bc%e2%97%80%e2%96%b6%e2%86%90%e2%86%91%e2%86%92%e2%86%93%e2%86%94%e2%86%95%e2%88%9e%c2%b1%e9%be%98%e9%9d%90%e9%bd%89%9%d%b");
    }

    @Test
    public void testEncoded() {
        testCases.forEach(testCase -> {
            assertThat(URLStrParser.parseEncodedStr(URL.encode(testCase)), equalTo(URL.valueOf(testCase)));
        });

        errorEncodedCases.forEach(errorCase -> {
            Assertions.assertThrows(RuntimeException.class,
                    () -> URLStrParser.parseEncodedStr(errorCase));
        });
    }

    @Test
    public void testDecoded() {
        testCases.forEach(testCase -> {
            assertThat(URLStrParser.parseDecodedStr(testCase), equalTo(URL.valueOf(testCase)));
        });

        errorDecodedCases.forEach(errorCase -> {
            Assertions.assertThrows(RuntimeException.class,
                    () -> URLStrParser.parseDecodedStr(errorCase));
        });
    }

}
