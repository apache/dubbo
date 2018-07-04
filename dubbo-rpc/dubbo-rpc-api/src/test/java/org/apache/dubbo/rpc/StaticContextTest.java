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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class StaticContextTest {

    @Test
    public void testGetContext() {
        String name = "custom";

        StaticContext context = StaticContext.getContext(name);
        Assert.assertTrue(context != null);
        Assert.assertEquals(name, context.getName());

        StaticContext.remove(name);

        StaticContext sysContext = StaticContext.getSystemContext();
        Assert.assertTrue(sysContext != null);

    }

    @Test
    public void testGetKey() {
        String interfaceName = "interface";
        String method = "method";
        String group = "group";
        String version = "1.0";

        String suffix = "suffix";

        Map<String, String> para = new HashMap<>();
        para.put(Constants.INTERFACE_KEY, interfaceName);
        para.put(Constants.GROUP_KEY, group);
        para.put(Constants.VERSION_KEY, version);

        URL url = new URL("dubbo", "localhost", 20880, interfaceName, para);

        Assert.assertEquals(StaticContext.getKey(url, method, suffix),
                StaticContext.getKey(para, method, suffix));

    }
}
