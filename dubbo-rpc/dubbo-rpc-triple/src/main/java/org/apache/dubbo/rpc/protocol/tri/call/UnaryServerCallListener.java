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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.tri.RpcStatus;

public class UnaryServerCallListener extends AbstractServerCallListener implements ServerCall.Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCall.class);

    public UnaryServerCallListener(ServerCall call, RpcInvocation invocation, Invoker<?> invoker) {
        super(call, invocation, invoker);
        call.requestN(2);
    }

    @Override
    protected void onServerResponse(AppResponse response) {
        call.writeMessage(response.getValue());
        call.close(RpcStatus.OK, response.getObjectAttachments());
    }

    @Override
    public void onMessage(Object message) {
        if (message instanceof Object[]) {
            invocation.setArguments((Object[]) message);
        } else {
            invocation.setArguments(new Object[]{message});
        }
    }


    @Override
    public void onCancel(String errorInfo) {
        // ignored
    }

    @Override
    public void onComplete() {
        invoke();
    }

}
