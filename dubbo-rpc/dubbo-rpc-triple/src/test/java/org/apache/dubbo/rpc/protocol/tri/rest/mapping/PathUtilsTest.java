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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.util.PathUtils;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;

import org.junit.jupiter.api.Test;

class PathUtilsTest {

    @Test
    void getContextPath() {
        URL url = URL.valueOf("tri://127.0.0.1/test/");
        url = url.addParameter(CommonConstants.INTERFACE_KEY, IGreeter.class.getName());
        System.out.println(PathUtils.getContextPath(url));
    }

    @Test
    void isDirectPath() {}

    @Test
    void combine() {}

    @Test
    void normalize() {}
}
