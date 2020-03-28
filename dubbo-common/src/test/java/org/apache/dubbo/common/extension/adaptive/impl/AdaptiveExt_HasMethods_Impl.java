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

package org.apache.dubbo.common.extension.adaptive.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.adaptive.AdaptiveExt_HasMethods;
import org.apache.dubbo.rpc.Invocation;

import org.junit.jupiter.api.Assertions;

public class AdaptiveExt_HasMethods_Impl implements AdaptiveExt_HasMethods {
    @Override
    public String echo1(URL url, String s) {
        return "Hello " + s;
    }

    @Override
    public String echo2(URL url, String s) {
        return "Hello " + s;
    }

    @Override
    public String echo3(URL url, String s, Invocation invocation) {
        return "Hello " + s;
    }

    @Override
    public void echo4(URL url, String s) {
        Assertions.assertNotNull(s);
    }

    @Override
    public String getName() throws Exception {
        throw new Exception("Test");
    }
}
