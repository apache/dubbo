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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TriplePathResolverTest {

    private static final PathResolver PATH_RESOLVER = ExtensionLoader.getExtensionLoader(PathResolver.class)
        .getDefaultExtension();

    private static Invoker<Object> INVOKER = new Invoker<Object>() {
        @Override
        public URL getUrl() {
            return null;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void destroy() {

        }

        @Override
        public Class<Object> getInterface() {
            return null;
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return null;
        }
    };

    @BeforeEach
    public void init() {

        PATH_RESOLVER.add("/abc", INVOKER);
    }

    @AfterEach
    public void destroy() {
        PATH_RESOLVER.destroy();
    }

    @Test
    public void testResolve() {
        Assertions.assertEquals(INVOKER, getInvokerByPath("/abc"));
    }

    @Test
    public void testRemove() {
        Assertions.assertEquals(INVOKER, getInvokerByPath("/abc"));
        PATH_RESOLVER.remove("/abc");
        Assertions.assertNull(getInvokerByPath("/abc"));
    }

    @Test
    public void testDestroy() {
        Assertions.assertEquals(INVOKER, getInvokerByPath("/abc"));
        {
            PATH_RESOLVER.add("/bcd", INVOKER);
            Assertions.assertEquals(INVOKER, getInvokerByPath("/bcd"));
        }
        PATH_RESOLVER.destroy();
        Assertions.assertNull(getInvokerByPath("/abc"));
        Assertions.assertNull(getInvokerByPath("/bcd"));
    }

    private Invoker getInvokerByPath(String path) {
        return PATH_RESOLVER.resolve(path);
    }
}
