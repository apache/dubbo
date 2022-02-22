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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ClientCallTest {
    @Test
    public void testNewCall() throws NoSuchMethodException {
        Connection connection = Mockito.mock(Connection.class);
        URL url = URL.valueOf("tri://127.0.0.1:9103/" + IGreeter.class.getName());
        url.getOrDefaultApplicationModel().getExtensionLoader(ExecutorRepository.class)
            .getDefaultExtension()
            .createExecutorIfAbsent(url);
        ClientCall call = new ClientCall(url, connection);
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("test");
        MethodDescriptor echoMethod = new MethodDescriptor(IGreeter.class.getDeclaredMethod("echo", String.class));
        final RequestMetadata request = StreamUtils.createRequest(url, echoMethod, invocation, 1L, Compressor.NONE, "", 3000,
            null, null);
        ClientCallUtil.call(call, request);
    }

}