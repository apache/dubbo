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

import org.apache.dubbo.rpc.protocol.tri.RpcStatus;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.WrapResponseUnpack;
import org.apache.dubbo.triple.TripleWrapper;

import java.util.Map;

public class WrapResponseCallListener implements ClientCall.Listener {
    private final ClientCall.Listener delegate;
    private final WrapResponseUnpack responseUnpack;

    protected WrapResponseCallListener(ClientCall.Listener delegate, GenericUnpack genericUnpack) {
        this.delegate = delegate;
        this.responseUnpack = new WrapResponseUnpack(genericUnpack);
    }

    public static ClientCall.Listener wrap(ClientCall.Listener listener, GenericUnpack genericUnpack) {
        return new WrapResponseCallListener(listener, genericUnpack);
    }

    @Override
    public void onMessage(Object message) {
        try {
            final Object unpack = responseUnpack.unpack((TripleWrapper.TripleResponseWrapper) message);
            delegate.onMessage(unpack);
        } catch (Throwable t) {
            final RpcStatus status = RpcStatus.INTERNAL
                    .withDescription("Failed deserialize response")
                    .withCause(t);
            onClose(status, null);
        }
    }

    @Override
    public void onClose(RpcStatus status, Map<String, Object> trailers) {
        delegate.onClose(status, trailers);
    }

}
