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
package org.apache.dubbo.rpc.executor;

import org.apache.dubbo.common.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IsolationExecutorSupportFactoryTest {
    @Test
    void test() {
        Assertions.assertInstanceOf(DefaultExecutorSupport.class, IsolationExecutorSupportFactory.getIsolationExecutorSupport(URL.valueOf("dubbo://")));
        Assertions.assertInstanceOf(DefaultExecutorSupport.class, IsolationExecutorSupportFactory.getIsolationExecutorSupport(URL.valueOf("empty://")));
        Assertions.assertInstanceOf(DefaultExecutorSupport.class, IsolationExecutorSupportFactory.getIsolationExecutorSupport(URL.valueOf("exchange://")));
        Assertions.assertInstanceOf(Mock1ExecutorSupport.class, IsolationExecutorSupportFactory.getIsolationExecutorSupport(URL.valueOf("mock1://")));
        Assertions.assertInstanceOf(Mock2ExecutorSupport.class, IsolationExecutorSupportFactory.getIsolationExecutorSupport(URL.valueOf("mock2://")));
        Assertions.assertInstanceOf(DefaultExecutorSupport.class, IsolationExecutorSupportFactory.getIsolationExecutorSupport(URL.valueOf("mock3://")));
    }
}
