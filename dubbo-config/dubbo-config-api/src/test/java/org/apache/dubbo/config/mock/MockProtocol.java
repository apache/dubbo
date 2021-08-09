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
package org.apache.dubbo.config.mock;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import org.mockito.Mockito;

public class MockProtocol implements Protocol {

    /* (non-Javadoc)
     * @see org.apache.dubbo.rpc.Protocol#getDefaultPort()
     */
    @Override
    public int getDefaultPort() {

        return 0;
    }

    /* (non-Javadoc)
     * @see org.apache.dubbo.rpc.Protocol#export(org.apache.dubbo.rpc.Invoker)
     */
    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        return Mockito.mock(Exporter.class);
    }

    /* (non-Javadoc)
     * @see org.apache.dubbo.rpc.Protocol#refer(java.lang.Class, org.apache.dubbo.common.URL)
     */
    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {

        final URL u = url;

        return new Invoker<T>() {
            @Override
            public Class<T> getInterface() {
                return null;
            }

            public URL getUrl() {
                return u;
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public Result invoke(Invocation invocation) throws RpcException {
                return null;
            }

            @Override
            public void destroy() {

            }
        };
    }

    /* (non-Javadoc)
     * @see org.apache.dubbo.rpc.Protocol#destroy()
     */
    @Override
    public void destroy() {

    }

}
