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
package org.apache.dubbo.rpc.filter.tps;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.support.MockInvocation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.rpc.Constants.TPS_LIMIT_RATE_KEY;
import static org.apache.dubbo.rpc.Constants.TPS_LIMIT_INTERVAL_KEY;

public class DefaultTPSLimiterTest {

    private DefaultTPSLimiter defaultTPSLimiter = new DefaultTPSLimiter();

    @Test
    public void testIsAllowable() throws Exception {
        Invocation invocation = new MockInvocation();
        URL url = URL.valueOf("test://test");
        url = url.addParameter(INTERFACE_KEY, "org.apache.dubbo.rpc.file.TpsService");
        url = url.addParameter(TPS_LIMIT_RATE_KEY, 2);
        url = url.addParameter(TPS_LIMIT_INTERVAL_KEY, 1000);
        for (int i = 0; i < 3; i++) {
            Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
        }
    }

    @Test
    public void testIsNotAllowable() throws Exception {
        Invocation invocation = new MockInvocation();
        URL url = URL.valueOf("test://test");
        url = url.addParameter(INTERFACE_KEY, "org.apache.dubbo.rpc.file.TpsService");
        url = url.addParameter(TPS_LIMIT_RATE_KEY, 2);
        url = url.addParameter(TPS_LIMIT_INTERVAL_KEY, 1000);
        for (int i = 0; i < 4; i++) {
            if (i == 3) {
                Assertions.assertFalse(defaultTPSLimiter.isAllowable(url, invocation));
            } else {
                Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
            }
        }
    }


    @Test
    public void testConfigChange() throws Exception {
        Invocation invocation = new MockInvocation();
        URL url = URL.valueOf("test://test");
        url = url.addParameter(INTERFACE_KEY, "org.apache.dubbo.rpc.file.TpsService");
        url = url.addParameter(TPS_LIMIT_RATE_KEY, 2);
        url = url.addParameter(TPS_LIMIT_INTERVAL_KEY, 1000);
        for (int i = 0; i < 3; i++) {
            Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
        }
        url = url.addParameter(TPS_LIMIT_RATE_KEY, 2000);
        for (int i = 0; i < 3; i++) {
            Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
        }
    }
}
